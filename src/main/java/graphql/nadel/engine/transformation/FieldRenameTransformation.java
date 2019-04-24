package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.util.TraversalControl;

import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends AbstractFieldTransformation {
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        Field changedNode = environment.getField().transform(t -> t.name(mappingDefinition.getInputName()));
        return changeNode(environment.getTraverserContext(), changedNode);
    }
}
