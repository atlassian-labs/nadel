package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static graphql.Assert.assertTrue;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

public abstract class FieldTransformation {


    private ApplyEnvironment environment;
    private String fieldId = UUID.randomUUID().toString();

    public abstract ApplyResult apply(ApplyEnvironment environment);

    /*
     * This is a bit strange method because n FieldTransformations map to one unapply method and we don't know the mapping until
     * this method is called. So we actually give all relevant transformations as a List
     */
    public abstract UnapplyResult unapplyResultNode(ExecutionResultNode executionResultNode,
                                                    List<FieldTransformation> allTransformations,
                                                    UnapplyEnvironment environment);

    public abstract AbstractNode getDefinition();

    public String getFieldId() {
        return fieldId;
    }

    public void setEnvironment(ApplyEnvironment environment) {
        this.environment = environment;
    }

    public ApplyEnvironment getApplyEnvironment() {
        return environment;
    }


    public Field getOriginalField() {
        return getApplyEnvironment().getField();
    }

    public GraphQLOutputType getOriginalFieldType() {
        return getApplyEnvironment().getFieldDefinition().getType();
    }

    public GraphQLFieldsContainer getOriginalFieldsContainer() {
        return getApplyEnvironment().getFieldsContainer();
    }

    public GraphQLFieldDefinition getOriginalFieldDefinition() {
        return getApplyEnvironment().getFieldDefinition();
    }


    protected ExecutionStepInfo replaceFieldsAndTypesWithOriginalValues(List<FieldTransformation> allTransformations, ExecutionStepInfo esi, ExecutionStepInfo parentEsi) {
        MergedField underlyingMergedField = esi.getField();
        List<Field> underlyingFields = underlyingMergedField.getFields();
        assertTrue(allTransformations.size() == underlyingFields.size());

        List<Field> newFields = new ArrayList<>();
        for (FieldTransformation fieldTransformation : allTransformations) {
            newFields.add(fieldTransformation.getOriginalField());
        }
        MergedField newMergedField = MergedField.newMergedField(newFields).build();
        FieldTransformation fieldTransformation = allTransformations.get(0);
        GraphQLOutputType originalFieldType = fieldTransformation.getOriginalFieldType();

        ExecutionStepInfo esiWithMappedField = esi.transform(builder -> {
                    builder
                            .field(newMergedField)
                            .fieldDefinition(allTransformations.get(0).getOriginalFieldDefinition())
                            .type(originalFieldType);
                    if (parentEsi != null && unwrapAll(parentEsi.getType()) instanceof GraphQLObjectType) {
                        builder.fieldContainer((GraphQLObjectType) unwrapAll(parentEsi.getType()));
                    }
                }

        );
        return esiWithMappedField;
    }


}
