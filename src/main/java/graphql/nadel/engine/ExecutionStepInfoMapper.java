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

import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

public class ExecutionStepInfoMapper {


    public ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo parentExecutionStepInfo,
                                                  ExecutionStepInfo executionStepInfo,
                                                  GraphQLSchema overallSchema,
                                                  Map<Field, FieldTransformation> transformationMap) {
        ExecutionPath path = executionStepInfo.getPath();
        MergedField underlyingMergedField = executionStepInfo.getField();
        Field underlyingField = underlyingMergedField.getSingleField();
        FieldTransformation fieldTransformation = transformationMap.get(underlyingField);
        if (fieldTransformation != null) {
            underlyingMergedField = fieldTransformation.unapplyMergedField(underlyingMergedField);
        }
        MergedField mappedMergedField = underlyingMergedField;

        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();

        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        GraphQLOutputType mappedFieldType = mapOutputType(fieldType, overallSchema);
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(mappedMergedField.getName());

        ExecutionPath mappedPath = mapPath(parentExecutionStepInfo, path, mappedMergedField);

        return executionStepInfo.transform(builder -> builder
                .field(mappedMergedField)
                .type(mappedFieldType)
                .path(mappedPath)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition)
                .parentInfo(parentExecutionStepInfo)
        );

    }

    private ExecutionPath mapPath(ExecutionStepInfo parentExecutionStepInfo, ExecutionPath path, MergedField mergedField) {
        String fieldName = mergedField.getName();
        List<Object> segments = path.toList();
        for (int i = segments.size() - 1; i >= 0; i--) {
            Object segment = segments.get(i);
            if (segment instanceof String) {
                segments.set(i, fieldName);
                break;
            }
        }
        ExecutionPath parentPath = parentExecutionStepInfo.getPath();
        List<Object> parentSegments = parentPath.toList();
        //
        // Normally the parent path is the root path and hence there is nothing to add
        // but if we have a hydrated a field then we need to "merge" the paths not just append them
        // so for example
        //
        // /issue/reporter might lead to /userById and hence we need to collapse the top level hydrated field INTO the target field
        //
        if (!parentSegments.isEmpty()) {
            segments.remove(0);
            segments = FpKit.concat(parentSegments, segments);
        }
        path = ExecutionPath.fromList(segments);
        return path;
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
