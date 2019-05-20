package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.dsl.CollapseDefinition;
import graphql.util.TraversalControl;

import java.util.List;
import java.util.UUID;

import static graphql.Assert.assertTrue;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.util.TreeTransformerUtil.changeNode;

public class CollapseTransformation extends AbstractFieldTransformation {
    private final CollapseDefinition collapseDefinition;

    public CollapseTransformation(CollapseDefinition collapseDefinition) {
        this.collapseDefinition = collapseDefinition;
    }


    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        String fieldId = UUID.randomUUID().toString();

        List<String> path = collapseDefinition.getPath();
        Field curField = null;
        for (int ix = path.size() - 1; ix >= 0; ix--) {
            Field.Builder newField = Field.newField();
            if (curField != null) {
                newField.selectionSet(newSelectionSet().selection(curField).build());
            }
            newField.name(path.get(ix));
            curField = newField.build();
        }
        Field finalCurField = curField;
        Field changedNode = finalCurField.transform(builder -> builder.additionalData(NADEL_FIELD_ID, fieldId));
        changeNode(environment.getTraverserContext(), changedNode);
        // skip traversing subtree because the fields are in respect to the underlying schema and not the overall which will break
        return TraversalControl.ABORT;
    }

    @Override
    public ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode) {
        ExecutionResultNode curNode = executionResultNode;
        while (curNode instanceof ObjectExecutionResultNode) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child along the collapse path");
            curNode = curNode.getChildren().get(0);
        }
        assertTrue(curNode instanceof LeafExecutionResultNode, "expecting only object results and at the end one leaf result along the collapse path");
        LeafExecutionResultNode leafExecutionResultNode = (LeafExecutionResultNode) curNode;
        // path and type is still wrong here
        return changeFieldInResultNode(leafExecutionResultNode, getOriginalField());
    }
}
