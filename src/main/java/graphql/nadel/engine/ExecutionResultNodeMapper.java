package graphql.nadel.engine;

import graphql.execution.ExecutionPath;
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

public class ExecutionResultNodeMapper {

    private PathMapper pathMapper = new PathMapper();

    // it takes into account type renames and a special path mapping for hydration paths
    public ExecutionResultNode mapERNFromUnderlyingToOverall(ExecutionResultNode node, UnapplyEnvironment environment) {

        Map<String, String> typeRenameMappings = environment.typeRenameMappings;
        GraphQLSchema overallSchema = environment.overallSchema;

        // maybe we need that later
//        GraphQLOutputType mappedType = mapFieldType(executionStepInfo, typeRenameMappings, overallSchema);
        ExecutionPath mappedPath = pathMapper.mapPath(node.getExecutionPath(), node.getField(), environment);

        GraphQLObjectType mappedObjectType = mapObjectType(node, typeRenameMappings, overallSchema);
        GraphQLFieldDefinition mappedFieldDefinition = getFieldDef(overallSchema, mappedObjectType, node.getField().getName());

        return node.transform(builder -> builder
                .executionPath(mappedPath)
                .objectType(mappedObjectType)
                .fieldDefinition(mappedFieldDefinition)
        );

    }

    private GraphQLObjectType mapObjectType(ExecutionResultNode node, Map<String, String> typeRenameMappings, GraphQLSchema overallSchema) {
        String objectTypeName = mapTypeName(typeRenameMappings, node.getObjectType().getName());
        GraphQLObjectType mappedObjectType = overallSchema.getObjectType(objectTypeName);
        assertNotNull(mappedObjectType, "object type %s not found in overall schema", objectTypeName);
        return mappedObjectType;
    }

//    private GraphQLOutputType mapFieldType(ExecutionStepInfo executionStepInfo, Map<String, String> typeRenameMappings, GraphQLSchema overallSchema) {
//        return mapFieldType(executionStepInfo.getType(), overallSchema, typeRenameMappings);
//    }

    private String mapTypeName(Map<String, String> typeRenameMappings, String name) {
        return typeRenameMappings.getOrDefault(name, name);
    }

//    private GraphQLOutputType mapFieldType(GraphQLOutputType graphQLOutputType,
//                                           GraphQLSchema overallSchema,
//                                           Map<String, String> typeRenameMappings) {
//        if (isNotWrapped(graphQLOutputType)) {
//            String typeName = mapTypeName(typeRenameMappings, ((GraphQLNamedOutputType) graphQLOutputType).getName());
//            GraphQLOutputType outputType = (GraphQLOutputType) overallSchema.getType(typeName);
//            return assertNotNull(outputType, "type %s not found in overall schema for field type", ((GraphQLNamedOutputType) graphQLOutputType).getName());
//        }
//        if (isList(graphQLOutputType)) {
//            return list(mapFieldType((GraphQLOutputType) ((GraphQLList) graphQLOutputType).getWrappedType(), overallSchema, typeRenameMappings));
//        }
//        if (isNonNull(graphQLOutputType)) {
//            return nonNull(mapFieldType((GraphQLOutputType) ((GraphQLNonNull) graphQLOutputType).getWrappedType(), overallSchema, typeRenameMappings));
//        }
//        return Assert.assertShouldNeverHappen();
//    }

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
        return assertNotNull(fieldDefinition, "field '%s' not found in container '%s'", fieldName, fieldsContainer);
    }
}
