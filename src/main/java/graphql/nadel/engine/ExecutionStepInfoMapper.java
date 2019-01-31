package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldMappingTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionStepInfoMapper {


    public ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo executionStepInfo,
                                                  GraphQLSchema overallSchema,
                                                  Map<Field, FieldTransformation> transformationMap) {
        //TODO: handle __typename
        MergedField mergedField = executionStepInfo.getField();
        if (transformationMap.containsKey(mergedField.getSingleField())) {
            mergedField = unapplyTransformation(transformationMap.get(mergedField.getSingleField()), mergedField);
        }
        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();
        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        //TODO: the line below is not correct as it does not work list or non null types (since fieldType#getName will be null in that case)
        GraphQLOutputType mappedFieldType = (GraphQLOutputType) overallSchema.getType(fieldType.getName());
        GraphQLFieldDefinition fieldDefinition = executionStepInfo.getFieldDefinition();
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(fieldDefinition.getName());

        // TODO: map path

        MergedField finalMergedField = mergedField;
        return executionStepInfo.transform(builder -> builder
                .field(finalMergedField)
                .type(mappedFieldType)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition));

    }

    private MergedField unapplyTransformation(FieldTransformation fieldTransformation, MergedField mergedField) {
        if (fieldTransformation instanceof FieldMappingTransformation) {
            String originalName = ((FieldMappingTransformation) fieldTransformation).getOriginalName();
            List<Field> fields = mergedField
                    .getFields()
                    .stream()
                    .map(field -> field.transform(builder -> builder.name(originalName)))
                    .collect(Collectors.toList());
            return MergedField.newMergedField(fields).build();
        }
        return mergedField;
    }
}
