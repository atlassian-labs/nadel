package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.FetchedValue;
import graphql.execution.MergedField;
import graphql.execution.ResolveType;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.nadel.result.ResultNodesCreator;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.nadel.result.UnresolvedObjectResultNode;
import graphql.nadel.util.ErrorUtil;
import graphql.schema.GraphQLObjectType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static graphql.nadel.result.ObjectExecutionResultNode.newObjectExecutionResultNode;

public class ServiceResultToResultNodes {

    private final ExecutionStepInfoFactory executionStepInfoFactory = new ExecutionStepInfoFactory();
    private final FetchedValueAnalyzer fetchedValueAnalyzer = new FetchedValueAnalyzer();
    private final ResultNodesCreator resultNodesCreator = new ResultNodesCreator();
    //    private final ExecutionStrategyUtil util = new ExecutionStrategyUtil();
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

        AtomicInteger count = new AtomicInteger();

        RootExecutionResultNode result = (RootExecutionResultNode) resultNodesTransformer.transformParallel(nadelContext.getForkJoinPool(), rootNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                count.incrementAndGet();
                ExecutionResultNode node = context.thisNode();
                if (node instanceof RootExecutionResultNode) {
                    RootExecutionResultNode changedRootNode = fetchTopLevelFields((RootExecutionResultNode) node,
                            executionContext,
                            serviceExecutionResult,
                            elapsedTimeForServiceCall,
                            normalizedQueryFromAst);
                    return TreeTransformerUtil.changeNode(context, changedRootNode);
                }
                if (!(node instanceof UnresolvedObjectResultNode)) {
                    return TraversalControl.CONTINUE;
                }
                ObjectExecutionResultNode resolvedNode = resolveUnresolvedNode(executionContext, (UnresolvedObjectResultNode) node, normalizedQueryFromAst, elapsedTimeForServiceCall);
                return TreeTransformerUtil.changeNode(context, resolvedNode);
            }
        });
//        System.out.println(count.get());
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("ServiceResultToResultNodes time: {} ms, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return result;
    }

    private ObjectExecutionResultNode resolveUnresolvedNode(ExecutionContext context,
                                                            UnresolvedObjectResultNode unresolvedNode,
                                                            NormalizedQueryFromAst normalizedQueryFromAst,
                                                            ElapsedTime elapsedTime) {
        ResolvedValue resolvedValue = unresolvedNode.getResolvedValue();
        NormalizedQueryField normalizedField = unresolvedNode.getNormalizedField();
        GraphQLObjectType resolvedType = unresolvedNode.getResolvedType();
        ExecutionPath executionPath = unresolvedNode.getExecutionPath();

        List<ExecutionResultNode> nodeChildren = new ArrayList<>(normalizedField.getChildren().size());
        for (NormalizedQueryField child : normalizedField.getChildren()) {
            if (child.getObjectType() == resolvedType) {
                ExecutionPath pathForChild = executionPath.segment(child.getResultKey());
                List<String> fieldIds = normalizedQueryFromAst.getFieldIds(child);

                FetchedValueAnalysis fetchedValueAnalysis = fetchAndAnalyzeField(context, unresolvedNode.getResolvedValue().getCompletedValue(), child, pathForChild, fieldIds);
                ExecutionResultNode executionResultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis).withElapsedTime(elapsedTime);
                nodeChildren.add(executionResultNode);
            }
        }
        return newObjectExecutionResultNode()
                .executionPath(unresolvedNode.getExecutionPath())
                .alias(normalizedField.getAlias())
                .fieldIds(unresolvedNode.getFieldIds())
                .objectType(normalizedField.getObjectType())
                .fieldDefinition(normalizedField.getFieldDefinition())
                .resolvedValue(resolvedValue)
                .children(nodeChildren)
                .elapsedTime(elapsedTime)
                .build();

    }

    private RootExecutionResultNode fetchTopLevelFields(RootExecutionResultNode rootNode,
                                                        ExecutionContext executionContext,
                                                        ServiceExecutionResult serviceExecutionResult,
                                                        ElapsedTime elapsedTime,
                                                        NormalizedQueryFromAst normalizedQueryFromAst) {
        List<NormalizedQueryField> topLevelFields = normalizedQueryFromAst.getTopLevelFields();

        ExecutionPath rootPath = ExecutionPath.rootPath();
        Object source = serviceExecutionResult.getData();

        List<ExecutionResultNode> children = new ArrayList<>(topLevelFields.size());
        for (NormalizedQueryField topLevelField : topLevelFields) {
            ExecutionPath path = rootPath.segment(topLevelField.getResultKey());
            List<String> fieldIds = normalizedQueryFromAst.getFieldIds(topLevelField);

            FetchedValueAnalysis fetchedValueAnalysis = fetchAndAnalyzeField(executionContext, source, topLevelField, path, fieldIds);
            ExecutionResultNode executionResultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis).withElapsedTime(elapsedTime);
            children.add(executionResultNode);
        }
        return (RootExecutionResultNode) rootNode.withNewChildren(children);
    }


    private FetchedValueAnalysis fetchAndAnalyzeField(ExecutionContext context,
                                                      Object source,
                                                      NormalizedQueryField normalizedQueryField,
                                                      ExecutionPath executionPath,
                                                      List<String> fieldIds) {
        FetchedValue fetchedValue = fetchValue(source, normalizedQueryField.getResultKey());
        return analyseValue(context, fetchedValue, normalizedQueryField, executionPath, fieldIds);
    }

    private FetchedValue fetchValue(Object source, String key) {
        if (source == null) {
            return FetchedValue.newFetchedValue()
                    .fetchedValue(null)
                    .rawFetchedValue(null)
                    .errors(Collections.emptyList())
                    .build();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) source;
        Object fetchedValue = map.get(key);
        return FetchedValue.newFetchedValue()
                .fetchedValue(fetchedValue)
                .rawFetchedValue(fetchedValue)
                .errors(Collections.emptyList())
                .build();
    }

    private FetchedValueAnalysis analyseValue(ExecutionContext executionContext,
                                              FetchedValue fetchedValue,
                                              NormalizedQueryField normalizedQueryField,
                                              ExecutionPath executionPath,
                                              List<String> fieldIds) {
        return fetchedValueAnalyzer.analyzeFetchedValue(executionContext, fetchedValue, normalizedQueryField, executionPath, fieldIds);
    }

}
