package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.nextgen.ExecutionStrategyUtil;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.ResultNodesCreator;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.nadel.result.UnresolvedObjectResultNode;
import graphql.nadel.util.ErrorUtil;
import graphql.util.FpKit;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.nadel.result.ObjectExecutionResultNode.newObjectExecutionResultNode;
import static graphql.util.FpKit.map;

public class ServiceResultToResultNodes {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceExecutionResultAnalyzer fetchedValueAnalyzer = new ServiceExecutionResultAnalyzer();
    private final graphql.nadel.result.ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    private final ExecutionStrategyUtil util = new ExecutionStrategyUtil();
    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

    private static final Logger log = LoggerFactory.getLogger(ServiceResultToResultNodes.class);

    public RootExecutionResultNode resultToResultNode(ExecutionContext executionContext,
                                                      ExecutionStepInfo executionStepInfo,
                                                      List<MergedField> mergedFields,
                                                      ServiceExecutionResult serviceExecutionResult,
                                                      ElapsedTime elapsedTimeForServiceCall
    ) {
        long startTime = System.currentTimeMillis();

        List<GraphQLError> errors = ErrorUtil.createGraphQlErrorsFromRawErrors(serviceExecutionResult.getErrors());
        RootExecutionResultNode rootNode = RootExecutionResultNode.newRootExecutionResultNode().errors(errors).elapsedTime(elapsedTimeForServiceCall).build();
        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        RootExecutionResultNode result = (RootExecutionResultNode) resultNodesTransformer.transform(rootNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (node instanceof RootExecutionResultNode) {
                    changeRootNode(context, (RootExecutionResultNode) node, executionContext, executionStepInfo, mergedFields, serviceExecutionResult, elapsedTimeForServiceCall);
                    return TraversalControl.CONTINUE;
                }
                if (!(node instanceof UnresolvedObjectResultNode)) {
                    return TraversalControl.CONTINUE;
                }
                UnresolvedObjectResultNode unresolvedNode = (UnresolvedObjectResultNode) node;
                ResolvedValue resolvedValue = unresolvedNode.getResolvedValue();
                ExecutionStepInfo esi = unresolvedNode.getExecutionStepInfo();

                FieldSubSelection fieldSubSelection = util.createFieldSubSelection(executionContext, esi, resolvedValue);
                List<ExecutionResultNode> children = fetchSubSelection(executionContext, fieldSubSelection, elapsedTimeForServiceCall);
                TreeTransformerUtil.changeNode(context, newObjectExecutionResultNode()
                        .executionPath(unresolvedNode.getExecutionPath())
                        .alias(esi.getField().getSingleField().getAlias())
                        .fieldIds(NodeId.getIds(esi.getField()))
                        .objectType(esi.getFieldContainer())
                        .fieldDefinition(esi.getFieldDefinition())
                        .resolvedValue(resolvedValue)
                        .children(children)
                        .elapsedTime(elapsedTimeForServiceCall)
                        .build());
                return TraversalControl.CONTINUE;
            }
        });
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("ServiceResultToResultNodes time: {} ms, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return result;
    }

    private void changeRootNode(TraverserContext<ExecutionResultNode> context,
                                RootExecutionResultNode rootNode,
                                ExecutionContext executionContext,
                                ExecutionStepInfo executionStepInfo,
                                List<MergedField> mergedFields,
                                ServiceExecutionResult serviceExecutionResult,
                                ElapsedTime elapsedTime) {
        Map<String, MergedField> subFields = FpKit.getByName(mergedFields, MergedField::getResultKey);
        MergedSelectionSet mergedSelectionSet = MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields).build();

        FieldSubSelection fieldSubSelectionWithData = FieldSubSelection.newFieldSubSelection().
                executionInfo(executionStepInfo)
                .source(serviceExecutionResult.getData())
                .mergedSelectionSet(mergedSelectionSet)
                .build();

        List<ExecutionResultNode> subNodes = fetchSubSelection(executionContext, fieldSubSelectionWithData, elapsedTime);
        TreeTransformerUtil.changeNode(context, rootNode.withNewChildren(subNodes));

    }

    private List<ExecutionResultNode> fetchSubSelection(ExecutionContext executionContext,
                                                        FieldSubSelection fieldSubSelection,
                                                        ElapsedTime elapsedTime) {
        List<FetchedValueAnalysis> fetchedValueAnalysisList = fetchAndAnalyze(executionContext, fieldSubSelection);
        return fetchedValueAnalysisToNodes(executionContext, fetchedValueAnalysisList, elapsedTime);
    }

    private List<FetchedValueAnalysis> fetchAndAnalyze(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        return map(fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                mergedField -> fetchAndAnalyzeField(context, fieldSubSelection.getSource(), mergedField, fieldSubSelection.getExecutionStepInfo()));
    }

    private FetchedValueAnalysis fetchAndAnalyzeField(ExecutionContext context,
                                                      Object source,
                                                      MergedField mergedField,
                                                      ExecutionStepInfo executionStepInfo) {
        ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, executionStepInfo);

        FetchedValue fetchedValue = fetchValue(source, mergedField);
        return analyseValue(context, fetchedValue, newExecutionStepInfo);
    }

    private FetchedValue fetchValue(Object source, MergedField mergedField) {
        if (source == null) {
            return FetchedValue.newFetchedValue()
                    .fetchedValue(null)
                    .rawFetchedValue(null)
                    .errors(Collections.emptyList())
                    .build();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) source;
        Object fetchedValue = map.get(mergedField.getResultKey());
        return FetchedValue.newFetchedValue()
                .fetchedValue(fetchedValue)
                .rawFetchedValue(fetchedValue)
                .errors(Collections.emptyList())
                .build();
    }

    private FetchedValueAnalysis analyseValue(ExecutionContext executionContext, FetchedValue fetchedValue, ExecutionStepInfo executionInfo) {
        return fetchedValueAnalyzer.analyzeFetchedValue(executionContext, fetchedValue, executionInfo);
    }

    private List<ExecutionResultNode> fetchedValueAnalysisToNodes(ExecutionContext executionContext, List<FetchedValueAnalysis> fetchedValueAnalysisList, ElapsedTime elapsedTime) {
        return map(fetchedValueAnalysisList, fetchedValue -> resultNodesCreator.createResultNode(fetchedValue).withElapsedTime(elapsedTime));
    }


}
