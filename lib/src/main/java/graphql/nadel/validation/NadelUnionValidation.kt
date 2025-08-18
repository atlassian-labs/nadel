package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.UnionHasExtraType
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLObjectType

class NadelUnionValidation internal constructor() {
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
                    memberOverallType = memberOverallType as GraphQLObjectType,
                )
            }
            .toResult()
    }

    context(NadelValidationContext)
    private fun validateUnionMember(
        parent: NadelServiceSchemaElement.Union,
        memberOverallType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        if (instructionDefinitions.isStubbed(memberOverallType)) {
            return NadelUnionMustNotReferenceStubbedObjectTypeError(parent, memberOverallType)
        }

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
                extraType = memberOverallType,
            )
        } else {
            ok()
        }
    }
}
