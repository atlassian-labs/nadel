package graphql.nadel.enginekt.transform.query

import graphql.nadel.dsl.ExtendedFieldDefinition
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class GraphQLQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val transforms: List<GraphQLQueryTransform>,
) {
    fun transform(
        userContext: Any?,
        field: NormalizedField,
    ): List<NormalizedField> {
        val fieldDef = field.fieldDefinition.definition
        val extendedFieldDef = field.fieldDefinition.definition as? ExtendedFieldDefinition

        return listOf(field)
    }

    companion object {
        fun create(overallSchema: GraphQLSchema): GraphQLQueryTransformer {
            return GraphQLQueryTransformer(overallSchema, transforms = emptyList())
        }
    }
}
