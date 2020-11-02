package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ResultPath;
import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.introspection.Introspection.SchemaMetaFieldDef;
import static graphql.introspection.Introspection.TypeMetaFieldDef;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;

@Internal
public class ExecutionResultNodeMapper {

    private PathMapper pathMapper = new PathMapper();

    public ExecutionResultNode mapERNFromUnderlyingToOverall(ExecutionResultNode node, UnapplyEnvironment environment) {

        Map<String, String> typeRenameMappings = environment.typeRenameMappings;
        GraphQLSchema overallSchema = environment.overallSchema;
        ResultPath mappedPath = pathMapper.mapPath(node.getResultPath(), node.getResultKey(), environment);
        GraphQLObjectType mappedObjectType = mapObjectType(node, typeRenameMappings, overallSchema);
        GraphQLFieldDefinition mappedFieldDefinition = getFieldDef(overallSchema, mappedObjectType, node.getFieldName());
        return node.transform(builder -> builder
                .executionPath(mappedPath)
                .objectType(mappedObjectType)
                .fieldDefinition(mappedFieldDefinition)
        );

    }

    private GraphQLObjectType mapObjectType(ExecutionResultNode node, Map<String, String> typeRenameMappings, GraphQLSchema overallSchema) {
        String objectTypeName = mapTypeName(typeRenameMappings, node.getObjectType().getName());
        GraphQLObjectType mappedObjectType = overallSchema.getObjectType(objectTypeName);
        assertNotNull(mappedObjectType, () -> String.format("object type %s not found in overall schema", objectTypeName));
        return mappedObjectType;
    }

    private String mapTypeName(Map<String, String> typeRenameMappings, String name) {
        return typeRenameMappings.getOrDefault(name, name);
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
        GraphQLFieldDefinition fieldDefinition = schema.getCodeRegistry().getFieldVisibility().getFieldDefinition(fieldsContainer, fieldName);
        return assertNotNull(fieldDefinition, () -> String.format("field '%s' not found in container '%s'", fieldName, fieldsContainer));
    }
}
