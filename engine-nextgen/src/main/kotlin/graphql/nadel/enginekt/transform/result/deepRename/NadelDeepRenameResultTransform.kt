package graphql.nadel.enginekt.transform.result.deepRename

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelDeepRenameInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.get
import graphql.nadel.enginekt.transform.result.NadelResultCopyInstruction
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.NadelResultRemoveInstruction
import graphql.nadel.enginekt.transform.result.NadelResultTransform
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class NadelDeepRenameResultTransform : NadelResultTransform {
    override fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField
    ): Boolean {
        return executionBlueprint.fieldInstructions[field] is NadelDeepRenameInstruction
    }

    override fun getInstructions(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(result.data, field.listOfResultKeys.dropLast(1))
        val deepRenameInstruction = executionBlueprint.fieldInstructions[field] as NadelDeepRenameInstruction

        return parentNodes.flatMap { parentNode ->
            @Suppress("UNCHECKED_CAST")
            val parentMap = parentNode.value as JsonMap? ?: return@flatMap emptyList()
            val toCopy = JsonNodeExtractor.getNodesAt(parentMap, deepRenameInstruction.pathToSourceField)
                .emptyOrSingle() ?: return@flatMap emptyList()

            listOf(
                NadelResultCopyInstruction(
                    subjectPath = parentNode.path + toCopy.path,
                    destinationPath = parentNode.path + deepRenameInstruction.location.fieldName,
                ),
                NadelResultRemoveInstruction(
                    subjectPath = parentNode.path + deepRenameInstruction.pathToSourceField.first(),
                ),
            )
        }
    }
}

object RemoveUnusedSelection {
    // TODO
}
