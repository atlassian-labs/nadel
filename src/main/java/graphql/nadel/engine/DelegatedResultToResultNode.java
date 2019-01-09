package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.nextgen.ExecutionStrategyUtil;
import graphql.execution.nextgen.FetchedValue;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.execution.nextgen.ResultNodesCreator;
import graphql.execution.nextgen.result.ExecutionResultMultiZipper;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.ExecutionResultZipper;
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.nadel.DelegatedExecutionResult;
import graphql.util.FpKit;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static graphql.util.FpKit.map;

public class DelegatedResultToResultNode {

    ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    DelegatedResultAnalyzer fetchedValueAnalyzer = new DelegatedResultAnalyzer();
    ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    ExecutionStrategyUtil util = new ExecutionStrategyUtil();

    public ObjectExecutionResultNode.RootExecutionResultNode resultToResultNode(ExecutionContext executionContext,
                                                                                DelegatedExecutionResult delegatedExecutionResult,
                                                                                ExecutionStepInfo executionStepInfo,
                                                                                List<MergedField> mergedFields) {

        //TODO: the ExecutionContext and the FieldSubSelection (the ExecutionStepInfo in it) are referencing the overall schema, not the private schema

        FieldSubSelection fieldSubSelectionWithData = new FieldSubSelection();
        fieldSubSelectionWithData.setExecutionStepInfo(executionStepInfo);
        fieldSubSelectionWithData.setSource(delegatedExecutionResult.getData());

        Map<String, MergedField> subFields = FpKit.getByName(mergedFields, MergedField::getResultKey);
        MergedSelectionSet mergedSelectionSet = MergedSelectionSet.newMergedSelectionSet()
                .subFields(subFields).build();

        fieldSubSelectionWithData.setMergedSelectionSet(mergedSelectionSet);

        List<NamedResultNode> namedResultNodes = resolveSubSelection(executionContext, fieldSubSelectionWithData);
        return new ObjectExecutionResultNode.RootExecutionResultNode(namedResultNodes);
    }

    private List<NamedResultNode> resolveSubSelection(ExecutionContext executionContext, FieldSubSelection fieldSubSelection) {
        return map(fetchSubSelection(executionContext, fieldSubSelection), node -> resolveAllChildNodes(executionContext, node));
    }

    private NamedResultNode resolveAllChildNodes(ExecutionContext context, NamedResultNode namedResultNode) {
        ExecutionResultMultiZipper unresolvedNodes = ResultNodesUtil.getUnresolvedNodes(namedResultNode.getNode());
        List<ExecutionResultZipper> resolvedNodes = map(unresolvedNodes.getZippers(), unresolvedNode -> resolveNode(context, unresolvedNode));
        return resolvedNodesToResultNode(namedResultNode, unresolvedNodes, resolvedNodes);
    }

    private ExecutionResultZipper resolveNode(ExecutionContext executionContext, ExecutionResultZipper unresolvedNode) {
        FetchedValueAnalysis fetchedValueAnalysis = unresolvedNode.getCurNode().getFetchedValueAnalysis();
        FieldSubSelection fieldSubSelection = util.createFieldSubSelection(executionContext, fetchedValueAnalysis);
        List<NamedResultNode> namedResultNodes = resolveSubSelection(executionContext, fieldSubSelection);
        return unresolvedNode.withNode(new ObjectExecutionResultNode(fetchedValueAnalysis, namedResultNodes));
    }

    private NamedResultNode resolvedNodesToResultNode(NamedResultNode namedResultNode,
                                                      ExecutionResultMultiZipper unresolvedNodes,
                                                      List<ExecutionResultZipper> resolvedNodes) {
        ExecutionResultNode rootNode = unresolvedNodes.withZippers(resolvedNodes).toRootNode();
        return namedResultNode.withNode(rootNode);
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
        return new FetchedValue(fetchedValue, fetchedValue, Collections.emptyList());
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
