package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.mapFrom
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

/**
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelBatchHydrationInputBuilder {
    internal fun getNonBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
    ): Map<NadelHydrationActorInputDef, NormalizedInputValue> {
        return mapFrom(
            instruction.actorInputValueDefs.mapNotNull { actorFieldArg ->
                when (val valueSource = actorFieldArg.valueSource) {
                    is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> {
                        val argValue: NormalizedInputValue? =
                            hydrationField.normalizedArguments[valueSource.argumentName]
                                ?: valueSource.defaultValue
                        if (argValue != null) {
                            actorFieldArg to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationActorInputDef.ValueSource.FieldResultValue -> null
                    is NadelHydrationActorInputDef.ValueSource.StaticValue -> {
                        val staticValue: NormalizedInputValue = makeNormalizedInputValue(
                            type = actorFieldArg.actorArgumentDef.type,
                            value = valueSource.value,
                        )
                        actorFieldArg to staticValue
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
    ): Pair<NadelHydrationActorInputDef, NadelHydrationActorInputDef.ValueSource.FieldResultValue>? {
        return instruction.actorInputValueDefs
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is NadelHydrationActorInputDef.ValueSource.FieldResultValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle()
    }
}
