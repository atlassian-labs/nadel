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
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
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
        val hasFieldInstructions = overallField.objectTypeNames.any { objectTypeName ->
            executionBlueprint.fieldInstructions.get(objectTypeName, overallField.name)?.isNotEmpty() == true
        }
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
 * parent's output is one or more interfaces (or a single union), the field is selectable at that level (an
 * interface field present on every parent interface, or `__typename`), the selection covers exactly the exposed
 * members, and at least one underlying member is hidden. Doesn't consider the hint or field instructions - the
 * caller does.
 */
private fun computeExposedImplNamesIfRelaxable(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: Service,
    overallField: ExecutableNormalizedField,
): Set<String>? {
    val parent = overallField.parent ?: return null
    val engineSchema = executionBlueprint.engineSchema
    val isTypename = overallField.fieldName == TypeNameMetaFieldDef.name

    val parentOutputTypes = parent.objectTypeNames.map { parentTypeName ->
        val parentType = engineSchema.getType(parentTypeName) as? GraphQLFieldsContainer ?: return null
        val parentFieldDef = parentType.getFieldDefinition(parent.fieldName) ?: return null
        unwrapAll(parentFieldDef.type)
    }.toSet()

    val parentAbstractTypes: List<GraphQLNamedType> =
        if (parentOutputTypes.isNotEmpty() && parentOutputTypes.all { it is GraphQLInterfaceType }) {
            val interfaces = parentOutputTypes.filterIsInstance<GraphQLInterfaceType>()
            val fieldIsOnEveryInterface =
                isTypename || interfaces.all { it.getFieldDefinition(overallField.fieldName) != null }
            if (!fieldIsOnEveryInterface) {
                return null
            }
            interfaces
        } else {
            val union = parentOutputTypes.singleOrNull() as? GraphQLUnionType ?: return null
            if (!isTypename) {
                return null
            }
            listOf(union)
        }

    val exposedOverallImplNames =
        parentAbstractTypes.flatMap { abstractMemberNames(engineSchema, it) ?: return null }.toSet()
    // Means the client used fragments with explicit objects. don't relax.
    if (overallField.objectTypeNames.toSet() != exposedOverallImplNames) {
        return null
    }

    val underlyingMemberNames = parentAbstractTypes.flatMap { abstractType ->
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(abstractType.name)
        val underlyingType = service.underlyingSchema.getType(underlyingTypeName)
        abstractMemberNames(service.underlyingSchema, underlyingType) ?: return null
    }
    // Relax only if some underlying member is hidden from the overall (graphql-java already prints bare when none is).
    val hasHiddenMember = underlyingMemberNames.any { underlyingName ->
        executionBlueprint.getOverallTypeName(service, underlyingName) !in exposedOverallImplNames
    }
    if (!hasHiddenMember) {
        return null
    }

    return exposedOverallImplNames
}

/** The implementation/member type names of an interface or union in [schema], or `null` if [type] is neither. */
private fun abstractMemberNames(schema: GraphQLSchema, type: GraphQLType?): List<String>? =
    when (type) {
        is GraphQLInterfaceType -> schema.getImplementations(type).map { it.name }
        is GraphQLUnionType -> type.types.map { it.name }
        else -> null
    }
