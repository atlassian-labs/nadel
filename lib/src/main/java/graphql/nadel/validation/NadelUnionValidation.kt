package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.validation.NadelSchemaValidationError.UnionHasExtraType
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal class NadelUnionValidation(
    private val typeValidation: NadelTypeValidation,
) {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        val overallUnion = schemaElement.overall
        val underlyingUnion = schemaElement.underlying
        return if (overallUnion is GraphQLUnionType && underlyingUnion is GraphQLUnionType) {
            overallUnion.types
                .map { memberOverallType ->
                    validateUnionMember(
                        service = schemaElement.service,
                        overallUnion = overallUnion,
                        underlying = underlyingUnion,
                        memberOverallType = memberOverallType
                    )
                }
                .toResult()
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    private fun validateUnionMember(
        service: Service,
        overallUnion: GraphQLUnionType,
        underlying: GraphQLUnionType,
        memberOverallType: GraphQLNamedOutputType,
    ): NadelSchemaValidationResult {
        val memberUnderlyingType = NadelSchemaUtil.getUnderlyingType(memberOverallType, service)

        return if (memberUnderlyingType == null) {
            NadelSchemaValidationError.MissingUnderlyingType(
                service = service,
                overallType = memberOverallType,
            )
        } else if (!underlying.types.contains(memberUnderlyingType)) {
            UnionHasExtraType(
                service = service,
                unionType = overallUnion,
                extraType = memberOverallType as GraphQLObjectType,
            )
        } else {
            typeValidation.validate(
                NadelServiceSchemaElement(
                    service = service,
                    overall = memberOverallType,
                    underlying = memberUnderlyingType,
                ),
            )
        }
    }
}
