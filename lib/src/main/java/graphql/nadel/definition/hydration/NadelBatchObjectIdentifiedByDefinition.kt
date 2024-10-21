package graphql.nadel.definition.hydration

import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.util.getObjectField

class NadelBatchObjectIdentifiedByDefinition(
    private val objectValue: ObjectValue,
) {
    val sourceId: String
        get() = (objectValue.getObjectField(Keyword.sourceId).value as StringValue).value
    val resultId: String
        get() = (objectValue.getObjectField(Keyword.resultId).value as StringValue).value

    internal object Keyword {
        const val sourceId = "sourceId"
        const val resultId = "resultId"
    }
}
