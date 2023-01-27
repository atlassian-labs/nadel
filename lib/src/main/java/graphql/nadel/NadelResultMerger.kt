package graphql.nadel

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQLError
import graphql.introspection.Introspection
import graphql.nadel.engine.transform.query.NadelFieldAndService
import graphql.nadel.engine.transform.query.NadelNamespacedFields.isNamespacedField
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.asJsonMap
import graphql.nadel.engine.util.asMutableJsonMap
import graphql.nadel.util.ErrorUtil
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema

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
    ): MutableJsonMap {
        val requiredFieldMap = buildRequiredFieldMap(fields, engineSchema)

        for ((resultKey, children) in requiredFieldMap) {
            if (resultKey !in data) {
                data[resultKey] = null
            }

            if (children.isNotEmpty()) {
                val childrenData = data[resultKey] as? MutableJsonMap?

                if (childrenData != null) {
                    val hasNonTypenameFields = children.any { it.name != Introspection.TypeNameMetaFieldDef.name }

                    // This portion mutates the result if the __typename is the only field successfully resolved
                    // This is to simulate the __typename being resolved by an underlying service
                    if (hasNonTypenameFields) {
                        val dataOnlyHasTypenameFields = childrenData
                            .keys
                            .all { key ->
                                children
                                    .find { it.resultKey == key }
                                    ?.name == Introspection.TypeNameMetaFieldDef.name
                            }

                        if (dataOnlyHasTypenameFields) {
                            data[resultKey] = null
                        }
                    }

                    // This portion ensures that all namespaced fields are present in the object
                    for (child in children) {
                        if (child.resultKey !in childrenData) {
                            childrenData[child.resultKey] = null
                        }
                    }
                }
            }
        }

        return data
    }

    private fun buildRequiredFieldMap(
        fields: List<NadelFieldAndService>,
        engineSchema: GraphQLSchema,
    ): MutableMap<String, MutableList<ExecutableNormalizedField>> {
        // key is resultKey I think??
        val requiredFields = mutableMapOf<String, MutableList<ExecutableNormalizedField>>()

        for ((field) in fields) {
            val requiredChildFields = requiredFields
                .computeIfAbsent(field.resultKey) {
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
