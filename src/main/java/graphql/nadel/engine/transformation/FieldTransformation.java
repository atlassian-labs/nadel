package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static graphql.Assert.assertTrue;

public abstract class FieldTransformation {

    public static final String NADEL_FIELD_ID = "NADEL_FIELD_ID";


    /*
     * This is a bit strange method because n FieldTransformations map to one unapply method and we don't know the mapping until
     * this method is called. So we actually give all relevant transformations as a List
     */
    public abstract TraversalControl unapplyResultNode(ExecutionResultNode executionResultNode,
                                                       List<FieldTransformation> allTransformations,
                                                       UnapplyEnvironment environment);


    private QueryVisitorFieldEnvironment environment;
    private String fieldId = UUID.randomUUID().toString();

    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        this.environment = environment;
        return TraversalControl.CONTINUE;
    }

    public String getFieldId() {
        return fieldId;
    }

    public QueryVisitorFieldEnvironment getOriginalFieldEnvironment() {
        return environment;
    }

    public Field getOriginalField() {
        return getOriginalFieldEnvironment().getField();
    }

    public GraphQLOutputType getOriginalFieldType() {
        return getOriginalFieldEnvironment().getFieldDefinition().getType();
    }

    public GraphQLFieldsContainer getOriginalFieldsContainer() {
        return getOriginalFieldEnvironment().getFieldsContainer();
    }

    public GraphQLFieldDefinition getOriginalFieldDefinition() {
        return getOriginalFieldEnvironment().getFieldDefinition();
    }


    protected ExecutionStepInfo replaceFieldsAndTypesWithOriginalValues(List<FieldTransformation> allTransformations, ExecutionStepInfo esi) {
        MergedField underlyingMergedField = esi.getField();
        List<Field> underlyingFields = underlyingMergedField.getFields();
        assertTrue(allTransformations.size() == underlyingFields.size());

        List<Field> newFields = new ArrayList<>();
        for (FieldTransformation fieldTransformation : allTransformations) {
            newFields.add(fieldTransformation.getOriginalField());
        }
        MergedField newMergedField = MergedField.newMergedField(newFields).build();
        GraphQLOutputType originalFieldType = allTransformations.get(0).getOriginalFieldType();

        ExecutionStepInfo esiWithMappedField = esi.transform(builder -> builder
                .field(newMergedField)
                .type(originalFieldType)
        );
        return esiWithMappedField;
    }


}
