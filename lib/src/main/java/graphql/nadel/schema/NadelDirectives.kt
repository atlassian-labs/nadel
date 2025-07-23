package graphql.nadel.schema

import graphql.language.DirectiveDefinition
import graphql.language.SDLDefinition
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultFieldPredicateDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDefinition
import graphql.nadel.definition.partition.NadelPartitionDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.definition.stubbed.NadelStubbedDefinition
import graphql.nadel.definition.virtualType.NadelVirtualTypeDefinition
import graphql.nadel.engine.util.singleOfType
import graphql.parser.Parser

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

    val defaultHydrationDirectiveDefinition = NadelDefaultHydrationDefinition.directiveDefinition

    val idHydratedDirectiveDefinition = NadelIdHydrationDefinition.directiveDefinition

    val stubbedDirectiveDefinition = NadelStubbedDefinition.directiveDefinition

    val nadelHydrationRemainingArguments = parseDefinition<DirectiveDefinition>(
        // language=GraphQL
        """
          directive @hydrationRemainingArguments on ARGUMENT_DEFINITION
        """.trimIndent()
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

    val partitionDirectiveDefinition = NadelPartitionDefinition.directiveDefinition

    val virtualTypeDirectiveDefinition = NadelVirtualTypeDefinition.directiveDefinition

    private inline fun <reified T : SDLDefinition<*>> parseDefinition(sdl: String): T {
        return Parser.parse(sdl).definitions.singleOfType()
    }
}
