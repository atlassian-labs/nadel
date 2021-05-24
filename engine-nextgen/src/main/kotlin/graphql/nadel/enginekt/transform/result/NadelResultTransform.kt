package graphql.nadel.enginekt.transform.result

import graphql.nadel.Service
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

interface NadelResultTransform {
    fun isApplicable(
        userContext: Any?,
        overallSchema: GraphQLSchema,
        service: Service,
        field: NormalizedField,
    ): Boolean
}
