package graphql.nadel.engine;

import graphql.Assert;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.introspection.Introspection.SchemaMetaFieldDef;
import static graphql.introspection.Introspection.TypeMetaFieldDef;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.schema.GraphQLTypeUtil.isNotWrapped;

public class ExecutionStepInfoMapper {

    private PathMapper pathMapper = new PathMapper();

    public ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo executionStepInfo, UnapplyEnvironment environment) {

        Map<String, String> typeRenameMappings = environment.typeRenameMappings;
        GraphQLSchema overallSchema = environment.overallSchema;
        ExecutionStepInfo parentExecutionStepInfo = environment.parentExecutionStepInfo;

        String fieldName = executionStepInfo.getField().getName();

        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();
        String fieldContainerName = mapTypeName(typeRenameMappings, fieldContainer.getName());

        GraphQLObjectType mappedFieldContainer = overallSchema.getObjectType(fieldContainerName);
        assertNotNull(mappedFieldContainer, "field container type " + fieldContainerName + " found in overall schema");
        GraphQLOutputType mappedFieldType = mapFieldType(fieldType, overallSchema, typeRenameMappings);
        GraphQLFieldDefinition mappedFieldDefinition = getFieldDef(overallSchema, mappedFieldContainer, fieldName);

        ExecutionPath mappedPath = pathMapper.mapPath(executionStepInfo, executionStepInfo.getField(), environment);

        return executionStepInfo.transform(builder -> builder
                .type(mappedFieldType)
                .path(mappedPath)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition)
                .parentInfo(parentExecutionStepInfo)
        );

    }

    private String mapTypeName(Map<String, String> typeRenameMappings, String name) {
        return typeRenameMappings.getOrDefault(name, name);
    }

    private GraphQLOutputType mapFieldType(GraphQLOutputType graphQLOutputType, GraphQLSchema overallSchema, Map<String, String> typeRenameMappings) {
        if (isNotWrapped(graphQLOutputType)) {
            String typeName = mapTypeName(typeRenameMappings, graphQLOutputType.getName());
            GraphQLOutputType outputType = (GraphQLOutputType) overallSchema.getType(typeName);
            return assertNotNull(outputType, "type " + graphQLOutputType.getName() + " not found in overall schema for field type");
        }
        if (isList(graphQLOutputType)) {
            return list(mapFieldType((GraphQLOutputType) ((GraphQLList) graphQLOutputType).getWrappedType(), overallSchema, typeRenameMappings));
        }
        if (isNonNull(graphQLOutputType)) {
            return nonNull(mapFieldType((GraphQLOutputType) ((GraphQLNonNull) graphQLOutputType).getWrappedType(), overallSchema, typeRenameMappings));
        }
        return Assert.assertShouldNeverHappen();
    }

    public static GraphQLFieldDefinition getFieldDef(GraphQLSchema schema, GraphQLCompositeType parentType, String fieldName) {
        if (schema.getQueryType() == parentType) {
            if (fieldName.equals(SchemaMetaFieldDef.getName())) {
                return SchemaMetaFieldDef;
            }
            if (fieldName.equals(TypeMetaFieldDef.getName())) {
                return TypeMetaFieldDef;
            }
        }
        if (fieldName.equals(TypeNameMetaFieldDef.getName())) {
            return TypeNameMetaFieldDef;
        }
        GraphQLFieldsContainer fieldsContainer = (GraphQLFieldsContainer) parentType;
        return schema.getCodeRegistry().getFieldVisibility().getFieldDefinition(fieldsContainer, fieldName);
    }
}
