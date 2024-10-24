package graphql.nadel.engine.blueprint.directives

import graphql.language.ArrayValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.util.AnyAstValue
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

internal fun GraphQLFieldDefinition.isHydration(): Boolean {
    return hasAppliedDirective(NadelHydrationDirective.SyntaxConstant.hydrated)
}

internal fun GraphQLFieldDefinition.getHydratedOrNull(): NadelHydrationDirective? {
    val appliedDirective = getAppliedDirective(NadelHydrationDirective.SyntaxConstant.hydrated)
        ?: return null
    return NadelHydrationDirective(appliedDirective)
}

internal fun GraphQLFieldDefinition.getHydrated(): NadelHydrationDirective {
    return getHydratedOrNull()!!
}

/**
 * ```graphql
 * "This allows you to hydrate new values into fields"
 * directive @hydrated(
 *     "The target service"
 *     service: String!
 *     "The target top level field"
 *     field: String!
 *     "How to identify matching results"
 *     identifiedBy: String! = "id"
 *     "How to identify matching results"
 *     inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
 *     "Are results indexed"
 *     indexed: Boolean! = false
 *     "Is querying batched"
 *     batched: Boolean! = false
 *     "The batch size"
 *     batchSize: Int! = 200
 *     "The timeout to use when completing hydration"
 *     timeout: Int! = -1
 *     "The arguments to the hydrated field"
 *     arguments: [NadelHydrationArgument!]!
 *     "Specify a condition for the hydration to activate"
 *     when: NadelHydrationCondition
 * ) repeatable on FIELD_DEFINITION
 * ```
 */
internal class NadelHydrationDirective(
    private val appliedDirective: GraphQLAppliedDirective,
) {
    val backingField: List<String>
        get() = appliedDirective.getArgument(SyntaxConstant.field).getValue<String>().split(".")

    val identifiedBy: String
        get() = appliedDirective.getArgument(SyntaxConstant.identifiedBy).getValue()

    val isIndexed: Boolean
        get() = appliedDirective.getArgument(SyntaxConstant.indexed).getValue()

    val isBatched: Boolean
        get() = appliedDirective.getArgument(SyntaxConstant.batched).getValue()

    val batchSize: Boolean
        get() = appliedDirective.getArgument(SyntaxConstant.batchSize).getValue()

    val arguments: List<NadelHydrationArgumentDefinition>
        get() = appliedDirective.getArgument(SyntaxConstant.arguments).getValue<ArrayValue>()
            .values
            .map {
                NadelHydrationArgumentDefinition(it as ObjectValue)
            }

    object SyntaxConstant {
        const val hydrated = "hydrated"
        const val field = "field"
        const val identifiedBy = "identifiedBy"
        const val indexed = "indexed"
        const val batched = "batched"
        const val batchSize = "batchSize"
        const val arguments = "arguments"
    }
}

/**
 * Argument belonging to [NadelHydrationDirective.arguments]
 */
internal class NadelHydrationArgumentDefinition(
    private val argumentObject: ObjectValue,
) {
    /**
     * Name of the backing field's argument.
     */
    val name: String
        get() = (argumentObject.getObjectField("name").value as StringValue).value

    /**
     * Value to support to the backing field's argument at runtime.
     */
    val value: AnyAstValue
        get() = argumentObject.getObjectField("name").value
}

internal fun ObjectValue.getObjectField(name: String): ObjectField {
    return objectFields.first { it.name == name }
}
