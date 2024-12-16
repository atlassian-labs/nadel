package graphql.nadel.hooks

import graphql.GraphQLError
import graphql.nadel.ServiceLike

sealed class NadelDynamicServiceResolutionResult {
    data class Success(val service: ServiceLike) : NadelDynamicServiceResolutionResult()
    data class Error(val error: GraphQLError) : NadelDynamicServiceResolutionResult()
}
