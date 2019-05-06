package graphql.nadel.engine;

import graphql.Assert;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

public class ExecutionStepInfoMapper {


    public ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo parentExecutionStepInfo,
                                                  ExecutionStepInfo executionStepInfo,
                                                  GraphQLSchema overallSchema,
                                                  boolean isHydrationTransformation,
                                                  boolean batched,
                                                  Map<Field, FieldTransformation> transformationMap) {
        MergedField underlyingMergedField = executionStepInfo.getField();
        List<Field> newFields = new ArrayList<>();
        for (Field underlyingField : underlyingMergedField.getFields()) {
            FieldTransformation fieldTransformation = transformationMap.get(underlyingField);
            if (fieldTransformation != null) {
                newFields.add(fieldTransformation.unapplyField(underlyingField));
            } else {
                newFields.add(underlyingField);
            }
        }

        MergedField mappedMergedField = MergedField.newMergedField(newFields).build();

        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();

        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        GraphQLOutputType mappedFieldType = mapOutputType(fieldType, overallSchema);
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(mappedMergedField.getName());

        ExecutionPath mappedPath = mapPath(parentExecutionStepInfo, executionStepInfo, isHydrationTransformation, batched, mappedMergedField);

        return executionStepInfo.transform(builder -> builder
                .field(mappedMergedField)
                .type(mappedFieldType)
                .path(mappedPath)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition)
                .parentInfo(parentExecutionStepInfo)
        );

    }

    private ExecutionPath mapPath(ExecutionStepInfo parentExecutionStepInfo, ExecutionStepInfo fieldStepInfo, boolean isHydrationTransformation, boolean batched, MergedField mergedField) {
        List<Object> fieldSegments = patchLastFieldName(fieldStepInfo, mergedField);
        ExecutionPath parentPath = parentExecutionStepInfo.getPath();
        if (isHydrationTransformation) {
            //
            // Normally the parent path is all ok and hence there is nothing to add
            // but if we have a hydrated a field then we need to "merge" the paths not just append them
            // so for example
            //
            // /issue/reporter might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
            fieldSegments.remove(0);
            if (batched) {
                fieldSegments.remove(0);
            }
            fieldSegments = FpKit.concat(parentPath.toList(), fieldSegments);
        }
        ExecutionPath newPath = ExecutionPath.fromList(fieldSegments);
        return newPath;
    }

    private List<Object> patchLastFieldName(ExecutionStepInfo fieldStepInfo, MergedField mergedField) {
        String fieldName = mergedField.getName();
        ExecutionPath fieldPath = fieldStepInfo.getPath();
        List<Object> fieldSegments = fieldPath.toList();
        for (int i = fieldSegments.size() - 1; i >= 0; i--) {
            Object segment = fieldSegments.get(i);
            if (segment instanceof String) {
                fieldSegments.set(i, fieldName);
                break;
            }
        }
        return fieldSegments;
    }

    private GraphQLOutputType mapOutputType(GraphQLOutputType graphQLOutputType, GraphQLSchema overallSchema) {
        if (GraphQLTypeUtil.isNotWrapped(graphQLOutputType)) {
            return assertNotNull((GraphQLOutputType) overallSchema.getType(graphQLOutputType.getName()), "type " + graphQLOutputType.getName() + " not found in overall schema");
        }
        if (GraphQLTypeUtil.isList(graphQLOutputType)) {
            return GraphQLList.list(mapOutputType((GraphQLOutputType) ((GraphQLList) graphQLOutputType).getWrappedType(), overallSchema));
        }
        if (GraphQLTypeUtil.isNonNull(graphQLOutputType)) {
            return GraphQLNonNull.nonNull(mapOutputType((GraphQLOutputType) ((GraphQLNonNull) graphQLOutputType).getWrappedType(), overallSchema));
        }
        return Assert.assertShouldNeverHappen();
    }

}
