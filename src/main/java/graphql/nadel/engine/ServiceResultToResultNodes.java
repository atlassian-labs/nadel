package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.ResolveType;
import graphql.execution.nextgen.ExecutionStrategyUtil;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.ResultNodesCreator;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.nadel.result.UnresolvedObjectResultNode;
import graphql.nadel.util.ErrorUtil;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.util.FpKit;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.nadel.result.ObjectExecutionResultNode.newObjectExecutionResultNode;

public class ServiceResultToResultNodes {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final ServiceExecutionResultAnalyzer fetchedValueAnalyzer = new ServiceExecutionResultAnalyzer();
    private final graphql.nadel.result.ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    private final ExecutionStrategyUtil util = new ExecutionStrategyUtil();
    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

    ResolveType resolveType = new ResolveType();


    private static final Logger log = LoggerFactory.getLogger(ServiceResultToResultNodes.class);


    public RootExecutionResultNode resultToResultNode(ExecutionContext executionContext,
                                                      ExecutionStepInfo executionStepInfo,
                                                      List<MergedField> mergedFields,
                                                      ServiceExecutionResult serviceExecutionResult,
                                                      ElapsedTime elapsedTimeForServiceCall,
                                                      NormalizedQueryFromAst normalizedQueryFromAst
    ) {
        long startTime = System.currentTimeMillis();

        List<GraphQLError> errors = ErrorUtil.createGraphQlErrorsFromRawErrors(serviceExecutionResult.getErrors());
        RootExecutionResultNode rootNode = RootExecutionResultNode.newRootExecutionResultNode().errors(errors).elapsedTime(elapsedTimeForServiceCall).build();
        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        RootExecutionResultNode result = (RootExecutionResultNode) resultNodesTransformer.transformParallel(nadelContext.getForkJoinPool(), rootNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (node instanceof RootExecutionResultNode) {
                    ExecutionResultNode executionResultNode = changeRootNode((RootExecutionResultNode) node, executionContext, executionStepInfo, mergedFields, serviceExecutionResult, elapsedTimeForServiceCall, normalizedQueryFromAst);
                    return TreeTransformerUtil.changeNode(context, executionResultNode);
                }
                if (!(node instanceof UnresolvedObjectResultNode)) {
                    return TraversalControl.CONTINUE;
                }
                UnresolvedObjectResultNode unresolvedNode = (UnresolvedObjectResultNode) node;
                ResolvedValue resolvedValue = unresolvedNode.getResolvedValue();
                ExecutionStepInfo esi = unresolvedNode.getExecutionStepInfo();
                NormalizedQueryField normalizedField = unresolvedNode.getNormalizedField();
                Map<String, NormalizedQueryField> normalizedFieldByResultKey = FpKit.getByName(normalizedField.getChildren(), NormalizedQueryField::getResultKey);

                FieldSubSelection fieldSubSelection = createFieldSubSelection(executionContext, esi, resolvedValue, normalizedField, normalizedQueryFromAst);
                List<ExecutionResultNode> children = fetchSubSelection(executionContext, fieldSubSelection, elapsedTimeForServiceCall, normalizedFieldByResultKey);
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

    public FieldSubSelection createFieldSubSelection(ExecutionContext executionContext,
                                                     ExecutionStepInfo executionInfo,
                                                     ResolvedValue resolvedValue,
                                                     NormalizedQueryField normalizedQueryField,
                                                     NormalizedQueryFromAst normalizedQueryFromAst) {
        MergedField field = executionInfo.getField();
        Object source = resolvedValue.getCompletedValue();
        Object localContext = resolvedValue.getLocalContext();

        GraphQLOutputType sourceType = executionInfo.getUnwrappedNonNullType();
        GraphQLObjectType resolvedObjectType = resolveType.resolveType(executionContext, field, source, executionInfo.getArguments(), sourceType);

        MergedSelectionSet mergedSelectionSet = mergedSelectionSetFromNormalizedField(normalizedQueryField, normalizedQueryFromAst, resolvedObjectType);

        // it is not really a new step but rather a refinement
        ExecutionStepInfo newExecutionStepInfoWithResolvedType = executionInfo.changeTypeWithPreservedNonNull(resolvedObjectType);

        return FieldSubSelection.newFieldSubSelection()
                .source(source)
                .localContext(localContext)
                .mergedSelectionSet(mergedSelectionSet)
                .executionInfo(newExecutionStepInfoWithResolvedType)
                .build();
    }

    private MergedSelectionSet mergedSelectionSetFromNormalizedField(NormalizedQueryField normalizedQueryField, NormalizedQueryFromAst normalizedQueryFromAst, GraphQLObjectType resolvedObjectType) {
        Map<String, MergedField> subFields = new LinkedHashMap<>(normalizedQueryField.getChildren().size());
        for (NormalizedQueryField childField : normalizedQueryField.getChildren()) {
            if (resolvedObjectType != null && !childField.getObjectType().getName().equals(resolvedObjectType.getName())) {
                continue;
            }
            MergedField mergedField = normalizedQueryFromAst.getMergedFieldByNormalizedFields().get(childField);
            subFields.put(childField.getResultKey(), mergedField);
        }
        return MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields)
                .build();
    }


    private ExecutionResultNode changeRootNode(RootExecutionResultNode rootNode,
                                               ExecutionContext executionContext,
                                               ExecutionStepInfo executionStepInfo,
                                               List<MergedField> mergedFields,
                                               ServiceExecutionResult serviceExecutionResult,
                                               ElapsedTime elapsedTime,
                                               NormalizedQueryFromAst normalizedQueryFromAst) {
        List<NormalizedQueryField> topLevelFields = normalizedQueryFromAst.getRootFields();
        Map<String, MergedField> subFields = new LinkedHashMap<>(topLevelFields.size());
        for (NormalizedQueryField topLevelField : topLevelFields) {
            MergedField mergedField = normalizedQueryFromAst.getMergedFieldByNormalizedFields().get(topLevelField);
            subFields.put(topLevelField.getResultKey(), mergedField);
        }
        Map<String, NormalizedQueryField> normalizedFielByResultKey = FpKit.getByName(topLevelFields, NormalizedQueryField::getResultKey);
        MergedSelectionSet mergedSelectionSet = MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields)
                .build();
        FieldSubSelection fieldSubSelectionWithData = FieldSubSelection.newFieldSubSelection().
                executionInfo(executionStepInfo)
                .source(serviceExecutionResult.getData())
                .mergedSelectionSet(mergedSelectionSet)
                .build();

        List<ExecutionResultNode> subNodes = fetchSubSelection(executionContext, fieldSubSelectionWithData, elapsedTime, normalizedFielByResultKey);
        return rootNode.withNewChildren(subNodes);
    }

