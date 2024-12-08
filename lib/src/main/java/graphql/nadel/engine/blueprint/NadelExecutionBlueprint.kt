package graphql.nadel.engine.blueprint

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hints.NadelValidationBlueprintHint
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

/**
 * Execution blueprint where keys are in terms of the overall schema.
 */
interface NadelOverallExecutionBlueprint {
    val engineSchema: GraphQLSchema

    val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>

    fun getUnderlyingTypeNamesForService(service: Service): Set<String>

    fun getOverAllTypeNamesForService(service: Service): Set<String>

    fun getUnderlyingTypeName(
        service: Service,
        overallTypeName: String,
    ): String

    fun getUnderlyingTypeName(overallTypeName: String): String

    fun getRename(overallTypeName: String): NadelTypeRenameInstruction?

    fun getOverallTypeName(
        service: Service,
        underlyingTypeName: String,
    ): String

    fun getServiceOwning(fieldCoordinates: FieldCoordinates): Service?
}

/**
 * Can't put inline function inside interface, so made it an extension function.
 *
 * This is temporary until we delete [NadelOverallExecutionBlueprintMigrator].
 */
inline fun <reified T : NadelFieldInstruction> NadelOverallExecutionBlueprint.getInstructionInsideVirtualType(
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

internal class NadelOverallExecutionBlueprintMigrator(
    private val hint: NadelValidationBlueprintHint,
    val old: NadelOverallExecutionBlueprint,
    val new: Lazy<NadelOverallExecutionBlueprint?>,
) : NadelOverallExecutionBlueprint {
    private val impl: NadelOverallExecutionBlueprint
        get() {
            return if (hint.isNewBlueprintEnabled()) {
                new.value ?: old
            } else {
                old
            }
        }

    override val engineSchema: GraphQLSchema
        get() = impl.engineSchema

    override val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>
        get() = impl.fieldInstructions

    override fun getUnderlyingTypeNamesForService(service: Service): Set<String> {
        return impl.getUnderlyingTypeNamesForService(service)
    }

    override fun getOverAllTypeNamesForService(service: Service): Set<String> {
        return impl.getOverAllTypeNamesForService(service)
    }

    override fun getUnderlyingTypeName(service: Service, overallTypeName: String): String {
        return impl.getUnderlyingTypeName(service, overallTypeName)
    }

    override fun getUnderlyingTypeName(overallTypeName: String): String {
        return impl.getUnderlyingTypeName(overallTypeName)
    }

    override fun getRename(overallTypeName: String): NadelTypeRenameInstruction? {
        return impl.getRename(overallTypeName)
    }

    override fun getOverallTypeName(service: Service, underlyingTypeName: String): String {
        return impl.getOverallTypeName(service, underlyingTypeName)
    }

    override fun getServiceOwning(fieldCoordinates: FieldCoordinates): Service? {
        return impl.getServiceOwning(fieldCoordinates)
    }
}

internal data class NadelOverallExecutionBlueprintImpl(
    override val engineSchema: GraphQLSchema,
    override val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    private val underlyingTypeNamesByService: Map<Service, Set<String>>,
    private val overallTypeNamesByService: Map<Service, Set<String>>,
    private val underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
    private val coordinatesToService: Map<FieldCoordinates, Service>,
    private val typeRenamesByOverallTypeName: Map<String, NadelTypeRenameInstruction>,
) : NadelOverallExecutionBlueprint {
    private fun setOfServiceTypes(
        map: Map<Service, Set<String>>,
        service: Service,
    ): Set<String> {
        return (map[service] ?: error("Could not find service: ${service.name}"))
    }

    override fun getUnderlyingTypeNamesForService(service: Service): Set<String> {
        return setOfServiceTypes(underlyingTypeNamesByService, service)
    }

    override fun getOverAllTypeNamesForService(service: Service): Set<String> {
        return setOfServiceTypes(overallTypeNamesByService, service)
    }

    override fun getUnderlyingTypeName(
        service: Service,
        overallTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return overallTypeName
        }
        return getUnderlyingBlueprint(service).typeInstructions.getUnderlyingName(overallTypeName)
    }

    override fun getUnderlyingTypeName(overallTypeName: String): String {
        return typeRenamesByOverallTypeName[overallTypeName]?.underlyingName ?: overallTypeName
    }

    override fun getRename(overallTypeName: String): NadelTypeRenameInstruction? {
        return typeRenamesByOverallTypeName[overallTypeName]
    }

    override fun getOverallTypeName(
        service: Service,
        underlyingTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return underlyingTypeName
        }
        return getUnderlyingBlueprint(service).typeInstructions.getOverallName(underlyingTypeName)
    }

    override fun getServiceOwning(fieldCoordinates: FieldCoordinates): Service? {
        return coordinatesToService[fieldCoordinates]
    }

    private fun getUnderlyingBlueprint(service: Service): NadelUnderlyingExecutionBlueprint {
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
    fun getUnderlyingName(overallTypeName: String): String {
        return byOverallName[overallTypeName]?.underlyingName ?: overallTypeName
    }

    fun getOverallName(underlyingTypeName: String): String {
        return byUnderlyingName[underlyingTypeName]?.overallName ?: underlyingTypeName
    }

    companion object {
        // Hack for secondary constructor for data classes
        operator fun invoke(
            typeInstructions: List<NadelTypeRenameInstruction>,
        ): NadelTypeRenameInstructions {
            return NadelTypeRenameInstructions(
                byUnderlyingName = typeInstructions.strictAssociateBy { it.underlyingName },
                byOverallName = typeInstructions.strictAssociateBy { it.overallName },
            )
        }
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
