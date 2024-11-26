package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

class NadelAssignableTypeValidation internal constructor(
    private val typeWrappingValidation: NadelTypeWrappingValidation,
) {
    context(NadelValidationContext)
    fun isTypeAssignable(
        suppliedType: GraphQLType,
        requiredType: GraphQLType,
    ): Boolean {
        val typeWrappingValid = typeWrappingValidation.isTypeWrappingValid(
            lhs = suppliedType,
            rhs = requiredType,
            rule = NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME,
        )

        return typeWrappingValid && isTypeNameValid(suppliedType.unwrapAll(), requiredType.unwrapAll())
    }

    context(NadelValidationContext)
    private fun isTypeNameValid(
        overallType: GraphQLUnmodifiedType,
        underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        return getUnderlyingTypeName(overallType) == underlyingType.name
    }
}
