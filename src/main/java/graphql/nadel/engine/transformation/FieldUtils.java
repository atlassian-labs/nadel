package graphql.nadel.engine.transformation;

import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.language.AstTransformer;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.nadel.engine.FieldIdUtil;
import graphql.util.FpKit;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;

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
        return pathToFields(path, nadelFieldId, null);
    }

    public static Field pathToFields(List<String> path, String nadelFieldId, SelectionSet selectionSet) {
        Field curField = null;
        for (int ix = path.size() - 1; ix >= 0; ix--) {
            Field.Builder newField = Field.newField();
            FieldIdUtil.setFieldId(newField, nadelFieldId, ix == 0);
            if (ix == path.size() - 1 && selectionSet != null) {
                newField.selectionSet(selectionSet);
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

    public static Field addFieldIdToChildren(Field field, String id) {
        if (field.getSelectionSet() == null) {
            return field;
        }
        SelectionSet selectionSet = (SelectionSet) new AstTransformer().transform(field.getSelectionSet(), new NodeVisitorStub() {

            @Override
            public TraversalControl visitField(Field field, TraverserContext<Node> context) {
                return TreeTransformerUtil.changeNode(context, FieldIdUtil.addFieldId(field, id, false));
            }
        });
        return field.transform(builder -> builder.selectionSet(selectionSet));

    }


    public static ExecutionResultNode mapChildren(ExecutionResultNode executionResultNode, Function<ExecutionResultNode, ExecutionResultNode> mapper) {
        List<ExecutionResultNode> newChildren = FpKit.map(executionResultNode.getChildren(), mapper);
        return executionResultNode.withNewChildren(newChildren);
    }

}
