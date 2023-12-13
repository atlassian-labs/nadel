package graphql.nadel.validation.util

import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.schema.NadelDirectives.dynamicServiceDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hiddenDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedFromDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedTemplateDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationComplexIdentifiedBy
import graphql.nadel.schema.NadelDirectives.nadelHydrationFromArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationTemplateEnumDefinition
import graphql.nadel.schema.NadelDirectives.nadelWhenConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelWhenConditionPredicateDefinition
import graphql.nadel.schema.NadelDirectives.nadelWhenConditionResultDefinition
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.renamedDirectiveDefinition

object NadelBuiltInTypes {
    val builtInScalars = setOf(
        GraphQLInt,
        GraphQLFloat,
        GraphQLString,
        GraphQLBoolean,
        GraphQLID,
    )

    val builtInScalarNames = builtInScalars
        .asSequence()
        .map { it.name }
        .toSet()

    val builtInDirectiveSyntaxTypeNames = sequenceOf<AnyNamedNode>(
        renamedDirectiveDefinition,
        hydratedDirectiveDefinition,
        nadelHydrationArgumentDefinition,
        dynamicServiceDirectiveDefinition,
        namespacedDirectiveDefinition,
        hiddenDirectiveDefinition,

        nadelHydrationFromArgumentDefinition,
        nadelHydrationComplexIdentifiedBy,
        nadelHydrationTemplateEnumDefinition,
        hydratedFromDirectiveDefinition,
        hydratedTemplateDirectiveDefinition,

        nadelWhenConditionPredicateDefinition,
        nadelWhenConditionResultDefinition,
        nadelWhenConditionDefinition,
    ).map {
        it.name
    }.toSet()

    val allNadelBuiltInTypeNames = builtInScalarNames + builtInDirectiveSyntaxTypeNames
}
