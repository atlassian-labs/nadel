package graphql.nadel.engine.blueprint

import graphql.nadel.Service
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.engine.util.strictAssociateBy
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates

/**
 * Execution blueprints store prerequisite information for executing a service.
 * This includes information like renames or hydrations etc. on specific fields.
 * It includes further information like what types a service defines and its underlying
 * schema.
 *
 * Defined as an interface to easily create a no-op placeholder.
 */
data class NadelServiceBlueprint internal constructor(
    val service: Service,
    val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    val typeRenames: NadelTypeRenameInstructions,
    val overallTypesDefined: Set<String>,
    val underlyingTypesDefined: Set<String>,
)

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
                // todo: one day this should be turned back into strictAssociateBy
                // The issue is that we have a grandfathered JSW schema that abuses this, but thankfully only for input types
                // This is also enforced by validation these days, so we shouldn't actually get dupes
                byUnderlyingName = typeInstructions.associateBy { it.underlyingName },
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
