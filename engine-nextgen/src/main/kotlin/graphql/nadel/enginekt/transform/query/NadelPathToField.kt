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
        pathToSourceField: List<String>,
        sourceFieldChildren: List<NormalizedField>,
    ): List<NormalizedField> {
        return createField(
            schema,
            parentType,
            pathToSourceField,
            sourceFieldChildren,
            pathToSourceFieldIndex = 0,
        )
    }

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        pathToSourceField: List<String>,
        sourceFieldChildren: List<NormalizedField>,
    ): NormalizedField {
        return createField(
            schema,
            parentType,
            pathToSourceField,
            sourceFieldChildren,
            pathToSourceFieldIndex = 0,
        )
    }

    private fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToSourceField: List<String>,
        sourceFieldChildren: List<NormalizedField>,
        pathToSourceFieldIndex: Int,
    ): List<NormalizedField> {
        return when (parentType) {
            is GraphQLInterfaceType -> schema.getImplementations(parentType).map { objectType: GraphQLObjectType ->
                createField(
                    schema,
                    parentType = objectType,
                    pathToSourceField,
                    sourceFieldChildren,
                    pathToSourceFieldIndex,
                )
            }
            is GraphQLUnionType -> parentType.types.flatMap { typeInUnion: GraphQLOutputType ->
                createField(
                    schema,
                    parentType = typeInUnion,
                    pathToSourceField,
                    sourceFieldChildren,
                    pathToSourceFieldIndex,
                )
            }
            is GraphQLObjectType -> listOf(
                createField(
                    schema,
                    parentType,
                    pathToSourceField,
                    sourceFieldChildren,
                    pathToSourceFieldIndex,
                )
            )
            else -> error("Unknown type '${parentType.javaClass.name}'")
        }
    }

    private fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        pathToSourceField: List<String>,
        sourceFieldChildren: List<NormalizedField>,
        pathToSourceFieldIndex: Int,
    ): NormalizedField {
        val fieldName = pathToSourceField[pathToSourceFieldIndex]
        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return NormalizedField.newQueryExecutionField()
            .objectType(parentType)
            .fieldDefinition(fieldDef)
            .children(
                if (pathToSourceFieldIndex == pathToSourceField.lastIndex) {
                    sourceFieldChildren
                } else {
                    createField(
                        schema,
                        parentType = fieldDef.type,
                        pathToSourceField,
                        sourceFieldChildren,
                        pathToSourceFieldIndex = pathToSourceFieldIndex + 1,
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
