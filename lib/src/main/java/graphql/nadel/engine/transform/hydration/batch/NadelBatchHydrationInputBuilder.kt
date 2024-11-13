package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.util.emptyOrSingle
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
                when (argument) {
                    is NadelHydrationArgument.VirtualFieldArgument -> {
                        val argValue: NormalizedInputValue? =
                            virtualField.normalizedArguments[argument.virtualFieldArgumentName]
                                ?: argument.defaultValue
                        if (argValue != null) {
                            argument to argValue
                        } else {
                            null
                        }
                    }
                    // These are batch values, ignore them
                    is NadelHydrationArgument.SourceField -> null
                    is NadelHydrationArgument.StaticValue -> {
                        argument to argument.normalizedInputValue
                    }
                    is NadelHydrationArgument.RemainingVirtualFieldArguments -> {
                        argument to NormalizedInputValue(
                            /* typeName = */ GraphQLTypeUtil.simplePrint(argument.backingArgumentDef.type),
                            /* value = */
                            argument.remainingArgumentNames
                                .associateWith {
                                    virtualField.normalizedArguments[it]?.value
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
    internal fun getBatchArgument(
        instruction: NadelBatchHydrationFieldInstruction,
    ): NadelHydrationArgument.SourceField? {
        return instruction.backingFieldArguments
            .asSequence()
            .filterIsInstance<NadelHydrationArgument.SourceField>()
            .emptyOrSingle()
    }
}
