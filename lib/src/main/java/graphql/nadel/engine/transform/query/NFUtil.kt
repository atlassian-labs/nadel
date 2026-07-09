package graphql.nadel.engine.transform.query

import graphql.nadel.engine.compiler.NadelExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.normalized.incremental.NormalizedDeferredExecution
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil

object NFUtil {
    // Exists for IntelliJ search everywhere purposes, DO NOT INVOKE
    private fun pathToField(unit: Unit): Nothing = error("no-op")

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: NadelQueryPath,
        aliasedPath: NadelQueryPath? = null,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NadelExecutableNormalizedField>,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution> = LinkedHashSet(),
    ): List<NadelExecutableNormalizedField> {
        return createFieldRecursively(
            schema,
            parentType,
            pathToField,
            aliasedPath,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
            deferredExecutions,
        )
    }

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: NadelQueryPath,
        aliasedPath: NadelQueryPath? = null,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NadelExecutableNormalizedField>,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution> = LinkedHashSet(),
    ): NadelExecutableNormalizedField {
        return createParticularField(
            schema,
            parentType,
            queryPathToField,
            aliasedPath,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
            deferredExecutions,
        )
    }

    private fun createFieldRecursively(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        queryPathToField: NadelQueryPath,
        aliasedPath: NadelQueryPath?,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NadelExecutableNormalizedField>,
        pathToFieldIndex: Int,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution>,
    ): List<NadelExecutableNormalizedField> {
        // Note: remember that we are creating fields that do not exist in the original NF
        // Thus, we need to handle interfaces and object types
        return when (parentType) {
            is GraphQLInterfaceType -> schema.getImplementations(parentType).map { objectType: GraphQLObjectType ->
                createParticularField(
                    schema,
                    parentType = objectType,
                    queryPathToField,
                    aliasedPath,
                    fieldArguments,
                    fieldChildren,
                    pathToFieldIndex,
                    deferredExecutions,
                )
            }
            is GraphQLObjectType -> listOf(
                createParticularField(
                    schema,
                    parentType,
                    queryPathToField,
                    aliasedPath,
                    fieldArguments,
                    fieldChildren,
                    pathToFieldIndex,
                    deferredExecutions,
                )
            )
            else -> error("Unsupported type '${parentType.javaClass.name}'")
        }
    }

    private fun createParticularField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: NadelQueryPath,
        aliasedPath: NadelQueryPath?,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<NadelExecutableNormalizedField>,
        pathToFieldIndex: Int,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution>,
    ): NadelExecutableNormalizedField {
        if (aliasedPath != null && aliasedPath.size != queryPathToField.size) {
            error("Aliased path must have the same length as the query path")
        }

        val fieldName = queryPathToField.segments[pathToFieldIndex]

        val fieldAlias = aliasedPath?.segments?.get(pathToFieldIndex).takeIf { it != fieldName }

        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return NadelExecutableNormalizedField.newNormalizedField()
            .objectTypeNames(listOf(parentType.name))
            .fieldName(fieldName)
            .deferredExecutions(deferredExecutions)
            .alias(fieldAlias)
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
                        aliasedPath,
                        fieldArguments,
                        fieldChildren,
                        pathToFieldIndex = pathToFieldIndex + 1,
                        deferredExecutions,
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
