package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.UnionHasExtraType
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLObjectType

internal class NadelUnionValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.Union,
    ): NadelSchemaValidationResult {
        return validateUnionMembers(schemaElement)
    }

    context(NadelValidationContext)
    private fun validateUnionMembers(
        schemaElement: NadelServiceSchemaElement.Union,
    ): NadelSchemaValidationResult {
        return schemaElement.overall.types
            .map { memberOverallType ->
                validateUnionMember(
                    parent = schemaElement,
                    memberOverallType = memberOverallType,
                )
            }
            .toResult()
    }

    context(NadelValidationContext)
    private fun validateUnionMember(
        parent: NadelServiceSchemaElement.Union,
        memberOverallType: GraphQLNamedOutputType,
    ): NadelSchemaValidationResult {
        val memberUnderlyingType = NadelSchemaUtil.getUnderlyingType(memberOverallType, parent.service)

        return if (memberUnderlyingType == null) {
            NadelSchemaValidationError.MissingUnderlyingType(
                service = parent.service,
                overallType = memberOverallType,
            )
        } else if (!parent.underlying.types.contains(memberUnderlyingType)) {
            UnionHasExtraType(
                service = parent.service,
                unionType = parent.overall,
                extraType = memberOverallType as GraphQLObjectType,
            )
        } else {
            // typeValidation.validate(
            //     NadelServiceSchemaElement.from(
            //         service = parent.service,
            //         overall = memberOverallType,
            //         underlying = memberUnderlyingType,
            //     ),
            // )
            wouldHaveVisited
                .add(
                    NadelServiceSchemaElement.from(
                        service = parent.service,
                        overall = memberOverallType,
                        underlying = memberUnderlyingType,
                    ).toRef(),
                )
            ok()
        }
    }
}
