package graphql.nadel.enginekt.transform.query

import graphql.normalized.NormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema

object NadelPathToField {
    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
    ): List<NormalizedField> {
        return createFieldRecursively(
            schema,
            parentType,
            pathToField,
            fieldChildren,
            pathToFieldIndex = 0,
        )
    }

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
    ): NormalizedField {
        return createParticularField(
            schema,
            parentType,
            pathToField,
            fieldChildren,
            pathToFieldIndex = 0,
        )
    }

    private fun createFieldRecursively(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: List<String>,
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
                    pathToField,
                    fieldChildren,
                    pathToFieldIndex,
                )
            }
            is GraphQLObjectType -> listOf(
                createParticularField(
                    schema,
                    parentType,
                    pathToField,
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
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
        pathToFieldIndex: Int,
    ): NormalizedField {
        val fieldName = pathToField[pathToFieldIndex]
        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return NormalizedField.newNormalizedField()
            .objectTypeNames(listOf(parentType.name))
            .fieldName(fieldName)
            .children(
                if (pathToFieldIndex == pathToField.lastIndex) {
                    fieldChildren
                } else {
                    createFieldRecursively(
                        schema,
                        parentType = fieldDef.type,
                        pathToField,
                        fieldChildren,
                        pathToFieldIndex = pathToFieldIndex + 1,
                    )
                }
            )
            .build()
    }
}
