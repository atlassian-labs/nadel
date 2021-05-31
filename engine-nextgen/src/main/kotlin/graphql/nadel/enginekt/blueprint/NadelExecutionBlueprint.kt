package graphql.nadel.enginekt.blueprint

import graphql.nadel.Service
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

data class NadelExecutionBlueprint(
    val fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
    val typeInstructions: NadelTypeRenameInstructions,
)

class NadelTypeRenameInstructions(
    private val overallInstruction: Map<String, NadelTypeRenameInstruction>,
    private val serviceInstructions: Map<String, NadelServiceTypeRenameInstructions>,
) {
    fun getForOverall(typeName: String): NadelTypeRenameInstruction? {
        return overallInstruction[typeName]
    }

    fun getForUnderlying(service: Service, typeName: String): NadelTypeRenameInstruction? {
        return serviceInstructions[service.name].let {
            it ?: error("Unable to lookup instructions for service '${service.name}'")
        }[typeName]
    }
}

class NadelServiceTypeRenameInstructions(
    private val instructions: Map<String, NadelTypeRenameInstruction>,
) {
    operator fun get(typeName: String): NadelTypeRenameInstruction? {
        return instructions[typeName]
    }
}

fun <T> Map<FieldCoordinates, T>.getForField(
    key: NormalizedField,
): Map<FieldCoordinates, T> {
    return mapFrom(
        key.objectTypeNames.asSequence()
            .map {
                makeFieldCoordinates(it, key.fieldName)
            }
            .mapNotNull {
                it to (this[it] ?: return@mapNotNull null)
            }
            .toList(),
    )
}
