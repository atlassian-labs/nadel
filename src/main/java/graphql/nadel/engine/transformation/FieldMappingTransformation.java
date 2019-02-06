package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.util.TraversalControl;

import static graphql.nadel.engine.transformation.FieldUtils.resultKeyForField;
import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldMappingTransformation extends CopyFieldTransformation {
    private final FieldMappingDefinition mappingDefinition;
    private String originalName;

    public FieldMappingTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        originalName = environment.getField().getName();
        Field changedNode = environment.getField().transform(t -> t.name(mappingDefinition.getInputName()));
        this.resultKey = resultKeyForField(changedNode);
        return changeNode(environment.getTraverserContext(), changedNode);
    }

    public FieldMappingDefinition getMappingDefinition() {
        return mappingDefinition;
    }

    public String getOriginalName() {
        return originalName;
    }
}