    private List<ExecutionResultNode> fetchSubSelection(ExecutionContext executionContext,
                                                        FieldSubSelection fieldSubSelection,
                                                        ElapsedTime elapsedTime,
                                                        Map<String, NormalizedQueryField> normalizedQueryField) {
        List<FetchedValueAnalysis> fetchedValueAnalysisList = fetchAndAnalyze(executionContext, fieldSubSelection);
        return fetchedValueAnalysisToNodes(executionContext, fetchedValueAnalysisList, elapsedTime, normalizedQueryField);
    }

    private List<FetchedValueAnalysis> fetchAndAnalyze(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        List<MergedField> subFieldsList = fieldSubSelection.getMergedSelectionSet().getSubFieldsList();
        List<FetchedValueAnalysis> result = new ArrayList<>(subFieldsList.size());
        for (MergedField mergedField : subFieldsList) {
            result.add(fetchAndAnalyzeField(context, fieldSubSelection.getSource(), mergedField, fieldSubSelection.getExecutionStepInfo()));
        }
        return result;
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

    private List<ExecutionResultNode> fetchedValueAnalysisToNodes(ExecutionContext executionContext,
                                                                  List<FetchedValueAnalysis> fetchedValueAnalysisList,
                                                                  ElapsedTime elapsedTime,
                                                                  Map<String, NormalizedQueryField> normalizedQueryFieldByResultKey) {
        List<ExecutionResultNode> result = new ArrayList<>(fetchedValueAnalysisList.size());
        for (FetchedValueAnalysis fetchedValueAnalysis : fetchedValueAnalysisList) {
            NormalizedQueryField childNormalizedField = normalizedQueryFieldByResultKey.get(fetchedValueAnalysis.getResultKey());
            ExecutionResultNode executionResultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis, childNormalizedField).withElapsedTime(elapsedTime);
            result.add(executionResultNode);
        }
        return result;
    }


}
