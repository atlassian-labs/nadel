package graphql.nadel.engine.transform.query

import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.normalized.incremental.NormalizedDeferredExecution
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil
import kotlin.collections.LinkedHashSet

object NFUtil {
    // Exists for IntelliJ search everywhere purposes, DO NOT INVOKE
    private fun pathToField(unit: Unit): Nothing = error("no-op")

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        pathToField: NadelQueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution> = LinkedHashSet(),
    ): List<ExecutableNormalizedField> {
        return createFieldRecursively(
            schema,
            parentType,
            pathToField,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
            deferredExecutions
        )
    }

    fun createField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: NadelQueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution> = LinkedHashSet(),
    ): ExecutableNormalizedField {
        return createParticularField(
            schema,
            parentType,
            queryPathToField,
            fieldArguments,
            fieldChildren,
            pathToFieldIndex = 0,
            deferredExecutions
        )
    }

    private fun createFieldRecursively(
        schema: GraphQLSchema,
        parentType: GraphQLOutputType,
        queryPathToField: NadelQueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
        pathToFieldIndex: Int,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution>,
    ): List<ExecutableNormalizedField> {
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
                    deferredExecutions
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
                    deferredExecutions
                )
            )
            else -> error("Unsupported type '${parentType.javaClass.name}'")
        }
    }

    private fun createParticularField(
        schema: GraphQLSchema,
        parentType: GraphQLObjectType,
        queryPathToField: NadelQueryPath,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
        pathToFieldIndex: Int,
        deferredExecutions: LinkedHashSet<NormalizedDeferredExecution>,
    ): ExecutableNormalizedField {
        val fieldName = queryPathToField.segments[pathToFieldIndex]
        val fieldDef = parentType.getFieldDefinition(fieldName)
            ?: error("No definition for ${parentType.name}.$fieldName")

        return ExecutableNormalizedField.newNormalizedField()
            .objectTypeNames(listOf(parentType.name))
            .fieldName(fieldName)
            .deferredExecutions(deferredExecutions)
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
                        deferredExecutions
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
