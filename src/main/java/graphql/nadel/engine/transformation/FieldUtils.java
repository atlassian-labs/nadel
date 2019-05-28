package graphql.nadel.engine.transformation;

import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.language.Field;
import graphql.util.FpKit;

import java.util.List;
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

    public static Field pathToFields(List<String> path, String nadelFieldId) {
        Field curField = null;
        for (int ix = path.size() - 1; ix >= 0; ix--) {
            Field.Builder newField = Field.newField().additionalData(FieldTransformation.NADEL_FIELD_ID, nadelFieldId);
            if (curField != null) {
                newField.selectionSet(newSelectionSet().selection(curField).build());
            }
            newField.name(path.get(ix));
            curField = newField.build();
        }
        return curField;
    }

    public static LeafExecutionResultNode getLeafNode(ExecutionResultNode executionResultNode) {
        ExecutionResultNode curNode = executionResultNode;
        while (curNode instanceof ObjectExecutionResultNode) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child ");
            curNode = curNode.getChildren().get(0);
        }
        assertTrue(curNode instanceof LeafExecutionResultNode, "expecting only object results and at the end one leaf");
        return (LeafExecutionResultNode) curNode;
    }

    public static ExecutionResultNode mapChildren(ExecutionResultNode executionResultNode, Function<ExecutionResultNode, ExecutionResultNode> mapper) {
        List<ExecutionResultNode> newChildren = FpKit.map(executionResultNode.getChildren(), mapper);
        return executionResultNode.withNewChildren(newChildren);
    }

}
