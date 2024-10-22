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
                    "\$source" -> ObjectField(
                        name = name,
                        pathToField = values,
                    )
                    "\$argument" -> FieldArgument(
                        name = name,
                        argumentName = values.single(),
                    )
                    else -> StaticArgument(
                        name = name,
                        staticValue = astValue,
                    )
                }
            } else {
                StaticArgument(
                    name = name,
                    staticValue = astValue,
                )
            }
        }
    }

    /**
     * Name of the backing field's argument.
     */
    abstract val name: String

    data class ObjectField(
        override val name: String,
        val pathToField: List<String>,
    ) : NadelHydrationArgumentDefinition()

    data class FieldArgument(
        override val name: String,
        val argumentName: String,
    ) : NadelHydrationArgumentDefinition()

    data class StaticArgument(
        override val name: String,
        val staticValue: AnyAstValue,
    ) : NadelHydrationArgumentDefinition()

    internal object Keyword {
        const val name = "name"
        const val value = "value"
    }
}
