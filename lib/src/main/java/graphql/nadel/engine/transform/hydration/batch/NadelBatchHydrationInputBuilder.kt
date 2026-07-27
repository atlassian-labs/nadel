package graphql.nadel.engine.transform.hydration.batch

import graphql.language.NullValue
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.mapFrom
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

/**
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelBatchHydrationInputBuilder {
    internal fun getNonBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        virtualField: ExecutableNormalizedField,
    ): Map<NadelHydrationArgument, NormalizedInputValue> {
        return mapFrom(
            instruction.backingFieldArguments.mapNotNull { argument ->
                when (val valueSource = argument.valueSource) {
                    is NadelHydrationArgument.ValueSource.ArgumentValue -> {
                        val argValue: NormalizedInputValue? =
                            virtualField.normalizedArguments[valueSource.argumentName]
                                ?: valueSource.defaultValue
                        if (argValue != null) {
                            argument to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationArgument.ValueSource.FieldResultValue -> null
                    is NadelHydrationArgument.ValueSource.StaticValue -> {
                        val staticValue: NormalizedInputValue = makeNormalizedInputValue(
                            type = argument.backingArgumentDef.type,
                            value = valueSource.value,
                        )
                        argument to staticValue
                    }
                    is NadelHydrationArgument.ValueSource.RemainingArguments -> {
                        argument to NormalizedInputValue(
                            /* typeName = */ GraphQLTypeUtil.simplePrint(argument.backingArgumentDef.type),
                            /* value = */
                            valueSource.remainingArgumentNames
                                .associateWith {
                                    virtualField.normalizedArguments[it]?.value ?: NullValue.of()
                                },
                        )
                    }
                }
            },
        )
    }

    /**
     * TODO: this should really be baked into the [instruction] and also be mandatory…
     *
     * Get the input def that is collated together to form the batch input.
     *
     * e.g. for a schema
     *
     * ```graphql
     * type User {
     *   friendId: [ID]
     *   friend(acquaintances: Boolean! = false): User @hydrated(
     *     from: "usersByIds",
     *     arguments: [
     *       {name: "userIds", valueFromField: "friendId"}
     *       {name: "acquaintances", valueFromArgument: "acquaintances"}
     *     ],
     *   )
     * }
     * ```
     *
     * then the input def would be the `userIds`.
     */
    internal fun getBatchInputDef(
        instruction: NadelBatchHydrationFieldInstruction,
    ): Pair<NadelHydrationArgument, NadelHydrationArgument.ValueSource.FieldResultValue>? {
        return instruction.backingFieldArguments
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is NadelHydrationArgument.ValueSource.FieldResultValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle()
    }
}
