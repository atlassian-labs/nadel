package graphql.nadel.definition.hydration

import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.engine.util.parseDefinition
import graphql.nadel.util.AnyAstValue
import graphql.nadel.util.getObjectField

/**
 * Argument belonging to [NadelHydrationDefinition.arguments]
 */
sealed class NadelHydrationArgumentDefinition {
    companion object {
        val inputValueDefinition = parseDefinition<InputObjectTypeDefinition>(
            // language=GraphQL
            """
                "This allows you to hydrate new values into fields"
                input NadelHydrationArgument {
                    name: String!
                    value: JSON!
                }
            """.trimIndent(),
        )

        fun from(argumentObject: ObjectValue): NadelHydrationArgumentDefinition {
            val name = (argumentObject.getObjectField(Keyword.name).value as StringValue).value
            val astValue = argumentObject.getObjectField(Keyword.value).value

            return if (astValue is StringValue && astValue.value.startsWith("$")) {
                val command = astValue.value.substringBefore(".")
                val values = astValue.value.substringAfter(".").split('.')

                when (command) {
                    "\$source" -> SourceField(
                        backingFieldArgumentName = name,
                        pathToSourceField = values,
                    )
                    "\$argument" -> VirtualFieldArgument(
                        backingFieldArgumentName = name,
                        virtualFieldArgumentName = values.single(),
                    )
                    else -> StaticArgument(
                        backingFieldArgumentName = name,
                        staticValue = astValue,
                    )
                }
            } else {
                StaticArgument(
                    backingFieldArgumentName = name,
                    staticValue = astValue,
                )
            }
        }
    }

    abstract val backingFieldArgumentName: String

    data class SourceField(
        override val backingFieldArgumentName: String,
        val pathToSourceField: List<String>,
    ) : NadelHydrationArgumentDefinition()

    data class VirtualFieldArgument(
        override val backingFieldArgumentName: String,
        val virtualFieldArgumentName: String,
    ) : NadelHydrationArgumentDefinition()

    data class StaticArgument(
        override val backingFieldArgumentName: String,
        val staticValue: AnyAstValue,
    ) : NadelHydrationArgumentDefinition()

    internal object Keyword {
        const val name = "name"
        const val value = "value"
    }
}
