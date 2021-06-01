package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.normalized.NormalizedField

object NadelHydrationQueryBuilder {
    fun getQuery(
        instruction: NadelHydrationFieldInstruction,
        hydrationField: NormalizedField,
        parentNode: JsonNode,
        pathToResultKeys: (List<String>) -> List<String>,
    ): NormalizedField {
        return NadelPathToField.createField(
            schema = instruction.sourceService.underlyingSchema,
            parentType = instruction.sourceService.underlyingSchema.queryType,
            pathToField = instruction.pathToSourceField,
            fieldArguments = NadelHydrationArgumentsBuilder.createSourceFieldArgs(
                instruction,
                parentNode,
                hydrationField,
                pathToResultKeys,
            ),
            fieldChildren = hydrationField.children,
        )
    }
}
