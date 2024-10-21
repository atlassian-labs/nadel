package graphql.nadel.definition.hydration

import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.util.AnyAstValue
import graphql.nadel.util.getObjectField

/**
 * Argument belonging to [NadelHydrationDefinition.arguments]
 */
class NadelHydrationArgumentDefinition(
    private val argumentObject: ObjectValue,
) {
    /**
     * Name of the backing field's argument.
     */
    val name: String
        get() = (argumentObject.getObjectField(Keyword.name).value as StringValue).value

    /**
     * Value to support to the backing field's argument at runtime.
     */
    val value: ValueSource
        get() = ValueSource.from(argumentObject.getObjectField(Keyword.value).value)

    internal object Keyword {
        const val name = "name"
        const val value = "value"
    }

    sealed class ValueSource {
        data class ObjectField(
            val pathToField: List<String>,
        ) : ValueSource()

        data class FieldArgument(
            val argumentName: String,
        ) : ValueSource()

        data class StaticArgument(
            val staticValue: AnyAstValue,
        ) : ValueSource()

        companion object {
            fun from(astValue: AnyAstValue): ValueSource {
                return if (astValue is StringValue && astValue.value.startsWith("$")) {
                    val command = astValue.value.substringBefore(".")
                    val values = astValue.value.substringAfter(".").split('.')

                    when (command) {
                        "\$source" -> ObjectField(
                            pathToField = values,
                        )
                        "\$argument" -> FieldArgument(
                            argumentName = values.single(),
                        )
                        else -> StaticArgument(staticValue = astValue)
                    }
                } else {
                    StaticArgument(staticValue = astValue)
                }
            }
        }
    }
}
