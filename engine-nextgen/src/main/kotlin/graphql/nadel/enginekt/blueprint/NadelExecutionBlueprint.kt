package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.GraphQLObjectTypeName
import graphql.nadel.enginekt.util.filterValuesOfType
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

interface NadelExecutionBlueprint {
    val schema: GraphQLSchema
    val typeInstructions: Map<String, NadelTypeRenameInstruction>
}

/**
 * Execution blueprint where keys are in terms of the overall schema.
 */
data class NadelOverallExecutionBlueprint(
    override val schema: GraphQLSchema,
    override val typeInstructions: Map<String, NadelTypeRenameInstruction>,
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    private val underlyingBlueprints: Map<String, NadelExecutionBlueprint>,
    private val coordinatesToService: Map<FieldCoordinates, Service>,
) : NadelExecutionBlueprint {
    fun getUnderlyingTypeName(overallTypeName: String): String {
        return overallTypeName.let { overall ->
            typeInstructions[overall]?.underlyingName ?: overall
        }
    }

    fun getOverallTypeName(
        service: Service,
        underlyingTypeName: String,
    ): String {
        return getOverallTypeName(service.name, underlyingTypeName)
    }

    private fun getOverallTypeName(
        serviceName: String,
        underlyingTypeName: String,
    ): String {
        val underlyingBlueprint = underlyingBlueprints[serviceName] ?: error("Could not find service: $serviceName")
        return underlyingTypeName.let { underlying ->
            underlyingBlueprint.typeInstructions[underlying]?.overallName ?: underlying
        }
    }

    fun getService(fieldCoordinates: FieldCoordinates): Service? {
        return coordinatesToService[fieldCoordinates]
    }
}

/**
 * Execution blueprint where keys are in terms of the underlying schema.
 */
data class NadelUnderlyingExecutionBlueprint(
    override val schema: GraphQLSchema,
    override val typeInstructions: Map<String, NadelTypeRenameInstruction>,
) : NadelExecutionBlueprint

inline fun <reified T : NadelFieldInstruction> Map<FieldCoordinates, NadelFieldInstruction>.getInstructionsOfTypeForField(
    field: ExecutableNormalizedField,
): Map<GraphQLObjectTypeName, T> {
    return mapFrom(
        field.objectTypeNames
            .mapNotNull { objectTypeName ->
                val coordinates = makeFieldCoordinates(objectTypeName, field.name)
                val instruction = this[coordinates] as? T ?: return@mapNotNull null
                objectTypeName to instruction
            },
    )
}
