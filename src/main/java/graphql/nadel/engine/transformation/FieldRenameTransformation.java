package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.util.TraversalControl;

import java.util.UUID;

import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends AbstractFieldTransformation {
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        String fieldId = UUID.randomUUID().toString();
        Field changedNode = environment.getField().transform(t -> t.name(mappingDefinition.getInputName()).additionalData(NADEL_FIELD_ID, fieldId));
        return changeNode(environment.getTraverserContext(), changedNode);
    }
}
