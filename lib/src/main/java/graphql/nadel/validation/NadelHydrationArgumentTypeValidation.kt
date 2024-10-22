package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.engine.util.isNullable
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

internal sealed class NadelHydrationArgumentTypeValidationResult {
    /**
     * Util to only run code on `this` if there is an error.
     */
    inline fun onError(onError: (Error) -> Unit) {
        if (this is Error) {
            onError(this)
        }
    }

    data object Success : NadelHydrationArgumentTypeValidationResult()

    sealed class Error : NadelHydrationArgumentTypeValidationResult()

    data class IncompatibleInputType(
        val suppliedType: GraphQLType,
        val requiredType: GraphQLType,
    ) : Error()

    data class IncompatibleField(
        val suppliedFieldContainer: GraphQLFieldsContainer,
        val suppliedField: GraphQLFieldDefinition,
        val requiredFieldContainer: GraphQLInputFieldsContainer,
        val requiredField: GraphQLInputObjectField,
    ) : Error()

    data class MissingInputField(
        val suppliedFieldContainer: GraphQLFieldsContainer,
        val requiredFieldContainer: GraphQLInputFieldsContainer,
        val requiredField: GraphQLInputObjectField,
    ) : Error()
}

internal class NadelHydrationArgumentTypeValidation {
    private val typeWrappingValidation = NadelTypeWrappingValidation()

    fun isAssignable(
        isBatchHydration: Boolean,
        suppliedType: GraphQLType,
        requiredType: GraphQLInputType,
    ): NadelHydrationArgumentTypeValidationResult {
        if (isBatchHydration) {
            validateArgumentType(
                suppliedType.unwrapAll(),
                requiredType.unwrapAll(),
            ).onError { return it }
        } else {
            validateArgumentType(
                suppliedType,
                requiredType,
            ).onError { return it }
        }

        validateArgumentTypeRecursively(
            suppliedType.unwrapAll(),
            requiredType.unwrapAll(),
        ).onError { return it }

        return NadelHydrationArgumentTypeValidationResult.Success
    }

    private fun validateArgumentType(
        suppliedType: GraphQLType,
        requiredType: GraphQLType,
    ): NadelHydrationArgumentTypeValidationResult {
        val isTypeWrappingValid = typeWrappingValidation.isTypeWrappingValid(
            lhs = suppliedType,
            rhs = requiredType,
            rule = LHS_MUST_BE_STRICTER_OR_SAME,
        )

        if (!isTypeWrappingValid) {
            return NadelHydrationArgumentTypeValidationResult.IncompatibleInputType(
                suppliedType = suppliedType,
                requiredType = requiredType,
            )
        }

        validateUnmodifiedType(suppliedType, requiredType)
            .onError { return it }

        return NadelHydrationArgumentTypeValidationResult.Success
    }

    /**
     * We are only comparing object types that are not immediately obviously equal
     *
     * i.e. object type to input object type
     */
    private fun validateArgumentTypeRecursively(
        suppliedType: GraphQLUnmodifiedType,
        requiredType: GraphQLUnmodifiedType,
    ): NadelHydrationArgumentTypeValidationResult {
        // Note: input object to input object is checked elsewhere, no need to check recursively
        if (suppliedType is GraphQLFieldsContainer && requiredType is GraphQLInputObjectType) {
            return validateOutputObjectToInputObject(
                suppliedObjectType = suppliedType,
                requiredInputObjectType = requiredType,
            )
        }

        return NadelHydrationArgumentTypeValidationResult.Success
    }

    private fun validateUnmodifiedType(
        suppliedType: GraphQLType,
        requiredType: GraphQLType,
    ): NadelHydrationArgumentTypeValidationResult {
        val suppliedTypeUnwrapped = suppliedType.unwrapAll()
        val requiredTypeUnwrapped = requiredType.unwrapAll()

        if (requiredTypeUnwrapped is GraphQLEnumType && suppliedTypeUnwrapped is GraphQLEnumType) {
            if (suppliedTypeUnwrapped.name == requiredTypeUnwrapped.name) {
                return NadelHydrationArgumentTypeValidationResult.Success
            }
        } else if (requiredTypeUnwrapped is GraphQLInputObjectType) {
            when (suppliedTypeUnwrapped) {
                is GraphQLInputObjectType -> if (requiredTypeUnwrapped.name == suppliedTypeUnwrapped.name) {
                    return NadelHydrationArgumentTypeValidationResult.Success
                }
                is GraphQLObjectType -> return NadelHydrationArgumentTypeValidationResult.Success
            }
        } else if (requiredTypeUnwrapped is GraphQLScalarType && suppliedTypeUnwrapped is GraphQLScalarType) {
            if (isScalarCompatible(suppliedTypeUnwrapped, requiredTypeUnwrapped)) {
                return NadelHydrationArgumentTypeValidationResult.Success
            }
        }

        return NadelHydrationArgumentTypeValidationResult.IncompatibleInputType(
            suppliedType = suppliedType,
            requiredType = requiredType,
        )
    }

    private fun isScalarCompatible(suppliedType: GraphQLNamedInputType, requiredType: GraphQLNamedInputType): Boolean {
        if (suppliedType.name == requiredType.name) {
            return true
        }

        // Per the spec, when ID is used as an input type, it accepts both Strings and Ints
        if (requiredType.name == Scalars.GraphQLID.name) {
            return suppliedType.name == Scalars.GraphQLString.name ||
                suppliedType.name == Scalars.GraphQLInt.name ||
                suppliedType.name == ExtendedScalars.GraphQLLong.name
        }

        // Accept ID into String for compatibility
        if (requiredType.name == Scalars.GraphQLString.name) {
            return suppliedType.name == Scalars.GraphQLID.name
        }

        return false
    }

    private fun validateOutputObjectToInputObject(
        suppliedObjectType: GraphQLFieldsContainer,
        requiredInputObjectType: GraphQLInputObjectType,
    ): NadelHydrationArgumentTypeValidationResult {
        for (requiredField in requiredInputObjectType.fields) {
            val suppliedField = suppliedObjectType.getField(requiredField.name)
                ?: if (requiredField.type.isNullable || requiredField.hasSetDefaultValue()) {
                    continue
                } else {
                    return NadelHydrationArgumentTypeValidationResult.MissingInputField(
                        suppliedFieldContainer = suppliedObjectType,
                        requiredFieldContainer = requiredInputObjectType,
                        requiredField = requiredField,
                    )
                }

            validateArgumentType(
                suppliedType = suppliedField.type,
                requiredType = requiredField.type,
            ).onError {
                return NadelHydrationArgumentTypeValidationResult.IncompatibleField(
                    suppliedFieldContainer = suppliedObjectType,
                    suppliedField = suppliedField,
                    requiredFieldContainer = requiredInputObjectType,
                    requiredField = requiredField,
                )
            }

            validateArgumentTypeRecursively(
                suppliedType = suppliedField.type.unwrapAll(),
                requiredType = requiredField.type.unwrapAll(),
            ).onError { return it }
        }

        return NadelHydrationArgumentTypeValidationResult.Success
    }
}
