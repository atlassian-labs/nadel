package graphql.nadel.engine.transform

import graphql.introspection.Introspection
import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.IntrospectionService
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelNoInterfaceToObjectFragmentExpansionTransform.State
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLTypeUtil.unwrapAll
import graphql.schema.GraphQLUnionType

/**
 * Sends a field the client selected at an interface/union level (an interface field, or `__typename`) to the
 * underlying service **bare** instead of expanding it into a `... on ConcreteType` fragment per exposed member,
 * so a not-yet-deployed sibling type can't be injected into a query the client never named. Hiding is preserved
 * on the result side: an aliased `__typename` is injected, and any node whose concrete type isn't exposed is
 * stripped back to `{}`.
 *
 * Gated per-service by [graphql.nadel.hints.NadelNoInterfaceToObjectFragmentExpansionHint]; inert when off.
 * Registered last so its result-side removals run after type renames. Only plain fields (no
 * [NadelFieldInstruction]) are relaxed.
 */
class NadelNoInterfaceToObjectFragmentExpansionTransform : NadelTransform<State> {
    data class State(
        val aliasHelper: NadelAliasHelper,
        val exposedOverallImplNames: Set<String>,
        val relaxedFieldResultKey: String,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        if (!executionContext.hints.noInterfaceToObjectFragmentExpansion(service)) {
            return null
        }
        if (service.name == IntrospectionService.name) {
            return null
        }
        // Do nothing for defer
        if (overallField.deferredExecutions.isNotEmpty()) {
            return null
        }
        val parent = overallField.parent ?: return null

        val exposedOverallImplNames = computeExposedImplNamesIfRelaxable(executionBlueprint, service, overallField)
            ?: return null

        // Do nothing for fields with instructions (rename/hydration/stub/…)
        val hasFieldInstructions = runCatching {
            executionBlueprint.getTypeNameToInstructionMap<NadelFieldInstruction>(overallField).isNotEmpty()
        }.getOrDefault(true)
        if (hasFieldInstructions) {
            return null
        }

        return State(
            aliasHelper = NadelAliasHelper.forField(tag = "abstract_member", field = parent),
            exposedOverallImplNames = exposedOverallImplNames,
            relaxedFieldResultKey = overallField.resultKey,
        )
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): NadelTransformFieldResult {
        // Keep objectTypeNames honest (only the exposed members). Rather than widen them, flag the field and
        // the injected __typename as forcePrintBare so the forked compiler emits them without a `... on Type`
        // fragment. See [NadelTransformFieldResult.forcePrintBare].
        val typeNameField = newNormalizedField()
            .objectTypeNames(state.exposedOverallImplNames.toList())
            .alias(state.aliasHelper.typeNameResultKey)
            .fieldName(Introspection.TypeNameMetaFieldDef.name)
            .build()

        return NadelTransformFieldResult(
            newField = field,
            artificialFields = listOf(typeNameField),
            forcePrintBare = true,
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
        transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
    ): List<NadelResultInstruction> {
        val parentPath = underlyingParentField?.queryPath ?: overallField.parent?.queryPath ?: return emptyList()
        val parentNodes = nodes.getNodesAt(parentPath, flatten = true)
        val key = state.aliasHelper.typeNameResultKey

        val instructions = mutableListOf<NadelResultInstruction>()
        for (node in parentNodes) {
            @Suppress("UNCHECKED_CAST")
            val nodeMap = node.value as? JsonMap ?: continue
            val underlyingTypeName = nodeMap[key] as String?
            val overallTypeName = underlyingTypeName?.let { executionBlueprint.getOverallTypeName(service, it) }
            val isExposed = overallTypeName != null && overallTypeName in state.exposedOverallImplNames
            if (!isExposed) {
                // Fail closed: hide any node whose concrete type isn't exposed (reduces it to `{}`).
                instructions += NadelResultInstruction.Remove(
                    subject = node,
                    key = NadelResultKey(state.relaxedFieldResultKey),
                )
            }
        }
        return instructions
    }
}

/**
 * The exposed overall implementation names if [overallField] is relaxable, else `null`. Relaxable means: the
 * parent is a single interface/union, the field is selectable at that level (an interface field, or
 * `__typename`), the selection covers exactly the exposed members, and at least one underlying member is
 * hidden. Doesn't consider the hint or field instructions - the caller does.
 */
private fun computeExposedImplNamesIfRelaxable(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: Service,
    overallField: ExecutableNormalizedField,
): Set<String>? {
    val parent = overallField.parent ?: return null

    val engineSchema = executionBlueprint.engineSchema
    val isTypename = overallField.fieldName == TypeNameMetaFieldDef.name

    // getFieldDefinitions can throw while planning a hydration backing query; any failure => not a candidate.
    val parentOutputTypes = runCatching {
        parent.getFieldDefinitions(engineSchema)
            .asSequence()
            .map { unwrapAll(it.type) }
            .toSet()
    }.getOrNull() ?: return null

    // Exposed members, gated on the field being selectable at that level (union => __typename only).
    val exposedOverallImplNames: Set<String>
    val parentAbstractTypeName: String
    when (val parentType = parentOutputTypes.singleOrNull()) {
        is GraphQLInterfaceType -> {
            if (!isTypename && parentType.getFieldDefinition(overallField.fieldName) == null) {
                return null
            }
            exposedOverallImplNames = engineSchema.getImplementations(parentType).map { it.name }.toSet()
            parentAbstractTypeName = parentType.name
        }
        is GraphQLUnionType -> {
            if (!isTypename) {
                return null
            }
            exposedOverallImplNames = parentType.types.map { it.name }.toSet()
            parentAbstractTypeName = parentType.name
        }
        else -> return null
    }

    // Means the client used fragments with explicit objects. don't relax.
    if (overallField.objectTypeNames.toSet() != exposedOverallImplNames) {
        return null
    }

    val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(parentAbstractTypeName)
    val underlyingMemberNames = when (val underlyingType = service.underlyingSchema.getType(underlyingTypeName)) {
        is GraphQLInterfaceType -> service.underlyingSchema.getImplementations(underlyingType).map { it.name }
        is GraphQLUnionType -> underlyingType.types.map { it.name }
        else -> return null
    }
    if (underlyingMemberNames.isEmpty()) {
        return null
    }

    // Nothing to hide unless a member is hidden (and graphql-java already prints bare when none is).
    val hasHiddenImpl = underlyingMemberNames.any { underlyingName ->
        executionBlueprint.getOverallTypeName(service, underlyingName) !in exposedOverallImplNames
    }
    if (!hasHiddenImpl) {
        return null
    }

    return exposedOverallImplNames
}
