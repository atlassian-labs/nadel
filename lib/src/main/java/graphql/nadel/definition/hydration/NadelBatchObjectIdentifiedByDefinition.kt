package graphql.nadel.definition.hydration

import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.engine.util.parseDefinition
import graphql.nadel.util.getObjectField

class NadelBatchObjectIdentifiedByDefinition(
    private val objectValue: ObjectValue,
) {
    companion object {
        val inputValueDefinition = parseDefinition<InputObjectTypeDefinition>(
            // language=GraphQL
            """
                "This is required by batch hydration to understand how to pull out objects from the batched result"
                input NadelBatchObjectIdentifiedBy {
                    sourceId: String!
                    resultId: String!
                }
            """.trimIndent(),
        )
    }

    val sourceId: String
        get() = (objectValue.getObjectField(Keyword.sourceId).value as StringValue).value
    val resultId: String
        get() = (objectValue.getObjectField(Keyword.resultId).value as StringValue).value

    internal object Keyword {
        const val sourceId = "sourceId"
        const val resultId = "resultId"
    }
}
