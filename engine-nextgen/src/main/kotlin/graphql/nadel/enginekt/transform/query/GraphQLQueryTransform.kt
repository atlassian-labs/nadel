package graphql.nadel.enginekt.transform.query

import graphql.nadel.Service
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

interface GraphQLQueryTransform {
    fun transform(
        service: Service,
        overallSchema: GraphQLSchema,
        normalizedField: NormalizedField,
    ): List<NormalizedField>
}
