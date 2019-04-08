package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.util.FpKit;
import graphql.util.TraversalControl;

import java.util.List;

import static graphql.nadel.engine.transformation.FieldUtils.resultKeyForField;
import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends CopyFieldTransformation {
    private final FieldMappingDefinition mappingDefinition;
    private String originalName;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        originalName = environment.getField().getName();
        Field changedNode = environment.getField().transform(t -> t.name(mappingDefinition.getInputName()));
        this.resultKey = resultKeyForField(changedNode);
        return changeNode(environment.getTraverserContext(), changedNode);
    }

    @Override
    public MergedField unapplyMergedField(MergedField mergedField) {
        String originalName = getOriginalName();
        List<Field> fields = FpKit.map(mergedField.getFields(), field -> field.transform(builder -> builder.name(originalName)));
        return MergedField.newMergedField(fields).build();
    }

    public FieldMappingDefinition getMappingDefinition() {
        return mappingDefinition;
    }

    public String getOriginalName() {
        return originalName;
    }
}
