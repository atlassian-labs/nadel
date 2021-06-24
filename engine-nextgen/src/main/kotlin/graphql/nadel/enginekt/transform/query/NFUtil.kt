package graphql.nadel.enginekt.transform.query

import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil

object NFUtil {
    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: QueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NormalizedField>,
    ): List<NormalizedField> {
        return createFieldRecursively(
            schema,
            parentType,
            pathToField,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
        )
    }

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: QueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NormalizedField>,
    ): NormalizedField {
        return createParticularField(
            schema,
            parentType,
            queryPathToField,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
        )
    }

    private fun createFieldRecursively(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        queryPathToField: QueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NormalizedField>,
        pathToFieldIndex: Int,
    ): List<NormalizedField> {
        // Note: remember that we are creating fields that do not exist in the original NF
        // Thus, we need to handle interfaces and object types
        return when (parentType) {
            is GraphQLInterfaceType -> schema.getImplementations(parentType).map { objectType: GraphQLObjectType ->
                createParticularField(
                    schema,
                    parentType = objectType,
                    queryPathToField,
                    fieldArguments,
                    fieldChildren,
                    pathToFieldIndex,
                )
            }
            is GraphQLObjectType -> listOf(
                createParticularField(
                    schema,
                    parentType,
                    queryPathToField,
                    fieldArguments,
                    fieldChildren,
                    pathToFieldIndex,
                )
            )
            else -> error("Unsupported type '${parentType.javaClass.name}'")
        }
    }

    private fun createParticularField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: QueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NormalizedField>,
        pathToFieldIndex: Int,
    ): NormalizedField {
        val fieldName = queryPathToField.segments[pathToFieldIndex]
        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return NormalizedField.newNormalizedField()
            .objectTypeNames(listOf(parentType.name))
            .fieldName(fieldName)
            .also { builder ->
                if (pathToFieldIndex == queryPathToField.segments.lastIndex) {
                    builder.normalizedArguments(fieldArguments)
                }
            }
            .children(
                if (pathToFieldIndex == queryPathToField.segments.lastIndex) {
                    fieldChildren
                } else {
                    createFieldRecursively(
                        schema,
                        parentType = GraphQLTypeUtil.unwrapAllAs(fieldDef.type),
                        queryPathToField,
                        fieldArguments,
                        fieldChildren,
                        pathToFieldIndex = pathToFieldIndex + 1,
                    )
                }
            )
            .build()
            .also { field ->
                // Set parents correctly
                field.children.forEach { child ->
                    child.replaceParent(field)
                }
            }
    }
}
