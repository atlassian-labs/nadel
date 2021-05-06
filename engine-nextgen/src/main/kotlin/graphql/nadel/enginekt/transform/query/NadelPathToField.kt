package graphql.nadel.enginekt.transform.query

import graphql.normalized.NormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

object NadelPathToField {
    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
    ): List<NormalizedField> {
        return createField(
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
        return createField(
            schema,
            parentType,
            pathToField,
            fieldChildren,
            pathToFieldIndex = 0,
        )
    }

    private fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
        pathToFieldIndex: Int,
    ): List<NormalizedField> {
        return when (parentType) {
            is GraphQLInterfaceType -> schema.getImplementations(parentType).map { objectType: GraphQLObjectType ->
                createField(
                    schema,
                    parentType = objectType,
                    pathToField,
                    fieldChildren,
                    pathToFieldIndex,
                )
            }
            is GraphQLUnionType -> parentType.types.flatMap { typeInUnion: GraphQLOutputType ->
                createField(
                    schema,
                    parentType = typeInUnion,
                    pathToField,
                    fieldChildren,
                    pathToFieldIndex,
                )
            }
            is GraphQLObjectType -> listOf(
                createField(
                    schema,
                    parentType,
                    pathToField,
                    fieldChildren,
                    pathToFieldIndex,
                )
            )
            else -> error("Unknown type '${parentType.javaClass.name}'")
        }
    }

    private fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        pathToField: List<String>,
        fieldChildren: List<NormalizedField>,
        pathToFieldIndex: Int,
    ): NormalizedField {
        val fieldName = pathToField[pathToFieldIndex]
        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return NormalizedField.newQueryExecutionField()
            .objectType(parentType)
            .fieldDefinition(fieldDef)
            .children(
                if (pathToFieldIndex == pathToField.lastIndex) {
                    fieldChildren
                } else {
                    createField(
                        schema,
                        parentType = fieldDef.type,
                        pathToField,
                        fieldChildren,
                        pathToFieldIndex = pathToFieldIndex + 1,
                    )
                }
            )
            .build()
            .also { nf ->
                // Fixup parents
                nf.children.forEach {
                    it.replaceParent(nf)
                }
            }
    }
}
