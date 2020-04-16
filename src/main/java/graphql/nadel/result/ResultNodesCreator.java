//package graphql.nadel.result;
//
//import graphql.Internal;
//import graphql.execution.NonNullableFieldWasNullException;
//import graphql.nadel.engine.FetchedValueAnalysis;
//import graphql.nadel.normalized.NormalizedQueryField;
//import graphql.schema.GraphQLTypeUtil;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//
//import static graphql.nadel.result.LeafExecutionResultNode.newLeafExecutionResultNode;
//import static graphql.nadel.result.UnresolvedObjectResultNode.newUnresolvedExecutionResultNode;
//
//@Internal
//public class ResultNodesCreator {
//
//    public ExecutionResultNode createResultNode(FetchedValueAnalysis fetchedValueAnalysis) {
//        Object resolvedValue = createResolvedValue(fetchedValueAnalysis);
//        NormalizedQueryField normalizedQueryField = fetchedValueAnalysis.getNormalizedQueryField();
//
//        boolean isNonNullType = GraphQLTypeUtil.isNonNull(fetchedValueAnalysis.getActualType());
//        if (fetchedValueAnalysis.isNullValue() && isNonNullType) {
//            //TODO: NoNullableFieldWasNullException doesn't work anymore because of ESI, replace it with a Nadel Exception
//            NonNullableFieldWasNullException nonNullableFieldWasNullException = new NonNullableFieldWasNullException(normalizedQueryField, fetchedValueAnalysis.getExecutionPath());
//            LeafExecutionResultNode result = newLeafExecutionResultNode()
//                    .executionPath(fetchedValueAnalysis.getExecutionPath())
//                    .alias(normalizedQueryField.getAlias())
//                    .fieldIds(fetchedValueAnalysis.getFieldIds())
//                    .objectType(normalizedQueryField.getObjectType())
//                    .fieldDefinition(normalizedQueryField.getFieldDefinition())
//                    .completedValue(resolvedValue)
////                    .nonNullableFieldWasNullException(nonNullableFieldWasNullException)
//                    .build();
//            return result;
//        }
//        if (fetchedValueAnalysis.isNullValue()) {
//            LeafExecutionResultNode result = newLeafExecutionResultNode()
//                    .alias(normalizedQueryField.getAlias())
//                    .fieldIds(fetchedValueAnalysis.getFieldIds())
//                    .objectType(normalizedQueryField.getObjectType())
//                    .fieldDefinition(normalizedQueryField.getFieldDefinition())
//                    .executionPath(fetchedValueAnalysis.getExecutionPath())
//                    .completedValue(resolvedValue)
//                    .build();
//            return result;
//        }
//        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT) {
//            return createUnresolvedNode(fetchedValueAnalysis);
//        }
//        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.LIST) {
//            return createListResultNode(fetchedValueAnalysis);
//        }
//        LeafExecutionResultNode result = newLeafExecutionResultNode()
//                .alias(normalizedQueryField.getAlias())
//                .fieldIds(fetchedValueAnalysis.getFieldIds())
//                .objectType(normalizedQueryField.getObjectType())
//                .fieldDefinition(normalizedQueryField.getFieldDefinition())
//                .executionPath(fetchedValueAnalysis.getExecutionPath())
//                .completedValue(resolvedValue)
//                .build();
//        return result;
//    }
//
//    private ExecutionResultNode createUnresolvedNode(FetchedValueAnalysis fetchedValueAnalysis) {
//        UnresolvedObjectResultNode result = newUnresolvedExecutionResultNode()
//                .executionPath(fetchedValueAnalysis.getExecutionPath())
//                .resolvedType(fetchedValueAnalysis.getResolvedType())
//                .fieldIds(fetchedValueAnalysis.getFieldIds())
//                .normalizedField(fetchedValueAnalysis.getNormalizedQueryField())
//                .completedValue(createResolvedValue(fetchedValueAnalysis))
//                .build();
//        return result;
//    }
//
//    private Object createResolvedValue(FetchedValueAnalysis fetchedValueAnalysis) {
//        return fetchedValueAnalysis.getCompletedValue();
//    }
//
//    private Optional<NonNullableFieldWasNullException> getFirstNonNullableException(Collection<ExecutionResultNode> collection) {
//        return collection.stream()
//                .filter(executionResultNode -> executionResultNode.getNonNullableFieldWasNullException() != null)
//                .map(ExecutionResultNode::getNonNullableFieldWasNullException)
//                .findFirst();
//    }
//
//    private ExecutionResultNode createListResultNode(FetchedValueAnalysis fetchedValueAnalysis) {
//        List<ExecutionResultNode> childrenNodes = new ArrayList<>(fetchedValueAnalysis.getChildren().size());
//        for (FetchedValueAnalysis child : fetchedValueAnalysis.getChildren()) {
//            childrenNodes.add(createResultNode(child));
//        }
//        NormalizedQueryField normalizedQueryField = fetchedValueAnalysis.getNormalizedQueryField();
//        ListExecutionResultNode result = ListExecutionResultNode.newListExecutionResultNode()
//                .alias(normalizedQueryField.getAlias())
//                .fieldIds(fetchedValueAnalysis.getFieldIds())
//                .objectType(normalizedQueryField.getObjectType())
//                .fieldDefinition(normalizedQueryField.getFieldDefinition())
//                .executionPath(fetchedValueAnalysis.getExecutionPath())
//                .completedValue(createResolvedValue(fetchedValueAnalysis))
//                .children(childrenNodes)
//                .build();
//        return result;
//    }
//}
