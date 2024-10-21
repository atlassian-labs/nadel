package graphql.nadel.schema

import graphql.GraphQLContext
import graphql.execution.ValuesResolver
import graphql.language.DirectiveDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.nadel.dsl.NadelPartitionDefinition
import graphql.nadel.engine.util.singleOfType
import graphql.parser.Parser
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLFieldDefinition
import java.util.Locale
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultFieldPredicateDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.engine.util.parseDefinition

/**
 * If you update this file please add to NadelBuiltInTypes
 */
object NadelDirectives {
    val deferDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
          directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
        """.trimIndent(),
    )

    val renamedDirectiveDefinition = NadelRenamedDefinition.directiveDefinition

    val nadelBatchObjectIdentifiedByDefinition = NadelBatchObjectIdentifiedByDefinition.inputValueDefinition

    val nadelHydrationArgumentDefinition = NadelHydrationArgumentDefinition.inputValueDefinition

    val nadelHydrationResultFieldPredicateDefinition = NadelHydrationResultFieldPredicateDefinition.inputValueDefinition

    val nadelHydrationResultConditionDefinition = NadelHydrationResultConditionDefinition.inputObjectDefinition

    val nadelHydrationConditionDefinition = NadelHydrationConditionDefinition.inputObjectDefinition

    val hydratedDirectiveDefinition = NadelHydrationDefinition.directiveDefinition

    val dynamicServiceDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field uses dynamic service resolution. This directive should only be used in commons fields, i.e. fields that are not part of a particular service."
            directive @dynamicServiceResolution on FIELD_DEFINITION
        """.trimIndent(),
    )

    val namespacedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field is a namespaced field."
            directive @namespaced on FIELD_DEFINITION
        """.trimIndent(),
    )

    val hiddenDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "Indicates that the field is not available for queries or introspection"
            directive @hidden on FIELD_DEFINITION
        """.trimIndent(),
    )

    val partitionDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to partition a field"
            directive @partition(
                "The path to the split point"
                pathToPartitionArg: [String!]!
            ) on FIELD_DEFINITION
        """.trimIndent()
    )

    @Deprecated(message = "To be replaced with directive wrapper class")
    internal fun createPartitionDefinition(fieldDefinition: GraphQLFieldDefinition): NadelPartitionDefinition? {
        val directive = fieldDefinition.getAppliedDirective(partitionDirectiveDefinition.name)
            ?: return null
        val pathToPartitionArg = getDirectiveValue<List<String>>(directive, "pathToPartitionArg")

        return NadelPartitionDefinition(pathToPartitionArg)
    }

    private inline fun <reified T : Any> getDirectiveValue(
        directive: GraphQLAppliedDirective,
        name: String,
    ): T {
        val argument = directive.getArgument(name)
            ?: throw IllegalStateException("The @${directive.name} directive argument '$name' argument MUST be present")

        val value = resolveArgumentValue<Any?>(argument)
        return T::class.java.cast(value)
    }

    private fun <T> resolveArgumentValue(graphQLArgument: GraphQLAppliedDirectiveArgument): T {
        @Suppress("UNCHECKED_CAST") // Trust caller. Can't do much
        return ValuesResolver.valueToInternalValue(
            graphQLArgument.argumentValue,
            graphQLArgument.type,
            GraphQLContext.getDefault(),
            Locale.getDefault()
        ) as T
    }

    private inline fun <reified T : SDLDefinition<*>> parseDefinition(sdl: String): T {
        return Parser.parse(sdl).definitions.singleOfType()
    }
}
