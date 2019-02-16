package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.nextgen.ExecutionStrategyUtil;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.ResultNodesCreator;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.DelegatedExecutionResult;
import graphql.schema.GraphQLSchema;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.util.FpKit.map;

public class ServiceResultToResultNodes {

    ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    DelegatedResultAnalyzer fetchedValueAnalyzer = new DelegatedResultAnalyzer();
    ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    ExecutionStrategyUtil util = new ExecutionStrategyUtil();

    public RootExecutionResultNode resultToResultNode(ExecutionContext executionContext,
                                                      DelegatedExecutionResult delegatedExecutionResult,
                                                      ExecutionStepInfo executionStepInfo,
                                                      List<MergedField> mergedFields,
                                                      GraphQLSchema underlyingSchema) {

        //TODO: currently changing the ExecutionContext and stepInfo so that it fits the underlying schema, this should be done somewhere else
        ExecutionContext executionContextForService = executionContext.transform(builder -> builder.graphQLSchema(underlyingSchema));
        ExecutionStepInfo stepInfoForService = executionStepInfo.transform(builder -> builder.type(underlyingSchema.getQueryType()));


        Map<String, MergedField> subFields = FpKit.getByName(mergedFields, MergedField::getResultKey);
        MergedSelectionSet mergedSelectionSet = MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields).build();

        FieldSubSelection fieldSubSelectionWithData = FieldSubSelection.newFieldSubSelection().
                executionInfo(stepInfoForService)
                .source(delegatedExecutionResult.getData())
                .mergedSelectionSet(mergedSelectionSet)
                .build();

        List<ExecutionResultNode> namedResultNodes = resolveSubSelection(executionContextForService, fieldSubSelectionWithData);
        return new RootExecutionResultNode(namedResultNodes);
    }

    private List<ExecutionResultNode> resolveSubSelection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        return map(fetchSubSelection(executionContext, fieldSubSelection), node -> resolveAllChildNodes(executionContext, node));
    }

    private ExecutionResultNode resolveAllChildNodes(ExecutionContext context, NamedResultNode namedResultNode) {
        NodeMultiZipper<ExecutionResultNode> unresolvedNodes = ResultNodesUtil.getUnresolvedNodes(namedResultNode.getNode());
        List<NodeZipper<ExecutionResultNode>> resolvedNodes = map(unresolvedNodes.getZippers(), unresolvedNode -> resolveNode(context, unresolvedNode));
        return resolvedNodesToResultNode(namedResultNode, unresolvedNodes, resolvedNodes);
    }

    private NodeZipper<ExecutionResultNode> resolveNode(ExecutionContext executionContext, NodeZipper<ExecutionResultNode> unresolvedNode) {
        FetchedValueAnalysis fetchedValueAnalysis = unresolvedNode.getCurNode().getFetchedValueAnalysis();
        FieldSubSelection fieldSubSelection = util.createFieldSubSelection(executionContext, fetchedValueAnalysis);
        List<ExecutionResultNode> namedResultNodes = resolveSubSelection(executionContext, fieldSubSelection);
        return unresolvedNode.withNewNode(new ObjectExecutionResultNode(fetchedValueAnalysis, namedResultNodes));
    }

    private ExecutionResultNode resolvedNodesToResultNode(NamedResultNode namedResultNode,
                                                          NodeMultiZipper<ExecutionResultNode> unresolvedNodes,
                                                          List<NodeZipper<ExecutionResultNode>> resolvedNodes) {
        ExecutionResultNode rootNode = unresolvedNodes.withReplacedZippers(resolvedNodes).toRootNode();
        return rootNode;
    }

    private List<NamedResultNode> fetchSubSelection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        List<FetchedValueAnalysis> fetchedValueAnalysisList = fetchAndAnalyze(executionContext, fieldSubSelection);
        return fetchedValueAnalysisToNodes(fetchedValueAnalysisList);
    }

    private List<FetchedValueAnalysis> fetchAndAnalyze(ExecutionContext context, FieldSubSelection fieldSubSelection) {
        return FpKit.map(fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                mergedField -> fetchAndAnalyzeField(context, fieldSubSelection.getSource(), mergedField, fieldSubSelection.getExecutionStepInfo()));
    }

    private FetchedValueAnalysis fetchAndAnalyzeField(ExecutionContext context, Object source, MergedField mergedField,
                                                      ExecutionStepInfo executionStepInfo) {
        ExecutionStepInfo newExecutionStepInfo = executionStepInfoFactory.newExecutionStepInfoForSubField(context, mergedField, executionStepInfo);
        FetchedValue fetchedValue = fetchValue(context, source, mergedField, newExecutionStepInfo);
        return analyseValue(context, fetchedValue, newExecutionStepInfo);
    }

    private FetchedValue fetchValue(ExecutionContext context, Object source, MergedField mergedField, ExecutionStepInfo newExecutionStepInfo) {
        Map<String, Object> map = (Map<String, Object>) source;
        Object fetchedValue = map.get(mergedField.getResultKey());
        return FetchedValue.newFetchedValue()
                .fetchedValue(fetchedValue)
                .rawFetchedValue(fetchedValue)
                .errors(Collections.emptyList())
                .build();
    }

    private FetchedValueAnalysis analyseValue(ExecutionContext executionContext, FetchedValue fetchedValue, ExecutionStepInfo executionInfo) {
        FetchedValueAnalysis fetchedValueAnalysis = fetchedValueAnalyzer.analyzeFetchedValue(executionContext, fetchedValue, executionInfo);
        return fetchedValueAnalysis;
    }

    private List<NamedResultNode> fetchedValueAnalysisToNodes(List<FetchedValueAnalysis> fetchedValueAnalysisList) {
        return FpKit.map(fetchedValueAnalysisList, fetchedValueAnalysis -> {
            ExecutionResultNode resultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis);
            return new NamedResultNode(fetchedValueAnalysis.getField().getResultKey(), resultNode);
        });
    }


}
