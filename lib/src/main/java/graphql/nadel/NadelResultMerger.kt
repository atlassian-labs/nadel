package graphql.nadel

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.engine.transform.query.NadelFieldAndService
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.asJsonMap
import graphql.nadel.engine.util.asMutableJsonMap
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.util.ErrorUtil
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema

@JvmInline
private value class ResultKey(val value: String)

internal class NadelResultMerger {
    fun mergeResults(
        fields: List<NadelFieldAndService>,
        engineSchema: GraphQLSchema,
        results: List<ServiceExecutionResult>,
    ): ExecutionResult {
        val data: MutableJsonMap = mutableMapOf()
        val extensions: MutableJsonMap = mutableMapOf()
        val errors: MutableList<GraphQLError> = mutableListOf()

        for (result in results) {
            putAndMergeTopLevelData(data, oneData = result.data)
            errors.addAll(ErrorUtil.createGraphQLErrorsFromRawErrors(result.errors))
            extensions.putAll(result.extensions)
        }

        return ExecutionResultImpl.newExecutionResult()
            .data(fixData(fields, engineSchema, data))
            .extensions(extensions.let {
                @Suppress("UNCHECKED_CAST") // .extensions should take in a Map<*, *> instead of strictly Map<Any?, Any?>
                it as Map<Any?, Any?>
            }.takeIf {
                it.isNotEmpty()
            })
            .errors(errors)
            .build()
    }

    private fun fixData(
        fields: List<NadelFieldAndService>,
        engineSchema: GraphQLSchema,
        data: MutableJsonMap,
    ): MutableJsonMap? {
        val requiredFieldMap = buildRequiredFieldMap(fields, engineSchema)

        for ((topLevelResultKey, children) in requiredFieldMap) {
            val topLevelFieldDef by lazy {
                fields
                    .first { (field) -> field.resultKey == topLevelResultKey.value }
                    .field
                    .getFieldDefinitions(engineSchema)
                    .single() // This is under Query, Mutation etc. so there is only one field
            }

            // Ensure field is present in result
            if (topLevelResultKey.value !in data) {
                data[topLevelResultKey.value] = null
            }

            if (children.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                val childrenData = data[topLevelResultKey.value] as? MutableJsonMap?

                if (childrenData != null) {
                    val queryHasNonTypenameFields = children.any { it.name != Introspection.TypeNameMetaFieldDef.name }

                    // This portion mutates the result if the __typename is the only field successfully resolved
                    // This is to simulate the __typename being resolved by an underlying service
                    if (queryHasNonTypenameFields) {
                        val dataOnlyHasTypenameFields = childrenData
                            .keys
                            .all { key ->
                                children
                                    .find { it.resultKey == key }
                                    ?.name == Introspection.TypeNameMetaFieldDef.name
                            }

                        if (dataOnlyHasTypenameFields) {
                            data[topLevelResultKey.value] = null
                        }
                    }

                    // This portion ensures that all namespaced fields are present in the object
                    for (child in children) {
                        if (child.resultKey !in childrenData) {
                            childrenData[child.resultKey] = null
                        }

                        // Handle non-null case
                        val childFieldDef = child.getFieldDefinitions(engineSchema).single()
                        if (childFieldDef.type.isNonNull && childrenData[child.resultKey] == null) {
                            data[topLevelResultKey.value] = null
                        }
                    }
                }
            }

            // Handle non-null case
            if (topLevelFieldDef.type.isNonNull && data[topLevelResultKey.value] == null) {
                return null
            }
        }

        return data
    }

    private fun buildRequiredFieldMap(
        fields: List<NadelFieldAndService>,
        engineSchema: GraphQLSchema,
    ): MutableMap<ResultKey, MutableList<ExecutableNormalizedField>> {
        val requiredFields = mutableMapOf<ResultKey, MutableList<ExecutableNormalizedField>>()

        // NOTE: please ensure all fields are from object types and will NOT have multiple field defs
        // Other code in this file relies on this contract
        for ((field) in fields) {
            val requiredChildFields = requiredFields
                .computeIfAbsent(ResultKey(field.resultKey)) {
                    mutableListOf()
                }

            if (isNamespacedField(field, engineSchema)) {
                requiredChildFields.addAll(field.children)
            }
        }

        return requiredFields
    }

    private fun putAndMergeTopLevelData(data: MutableJsonMap, oneData: JsonMap) {
        for ((topLevelFieldName: String, newTopLevelFieldValue: Any?) in oneData) {
            if (topLevelFieldName in data) {
                val existingValue = data[topLevelFieldName]
                if (existingValue == null) {
                    data[topLevelFieldName] = newTopLevelFieldValue
                } else if (existingValue is AnyMap && newTopLevelFieldValue is AnyMap) {
                    existingValue.asMutableJsonMap().putAll(
                        newTopLevelFieldValue.asJsonMap(),
                    )
                }
            } else {
                data[topLevelFieldName] = newTopLevelFieldValue
            }
        }
    }
}
