package graphql.nadel.validation.util

import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.schema.NadelDirectives.defaultHydrationDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.deferDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.dynamicServiceDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hiddenDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.idHydratedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.nadelBatchObjectIdentifiedByDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationArgumentDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationRemainingArguments
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultConditionDefinition
import graphql.nadel.schema.NadelDirectives.nadelHydrationResultFieldPredicateDefinition
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.partitionDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.renamedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.stubbedDirectiveDefinition
import graphql.nadel.schema.NadelDirectives.virtualTypeDirectiveDefinition

object NadelBuiltInTypes {
    val builtInScalars = setOf(
        GraphQLInt,
        GraphQLFloat,
        GraphQLString,
        GraphQLBoolean,
        GraphQLID,
    )

    val builtInScalarNames = builtInScalars
        .mapTo(LinkedHashSet()) {
            it.name
        }

    val builtInDirectiveSyntaxTypeNames = sequenceOf<AnyNamedNode>(
        renamedDirectiveDefinition,
        hydratedDirectiveDefinition,
        nadelHydrationArgumentDefinition,
        dynamicServiceDirectiveDefinition,
        namespacedDirectiveDefinition,
        hiddenDirectiveDefinition,
        deferDirectiveDefinition,
        partitionDirectiveDefinition,

        nadelBatchObjectIdentifiedByDefinition,

        nadelHydrationResultFieldPredicateDefinition,
        nadelHydrationResultConditionDefinition,
        nadelHydrationConditionDefinition,
        nadelHydrationRemainingArguments,

        virtualTypeDirectiveDefinition,
        defaultHydrationDirectiveDefinition,
        idHydratedDirectiveDefinition,
        stubbedDirectiveDefinition,
    ).mapTo(LinkedHashSet()) {
        it.name
    }

    val allNadelBuiltInTypeNames = builtInScalarNames + builtInDirectiveSyntaxTypeNames
}
