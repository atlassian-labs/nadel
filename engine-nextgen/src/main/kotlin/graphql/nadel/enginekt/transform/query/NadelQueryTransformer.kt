package graphql.nadel.enginekt.transform.query

import graphql.nadel.dsl.ExtendedFieldDefinition
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLSchema

class NadelQueryTransformer(
    private val overallSchema: GraphQLSchema,
    private val transforms: List<NadelQueryTransform>,
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
        fun create(overallSchema: GraphQLSchema): NadelQueryTransformer {
            return NadelQueryTransformer(overallSchema, transforms = emptyList())
        }
    }
}
