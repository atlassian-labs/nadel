package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.util.filterValuesOfType
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

interface NadelExecutionBlueprint {
    val schema: GraphQLSchema
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>
    val typeInstructions: Map<String, NadelTypeRenameInstruction>
}

/**
 * Execution blueprint where keys are in terms of the overall schema.
 */
data class NadelOverallExecutionBlueprint(
    override val schema: GraphQLSchema,
    override val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    override val typeInstructions: Map<String, NadelTypeRenameInstruction>,
    private val underlyingBlueprints: Map<String, NadelExecutionBlueprint>,
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
        val serviceName = service.name
        val underlyingBlueprint = underlyingBlueprints[serviceName] ?: error("Could not find service: $serviceName")
        return underlyingTypeName.let { underlying ->
            underlyingBlueprint.typeInstructions[underlying]?.overallName ?: underlying
        }
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
}

/**
 * Execution blueprint where keys are in terms of the underlying schema.
 */
data class NadelUnderlyingExecutionBlueprint(
    override val schema: GraphQLSchema,
    override val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    override val typeInstructions: Map<String, NadelTypeRenameInstruction>,
) : NadelExecutionBlueprint

fun <T> Map<FieldCoordinates, T>.getForField(
    field: NormalizedField,
): Map<FieldCoordinates, T> {
    return mapFrom(
        field.objectTypeNames.asSequence()
            .map {
                makeFieldCoordinates(it, field.fieldName)
            }
            .mapNotNull {
                it to (this[it] ?: return@mapNotNull null)
            }
            .toList(),
    )
}

inline fun <reified T : NadelFieldInstruction> Map<FieldCoordinates, NadelFieldInstruction>.getInstructionsOfTypeForField(
    field: NormalizedField,
): Map<FieldCoordinates, T> {
    return getForField(field).filterValuesOfType()
}
