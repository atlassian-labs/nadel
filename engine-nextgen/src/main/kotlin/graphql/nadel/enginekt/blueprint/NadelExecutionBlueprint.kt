package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.GraphQLObjectTypeName
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.mapFrom
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

/**
 * Execution blueprint where keys are in terms of the overall schema.
 */
data class NadelOverallExecutionBlueprint(
    val schema: GraphQLSchema,
    val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    private val underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
    private val coordinatesToService: Map<FieldCoordinates, Service>,
) {
    fun getUnderlyingTypeName(
        service: Service,
        overallTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return overallTypeName
        }

        return getUnderlyingBlueprint(service).typeInstructions.getUnderlyingName(overallTypeName)
    }

    fun getOverallTypeName(
        service: Service,
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
