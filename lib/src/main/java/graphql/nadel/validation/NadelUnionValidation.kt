package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.UnionHasExtraType
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal class NadelUnionValidation(
    private val typeValidation: NadelTypeValidation,
) {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLUnionType && schemaElement.underlying is GraphQLUnionType) {
            schemaElement.overall.types
                .flatMap { memberOverallType ->
                    val memberUnderlyingType =
                        NadelSchemaUtil.getUnderlyingType(memberOverallType, schemaElement.service)

                    if (memberUnderlyingType == null) {
                        listOf(
                            NadelSchemaValidationError.MissingUnderlyingType(
                                service = schemaElement.service,
                                overallType = memberOverallType,
                            ),
                        )
                    } else if (!schemaElement.underlying.types.contains(memberUnderlyingType)) {
                        listOf(
                            UnionHasExtraType(
                                service = schemaElement.service,
                                unionType = schemaElement.overall,
                                extraType = memberOverallType as GraphQLObjectType,
                            ),
                        )
                    } else {
                        typeValidation.validate(
                            NadelServiceSchemaElement(
                                service = schemaElement.service,
                                overall = memberOverallType,
                                underlying = memberUnderlyingType,
                            ),
                        )
                    }
                }
        } else {
            emptyList()
        }
    }
}
