package graphql.nadel.engine.blueprint

import graphql.nadel.NadelExecutableService
import graphql.nadel.NadelFieldMap
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceLike
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.engine.util.strictAssociateBy
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

data class NadelExecutionBlueprint(
    val engineSchema: GraphQLSchema,
    val services: List<NadelExecutableService>,
    val fieldOwnershipMap: NadelFieldMap<NadelExecutableService>,
)

/**
 * Execution blueprint where keys are in terms of the overall schema.
 */
@Deprecated("Use NadelExecutionBlueprint instead")
data class NadelOverallExecutionBlueprint(
    val engineSchema: GraphQLSchema,
    val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    private val underlyingTypeNamesByService: Map<String, Set<String>>,
    private val overallTypeNamesByService: Map<String, Set<String>>,
    private val underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
    private val coordinatesToService: Map<FieldCoordinates, Service>,
    private val typeRenamesByOverallTypeName: Map<String, NadelTypeRenameInstruction>,
) {
    private fun setOfServiceTypes(
        map: Map<String, Set<String>>,
        service: ServiceLike,
    ): Set<String> {
        return (map[service.name] ?: error("Could not find service: ${service.name}"))
    }

    fun getUnderlyingTypeNamesForService(service: ServiceLike): Set<String> {
        return setOfServiceTypes(underlyingTypeNamesByService, service)
    }

    fun getOverAllTypeNamesForService(service: ServiceLike): Set<String> {
        return setOfServiceTypes(overallTypeNamesByService, service)
    }

    fun getUnderlyingTypeName(
        service: ServiceLike,
        overallTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return overallTypeName
        }

        return getUnderlyingBlueprint(service).typeInstructions.getUnderlyingName(overallTypeName)
    }

    fun getUnderlyingTypeName(overallTypeName: String): String {
        return typeRenamesByOverallTypeName[overallTypeName]?.underlyingName ?: overallTypeName
    }

    fun getRename(overallTypeName: String): NadelTypeRenameInstruction? {
        return typeRenamesByOverallTypeName[overallTypeName]
    }

    fun getOverallTypeName(
        service: ServiceLike,
        underlyingTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return underlyingTypeName
        }
        return getUnderlyingBlueprint(service).typeInstructions.getOverallName(underlyingTypeName)
    }

    fun getServiceOwning(fieldCoordinates: FieldCoordinates): Service? {
        return coordinatesToService[fieldCoordinates]
    }

    inline fun <reified T : NadelFieldInstruction> getInstructionInsideVirtualType(
        hydrationDetails: ServiceExecutionHydrationDetails?,
        backingField: ExecutableNormalizedField,
    ): Map<GraphQLObjectTypeName, List<T>> {
        hydrationDetails ?: return emptyMap() // Need hydration to provide virtual hydration context

        val backingFieldParentTypeName = backingField.objectTypeNames.singleOrNull()
            ?: return emptyMap() // Don't support abstract types for now

        val nadelHydrationContext = fieldInstructions[hydrationDetails.hydrationVirtualField]!!
            .asSequence()
            .filterIsInstance<NadelGenericHydrationInstruction>()
            .first() as? NadelHydrationFieldInstruction
            ?: return emptyMap() // Virtual types only come about from standard hydrations, not batched

        val virtualTypeContext = nadelHydrationContext.virtualTypeContext
            ?: return emptyMap() // Not all hydrations create virtual types

        val virtualType = virtualTypeContext.backingTypeToVirtualType[backingFieldParentTypeName]
            ?: return emptyMap() // Not a virtual type

        val fieldCoordinatesInVirtualType = makeFieldCoordinates(virtualType, backingField.name)

        val instructions = fieldInstructions[fieldCoordinatesInVirtualType]
            ?.filterIsInstance<T>()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?: return emptyMap()

        return mapOf(
            backingField.objectTypeNames.single() to instructions,
        )
    }

    private fun getUnderlyingBlueprint(service: ServiceLike): NadelUnderlyingExecutionBlueprint {
        val name = service.name
        return underlyingBlueprints[name] ?: error("Could not find service: $name")
    }
}

data class NadelUnderlyingExecutionBlueprint internal constructor(
    val service: Service,
    val schema: GraphQLSchema,
    val typeInstructions: NadelTypeRenameInstructions,
) {
    companion object {
        // Hack for secondary constructor for data classes
        operator fun invoke(
            service: Service,
            schema: GraphQLSchema,
            typeInstructions: List<NadelTypeRenameInstruction>,
        ): NadelUnderlyingExecutionBlueprint {
            return NadelUnderlyingExecutionBlueprint(
                service = service,
                schema = schema,
                typeInstructions = NadelTypeRenameInstructions(typeInstructions),
            )
        }
    }
}

data class NadelTypeRenameInstructions internal constructor(
    private val byUnderlyingName: Map<String, NadelTypeRenameInstruction>,
    private val byOverallName: Map<String, NadelTypeRenameInstruction>,
) {
    constructor(
        typeInstructions: List<NadelTypeRenameInstruction>,
    ) : this(
        byUnderlyingName = typeInstructions.strictAssociateBy { it.underlyingName },
        byOverallName = typeInstructions.strictAssociateBy { it.overallName },
    )

    fun getUnderlyingName(overallTypeName: String): String {
        return byOverallName[overallTypeName]?.underlyingName ?: overallTypeName
    }

    fun getOverallName(underlyingTypeName: String): String {
        return byUnderlyingName[underlyingTypeName]?.overallName ?: underlyingTypeName
    }

    companion object {
        internal val Empty = NadelTypeRenameInstructions(emptyMap(), emptyMap())
    }
}

/**
 * todo: why doesn't this belong inside [NadelOverallExecutionBlueprint]
 */
inline fun <reified T : NadelFieldInstruction> Map<FieldCoordinates, List<NadelFieldInstruction>>.getTypeNameToInstructionMap(
    field: ExecutableNormalizedField,
): Map<GraphQLObjectTypeName, T> {
    return mapFrom(
        field.objectTypeNames
            .mapNotNull { objectTypeName ->
                val coordinates = makeFieldCoordinates(objectTypeName, field.name)
                val instruction = this[coordinates]
                    ?.filterIsInstance<T>()
                    ?.emptyOrSingle() ?: return@mapNotNull null
                objectTypeName to instruction
            },
    )
}

/**
 * todo: why doesn't this belong inside [NadelOverallExecutionBlueprint]
 */
inline fun <reified T : NadelFieldInstruction> Map<FieldCoordinates, List<NadelFieldInstruction>>.getTypeNameToInstructionsMap(
    field: ExecutableNormalizedField,
): Map<GraphQLObjectTypeName, List<T>> {
    return mapFrom(
        field.objectTypeNames
            .mapNotNull { objectTypeName ->
                val coordinates = makeFieldCoordinates(objectTypeName, field.name)
                val instructions = (this[coordinates] ?: return@mapNotNull null)
                    .filterIsInstance<T>()
                when {
                    instructions.isEmpty() -> return@mapNotNull null
                    else -> objectTypeName to instructions
                }
            },
    )
}
