package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.dsl.CollapseDefinition;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;

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

        List<String> path = collapseDefinition.getPath();
        Field finalCurField = pathToFields(path, getFieldId());
        changeNode(environment.getTraverserContext(), finalCurField);
        // skip traversing subtree because the fields are in respect to the underlying schema and not the overall which will break
        return TraversalControl.ABORT;
    }


    @Override
    public TraversalControl unapplyResultNode(ExecutionResultNode executionResultNode, List<FieldTransformation> transformations, UnapplyEnvironment environment) {
        LeafExecutionResultNode leafExecutionResultNode = getLeafNode(executionResultNode);
        // path and type is still wrong here
        LeafExecutionResultNode leafNode = changeFieldInResultNode(leafExecutionResultNode, getOriginalField());
        changeNode(environment.context, leafNode);
        return TraversalControl.ABORT;
    }


}
