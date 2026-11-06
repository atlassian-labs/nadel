package graphql.nadel.engine.transform.query

import graphql.introspection.Introspection
import graphql.language.OperationDefinition.Operation
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.blueprint.NadelIntrospectionRunnerFactory
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelPartitionInstruction
import graphql.nadel.engine.util.copyWithChildren
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema

internal class NadelFieldToService(
    querySchema: GraphQLSchema,
    private val overallExecutionBlueprint: NadelOverallExecutionBlueprint,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
    private val dynamicServiceResolution: DynamicServiceResolution,
) {
    private val introspectionService = IntrospectionService(querySchema, introspectionRunnerFactory)

    fun getServicesForTopLevelFields(
        executionContext: NadelExecutionContext,
    ): List<NadelFieldAndService> {
        val query = executionContext.query
        val executionHints = executionContext.hints
        val batchRoots = executionHints.batchRootFields()
        val batchNamespaces = query.operation == Operation.QUERY && executionHints.batchNamespacedRootFields()

        if (!batchRoots && !batchNamespaces) {
            return query.topLevelFields.flatMap { topLevelField ->
                if (isNamespacedField(topLevelField)) {
                    getServicePairsForNamespacedFields(topLevelField)
                } else {
                    listOf(NadelFieldAndService(fields = listOf(topLevelField), service = getService(topLevelField)))
                }
            }
        }

        val result = mutableListOf<NadelFieldAndService>()
        val batchedByGroup = LinkedHashMap<NadelBatchGroup, MutableList<ExecutableNormalizedField>>()
        val namespacesByGroup = LinkedHashMap<NadelBatchGroup, MutableList<ExecutableNormalizedField>>()
        for (topLevelField in query.topLevelFields) {
            if (isNamespacedField(topLevelField)) {
                for (entry in getServicePairsForNamespacedFields(topLevelField)) {
                    val group = if (batchNamespaces) {
                        getNamespaceBatchGroup(executionContext, entry)
                    } else {
                        null
                    }

                    if (group == null) {
                        result += entry
                    } else {
                        namespacesByGroup.getOrPut(group) { mutableListOf() }.addAll(entry.fields)
                    }
                }
                continue
            }

            val service = getService(topLevelField)
            if (batchRoots && canBatchRootField(topLevelField, service, executionHints)) {
                val shardingTarget = executionContext.hooks.getShardingTarget(executionContext, service, topLevelField)
                batchedByGroup.getOrPut(NadelBatchGroup(service, shardingTarget)) { mutableListOf() }.add(topLevelField)
            } else {
                result += NadelFieldAndService(fields = listOf(topLevelField), service = service)
            }
        }

        for ((group, namespaces) in namespacesByGroup) {
            val roots = batchedByGroup[group]
            if (roots != null && roots.none(::hasPartitionInstruction)) {
                roots.addAll(namespaces)
            } else {
                result += NadelFieldAndService(fields = namespaces, service = group.service)
            }
        }

        batchedByGroup.forEach { (group, batchedFields) ->
            result += NadelFieldAndService(fields = batchedFields, service = group.service)
        }

        return result
    }

    private fun getNamespaceBatchGroup(
        executionContext: NadelExecutionContext,
        entry: NadelFieldAndService,
    ): NadelBatchGroup? {
        if (!executionContext.hints.batchNamespacedRootFields(entry.service)) {
            return null
        }

        val namespace = entry.fields.single()
        if (!canBatchNamespaceField(namespace) ||
            namespace.children.isEmpty() ||
            namespace.children.any { !canBatchNamespaceField(it) }
        ) {
            return null
        }

        val targets = namespace.children.map { child ->
            executionContext.hooks.getShardingTarget(executionContext, entry.service, child)
        }.distinct()

        if (targets.size != 1) {
            return null
        }

        return NadelBatchGroup(entry.service, targets.single())
    }

    private fun canBatchNamespaceField(field: ExecutableNormalizedField): Boolean {
        return !field.name.startsWith("__") &&
            !hasPartitionInstruction(field) &&
            !dynamicServiceResolution.needsDynamicServiceResolution(field)
    }

    private fun hasPartitionInstruction(field: ExecutableNormalizedField): Boolean {
        return overallExecutionBlueprint
            .getTypeNameToInstructionMap<NadelPartitionInstruction>(field)
            .isNotEmpty()
    }

    /**
     * Root fields are only batched into a single service call when the service opts in via
     * [NadelExecutionHints.batchRootFields].
     */
    private fun canBatchRootField(
        field: ExecutableNormalizedField,
        service: Service,
        executionHints: NadelExecutionHints,
    ): Boolean {
        if (!executionHints.batchRootFields(service)) {
            return false
        }
        if (field.name.startsWith("__")) {
            return false
        }
        return !dynamicServiceResolution.needsDynamicServiceResolution(field)
    }

    /**
     * Returns the dynamically resolved service for the field, if it is annotated with @dynamicServiceResolution,
     * otherwise returns the originalService.
     */
    fun resolveDynamicService(
        fields: List<ExecutableNormalizedField>,
        originalService: Service,
    ): Service {
        val field = fields.singleOrNull() ?: return originalService
        return if (dynamicServiceResolution.needsDynamicServiceResolution(field)) {
            dynamicServiceResolution.resolveServiceForField(field)
        } else {
            originalService
        }
    }

    private fun getServicePairsForNamespacedFields(
        topLevelField: ExecutableNormalizedField,
    ): List<NadelFieldAndService> {
        return topLevelField.children
            .groupBy { childField ->
                getServiceForNamespacedField(childField)
            }
            .map { (service, childTopLevelFields) ->
                NadelFieldAndService(
                    fields = listOf(topLevelField.copyWithChildren(childTopLevelFields)),
                    service = service,
                )
            }
    }

    private fun getServiceForNamespacedField(
        overallField: ExecutableNormalizedField,
    ): Service {
        if (overallField.name == Introspection.TypeNameMetaFieldDef.name) {
            return introspectionService
        }

        return getService(overallField)
    }

    private fun getService(overallField: ExecutableNormalizedField): Service {
        if (overallField.name.startsWith("__")) {
            return introspectionService
        }

        val operationTypeName = overallField.objectTypeNames.single()
        val fieldCoordinates = makeFieldCoordinates(operationTypeName, overallField.name)
        return overallExecutionBlueprint.getServiceOwning(fieldCoordinates)
            ?: error("Unable to find service for field at: $fieldCoordinates")
    }

    private fun isNamespacedField(field: ExecutableNormalizedField): Boolean {
        return isNamespacedField(field, overallExecutionBlueprint.engineSchema)
    }
}

data class NadelFieldAndService(
    val fields: List<ExecutableNormalizedField>,
    val service: Service,
)

/**
 * Grouping key for root-field batching: root fields are only batched together when they share the
 * same [service] and the same [shardingTarget]. [shardingTarget] is an opaque value supplied by
 * [graphql.nadel.hooks.NadelExecutionHooks.getShardingTarget] (`null` when the field is not bound to
 * a specific shard).
 */
private data class NadelBatchGroup(
    val service: Service,
    val shardingTarget: Any?,
)
