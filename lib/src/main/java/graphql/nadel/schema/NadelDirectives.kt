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

    val renamedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to rename a type or field in the overall schema"
            directive @renamed(
                "The type to be renamed"
                from: String!
            ) on FIELD_DEFINITION | OBJECT | INTERFACE | UNION | INPUT_OBJECT | SCALAR | ENUM
        """.trimIndent(),
    )

    val nadelBatchObjectIdentifiedByDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This is required by batch hydration to understand how to pull out objects from the batched result"
            input NadelBatchObjectIdentifiedBy {
                sourceId: String!
                resultId: String!
            }
        """.trimIndent(),
    )

    val nadelHydrationArgumentDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields"
            input NadelHydrationArgument {
                name: String!
                value: JSON!
            }
        """.trimIndent(),
    )

    val nadelHydrationResultFieldPredicateDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            input NadelHydrationResultFieldPredicate @oneOf {
                startsWith: String
                equals: JSON
                matches: String
            }
        """.trimIndent(),
    )

    val nadelHydrationResultConditionDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "Specify a condition for the hydration to activate based on the result"
            input NadelHydrationResultCondition {
                sourceField: String!
                predicate: NadelHydrationResultFieldPredicate!
            }
        """.trimIndent(),
    )

    val nadelHydrationConditionDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "Specify a condition for the hydration to activate"
            input NadelHydrationCondition {
                result: NadelHydrationResultCondition!
            }
        """.trimIndent(),
    )

    val hydratedDirectiveDefinition = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields"
            directive @hydrated(
                "The target service"
                service: String!
                "The target top level field"
                field: String!
                "How to identify matching results"
                identifiedBy: String! = "id"
                "How to identify matching results"
                inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
                "Are results indexed"
                indexed: Boolean! = false
                "Is querying batched"
                batched: Boolean! = false
                "The batch size"
                batchSize: Int! = 200
                "The timeout to use when completing hydration"
                timeout: Int! = -1
                "The arguments to the hydrated field"
                arguments: [NadelHydrationArgument!]!
                "Specify a condition for the hydration to activate"
                when: NadelHydrationCondition
            ) repeatable on FIELD_DEFINITION
        """.trimIndent(),
    )

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

    val nadelHydrationFromArgumentDefinition = parseDefinition<InputObjectTypeDefinition>(
        // language=GraphQL
        """
            "This allows you to hydrate new values into fields with the @hydratedFrom directive"
            input NadelHydrationFromArgument {
                name: String!
                valueFromField: String
                valueFromArg: String
            }
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
