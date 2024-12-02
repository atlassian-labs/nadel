package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLType

class NadelAssignableTypeValidation internal constructor(
    private val typeWrappingValidation: NadelTypeWrappingValidation,
) {
    context(NadelValidationContext)
    fun isOutputTypeAssignable(
        overallType: GraphQLType,
        underlyingType: GraphQLType,
    ): Boolean {
        // Note: the (supplied) value comes from the underlying service, and that value needs to fit the (required) overall field's type
        return isTypeAssignable(
            suppliedType = underlyingType,
            requiredType = overallType,
            // Compare underlying type names
            suppliedTypeName = underlyingType.unwrapAll().name,
            requiredTypeName = instructionDefinitions.getUnderlyingTypeName(overallType.unwrapAll()),
        )
    }

    context(NadelValidationContext)
    fun isInputTypeAssignable(
        overallType: GraphQLType,
        underlyingType: GraphQLType,
    ): Boolean {
        // Note: the (supplied) value comes from the user (overall schema) and needs to be assigned to the (required) underlying type
        return isTypeAssignable(
            suppliedType = overallType,
            requiredType = underlyingType,
            // Compare underlying type names
            suppliedTypeName = instructionDefinitions.getUnderlyingTypeName(overallType.unwrapAll()),
            requiredTypeName = underlyingType.unwrapAll().name,
        )
    }

    context(NadelValidationContext)
    fun isTypeAssignable(
        suppliedType: GraphQLType,
        requiredType: GraphQLType,
        suppliedTypeName: String,
        requiredTypeName: String,
    ): Boolean {
        return suppliedTypeName == requiredTypeName && isTypeWrappingValid(suppliedType, requiredType)
    }

    private fun isTypeWrappingValid(
        suppliedType: GraphQLType,
        requiredType: GraphQLType,
    ): Boolean {
        return typeWrappingValidation.isTypeWrappingValid(
            lhs = suppliedType,
            rhs = requiredType,
            rule = NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME,
        )
    }
}
