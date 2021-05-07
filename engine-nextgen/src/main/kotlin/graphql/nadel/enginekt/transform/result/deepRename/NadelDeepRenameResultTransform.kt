package graphql.nadel.enginekt.transform.result.deepRename

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getForField
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.NadelResultTransform
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.util.RemoveUnusedField
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
        return executionBlueprint.fieldInstructions.getForField(field) is NadelDeepRenameFieldInstruction
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
        val deepRenameInstruction = executionBlueprint.fieldInstructions.getForField(field)
            as NadelDeepRenameFieldInstruction

        return parentNodes.flatMap { parentNode ->
            @Suppress("UNCHECKED_CAST") // Ensure the result is a Map, return if null
            parentNode.value as JsonMap? ?: return@flatMap emptyList()

            val toCopy = JsonNodeExtractor.getNodesAt(parentNode, deepRenameInstruction.pathToSourceField)
                .emptyOrSingle() ?: return@flatMap emptyList()

            listOf(
                NadelResultInstruction.Copy(
                    subjectPath = toCopy.path,
                    destinationPath = parentNode.path + field.resultKey,
                ),
            ) + RemoveUnusedField.getInstructions(
                node = parentNode,
                originalSelection = field,
                pathToField = deepRenameInstruction.pathToSourceField,
            )
        }
    }
}
