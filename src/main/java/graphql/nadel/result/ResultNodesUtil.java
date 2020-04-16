package graphql.nadel.result;

import graphql.Assert;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
import graphql.util.NodeLocation;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static graphql.nadel.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Internal
public class ResultNodesUtil {

    public static ExecutionResult toExecutionResult(RootExecutionResultNode root) {
        ExecutionResultData executionResultData = toDataImpl(root);
        return ExecutionResultImpl.newExecutionResult()
                .data(executionResultData.data)
                .errors(executionResultData.errors)
                .build();
    }

    private static class ExecutionResultData {
        Object data;
        List<GraphQLError> errors;


        public ExecutionResultData(Object data, List<GraphQLError> errors) {
            this.data = data;
            this.errors = errors;
        }
    }


    private static ExecutionResultData data(Object data, ExecutionResultNode executionResultNode) {
        List<GraphQLError> allErrors = new ArrayList<>();
        allErrors.addAll(executionResultNode.getErrors());
        return new ExecutionResultData(data, allErrors);
    }

    private static ExecutionResultData data(Object data, List<GraphQLError> errors) {
        return new ExecutionResultData(data, errors);
    }

    private static ExecutionResultData data(Object data, NonNullableFieldWasNullError error) {
        return new ExecutionResultData(data, Collections.singletonList(error));
    }

    private static ExecutionResultData toDataImpl(ExecutionResultNode root) {
        if (root instanceof LeafExecutionResultNode) {
            return root.isNullValue() ? data(null, root) : data(root.getCompletedValue(), root);
        }
        if (root instanceof ListExecutionResultNode) {
            Optional<NonNullableFieldWasNullError> childNonNullableException = root.getChildNonNullableError();
            if (childNonNullableException.isPresent()) {
                return data(null, childNonNullableException.get());
            }

            List<GraphQLError> errors = new ArrayList<>();
            List<Object> data = new ArrayList<>();
            for (ExecutionResultNode child : root.getChildren()) {
                ExecutionResultData erd = toDataImpl(child);
                data.add(erd.data);
                if (!erd.errors.isEmpty()) {
                    errors.addAll(erd.errors);
                }
            }
            if (!root.getErrors().isEmpty()) {
                errors.addAll(root.getErrors());
            }
            return data(data, errors);
        }

        if (root instanceof UnresolvedObjectResultNode) {
            return data("Not resolved : " + root.getExecutionPath() + " with field " + root.getFieldName(), emptyList());
        }
        if (root instanceof ObjectExecutionResultNode) {
            Optional<NonNullableFieldWasNullError> childNonNullableFieldError = root.getChildNonNullableError();
            if (childNonNullableFieldError.isPresent()) {
                return data(null, childNonNullableFieldError.get());
            }
            Map<String, Object> resultMap = new LinkedHashMap<>();
            List<GraphQLError> errors = new ArrayList<>();
            root.getChildren().forEach(child -> {
                ExecutionResultData executionResultData = toDataImpl(child);
                resultMap.put(child.getResultKey(), executionResultData.data);
                errors.addAll(executionResultData.errors);
            });
            errors.addAll(root.getErrors());
            return data(resultMap, errors);
        }
        return Assert.assertShouldNeverHappen("An unexpected root type %s", root.getClass());
    }


    public static Optional<NonNullableFieldWasNullError> getFirstNonNullableError(Collection<ExecutionResultNode> collection) {
        return collection.stream()
                .filter(executionResultNode -> executionResultNode.getNonNullableFieldWasNullError() != null)
                .map(ExecutionResultNode::getNonNullableFieldWasNullError)
                .findFirst();
    }


    public static NonNullableFieldWasNullError bubbledUpNonNullableError(GraphQLFieldDefinition fieldDefinition,
                                                                         ExecutionPath executionPath,
                                                                         Collection<ExecutionResultNode> children) {
        // can only happen for the root node
        if (fieldDefinition == null) {
            return null;
        }
        GraphQLOutputType actualType = getActualType(fieldDefinition, executionPath);
        if (GraphQLTypeUtil.isNonNull(actualType)) {
            Optional<NonNullableFieldWasNullError> firstNonNullableException = getFirstNonNullableError(children);
            if (firstNonNullableException.isPresent()) {
                return new NonNullableFieldWasNullError((GraphQLNonNull) actualType, executionPath);
            }

        }
        return null;
    }

    private static GraphQLOutputType getActualType(GraphQLFieldDefinition fieldDefinition, ExecutionPath executionPath) {
        // example: field definition type: [[String]!]!, path: /foo/bar/type[3] => result is [String]!
        GraphQLOutputType result = fieldDefinition.getType();
        while (executionPath.isListSegment()) {
            executionPath = executionPath.dropSegment();
            // might be non null or not
            result = (GraphQLOutputType) GraphQLTypeUtil.unwrapNonNull(result);
            Assert.assertTrue(result instanceof GraphQLList);
            result = (GraphQLOutputType) ((GraphQLList) result).getWrappedType();
        }
        return result;

    }

    public static List<NodeZipper<ExecutionResultNode>> getUnresolvedNodes(Collection<ExecutionResultNode> roots) {
        List<NodeZipper<ExecutionResultNode>> result = new ArrayList<>();

        ResultNodeTraverser traverser = ResultNodeTraverser.depthFirst();
        traverser.traverse(new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof UnresolvedObjectResultNode) {
                    result.add(new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), RESULT_NODE_ADAPTER));
                }
                return TraversalControl.CONTINUE;
            }

        }, roots);
        return result;
    }

    public static NodeMultiZipper<ExecutionResultNode> getUnresolvedNodes(ExecutionResultNode root) {
        List<NodeZipper<ExecutionResultNode>> unresolvedNodes = getUnresolvedNodes(singleton(root));
        return new NodeMultiZipper<>(root, unresolvedNodes, RESULT_NODE_ADAPTER);
    }


    public static NodeLocation key(String name) {
        return new NodeLocation(name, 0);
    }

    public static NodeLocation index(int index) {
        return new NodeLocation(null, index);
    }

}
