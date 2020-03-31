package graphql.nadel.engine.transformation;

import graphql.language.AstTransformer;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.util.FpKit;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static graphql.Assert.assertTrue;
import static graphql.language.SelectionSet.newSelectionSet;

public final class FieldUtils {

    /**
     * This returns the aliased result name if a field is alised other its the field name
     *
     * @param field the field in play
     *
     * @return the result name
     */
    public static String resultKeyForField(Field field) {
        return field.getAlias() != null ? field.getAlias() : field.getName();
    }

    public static Field pathToFields(List<String> path,
                                     String nadelFieldId,
                                     List<String> additionalIds,
                                     boolean firstRootOfTransformation,
                                     Map<String, List<FieldMetadata>> metadataByFieldId) {
        return pathToFields(path, nadelFieldId, additionalIds, firstRootOfTransformation, null, metadataByFieldId);
    }

    public static Field pathToFields(List<String> path,
                                     String nadelFieldId,
                                     List<String> additionalIds,
                                     boolean firstRootOfTransformation,
                                     SelectionSet lastSelectionSet,
                                     Map<String, List<FieldMetadata>> metadataByFieldId) {
        Field curField = null;
        for (int ix = path.size() - 1; ix >= 0; ix--) {
            Field.Builder newField = Field.newField();
            String fieldId = UUID.randomUUID().toString();
            newField.additionalData(NodeId.ID, fieldId);
            FieldMetadataUtil.setFieldMetadata(fieldId, nadelFieldId, additionalIds, ix == 0 && firstRootOfTransformation, metadataByFieldId);
            if (ix == path.size() - 1 && lastSelectionSet != null) {
                newField.selectionSet(lastSelectionSet);
            }
            if (curField != null) {
                newField.selectionSet(newSelectionSet().selection(curField).build());
            }
            newField.name(path.get(ix));
            curField = newField.build();
        }
        return curField;
    }

    public static LeafExecutionResultNode geFirstLeafNode(ExecutionResultNode executionResultNode) {
        ExecutionResultNode curNode = executionResultNode;
        while (curNode instanceof ObjectExecutionResultNode) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child ");
            curNode = curNode.getChildren().get(0);
        }
        assertTrue(curNode instanceof LeafExecutionResultNode, "expecting only object results and at the end one leaf");
        return (LeafExecutionResultNode) curNode;
    }


    public static ExecutionResultNode getSubTree(ExecutionResultNode executionResultNode, int levels) {
        ExecutionResultNode curNode = executionResultNode;
        int curLevel = 0;
        while (curNode.getChildren().size() > 0 && curLevel++ < levels) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child ");
            curNode = curNode.getChildren().get(0);
            if (curNode instanceof LeafExecutionResultNode) {
                return curNode;
            }
        }
        return curNode;
    }

    public static void addFieldIdToChildren(Field field, String id, Map<String, List<FieldMetadata>> metadataByFieldId) {
        if (field.getSelectionSet() == null) {
            return;
        }
        //TODO: make it go down fragments
        new AstTransformer().transform(field.getSelectionSet(), new NodeVisitorStub() {

            @Override
            public TraversalControl visitField(Field field, TraverserContext<Node> context) {
                FieldMetadataUtil.addFieldMetadata(field, id, false, metadataByFieldId);
                return TraversalControl.CONTINUE;
            }
        });

    }


    public static ExecutionResultNode mapChildren(ExecutionResultNode executionResultNode, Function<ExecutionResultNode, ExecutionResultNode> mapper) {
        List<ExecutionResultNode> newChildren = FpKit.map(executionResultNode.getChildren(), mapper);
        return executionResultNode.withNewChildren(newChildren);
    }

}
