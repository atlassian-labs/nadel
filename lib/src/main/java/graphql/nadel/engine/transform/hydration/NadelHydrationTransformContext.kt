package graphql.nadel.engine.transform.hydration

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.normalized.ExecutableNormalizedField

/**
 * Context for a generic hydration transform.
 */
internal interface NadelHydrationTransformContext {
    val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelGenericHydrationInstruction>>
    val hydrationCauseField: ExecutableNormalizedField
    val hydrationCauseService: Service
    val aliasHelper: NadelAliasHelper
}
