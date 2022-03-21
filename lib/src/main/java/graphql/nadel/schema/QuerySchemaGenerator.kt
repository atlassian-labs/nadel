package graphql.nadel.schema

import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLSchema
import graphql.schema.transform.FieldVisibilitySchemaTransformation
import graphql.schema.transform.VisibleFieldPredicate

internal object QuerySchemaGenerator {
    private val visibleFieldPredicate = VisibleFieldPredicate { environment ->
        if (environment.schemaElement is GraphQLDirectiveContainer) {
            val container = environment.schemaElement as GraphQLDirectiveContainer
            container.hasAppliedDirective(NadelDirectives.hiddenDirectiveDefinition.name) == false
        } else {
            true
        }
    }

    fun generateQuerySchema(engineSchema: GraphQLSchema): GraphQLSchema {
        return FieldVisibilitySchemaTransformation(visibleFieldPredicate).apply(engineSchema)
    }
}
