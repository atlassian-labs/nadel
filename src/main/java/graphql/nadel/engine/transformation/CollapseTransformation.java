package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.dsl.CollapseDefinition;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;
import java.util.UUID;

import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.getLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.pathToFields;
import static graphql.util.TreeTransformerUtil.changeNode;

public class CollapseTransformation extends FieldTransformation {
    private final CollapseDefinition collapseDefinition;

    public CollapseTransformation(CollapseDefinition collapseDefinition) {
        this.collapseDefinition = collapseDefinition;
    }


    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        String fieldId = UUID.randomUUID().toString();

        List<String> path = collapseDefinition.getPath();
        Field finalCurField = pathToFields(path);
        Field changedNode = finalCurField.transform(builder -> builder.additionalData(NADEL_FIELD_ID, fieldId));
        changeNode(environment.getTraverserContext(), changedNode);
        // skip traversing subtree because the fields are in respect to the underlying schema and not the overall which will break
        return TraversalControl.ABORT;
    }


    @Override
    public ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode, List<FieldTransformation> transformations, UnapplyEnvironment environment) {
        LeafExecutionResultNode leafExecutionResultNode = getLeafNode(executionResultNode);
        // path and type is still wrong here
        return changeFieldInResultNode(leafExecutionResultNode, getOriginalField());
    }


}
