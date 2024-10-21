package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

sealed class NadelHydrationArgumentTypeValidationError {
    data class IncompatibleInputType(
        val suppliedType: GraphQLType,
        val requiredType: GraphQLInputType,
    ) : NadelHydrationArgumentTypeValidationError()

    data class IncompatibleField(
        val suppliedFieldContainer: GraphQLFieldsContainer,
        val suppliedField: GraphQLFieldDefinition,
        val requiredFieldContainer: GraphQLInputFieldsContainer,
        val requiredField: GraphQLInputObjectField,
    ) : NadelHydrationArgumentTypeValidationError()

    data class MissingInputField(
        val suppliedFieldContainer: GraphQLFieldsContainer,
        val requiredFieldContainer: GraphQLInputFieldsContainer,
        val requiredField: GraphQLInputObjectField,
    ) : NadelHydrationArgumentTypeValidationError()
}

class NadelHydrationArgumentTypeValidation {
    fun validateArgumentType(
        isBatchHydration: Boolean,
        suppliedType: GraphQLType,
        requiredType: GraphQLInputType,
    ): NadelHydrationArgumentTypeValidationError? {
        return validateArgumentType(
            suppliedType,
            requiredType,
        )
    }

    private fun validateArgumentType(
        suppliedType: GraphQLType,
        requiredType: GraphQLInputType,
    ): NadelHydrationArgumentTypeValidationError? {
        val suppliedTypeUnwrapped = suppliedType.unwrapAll()
        val requiredTypeUnwrapped = requiredType.unwrapAll()

        return when (requiredTypeUnwrapped) {
            is GraphQLInputObjectType -> validateInputObject(suppliedTypeUnwrapped, requiredTypeUnwrapped)
            else -> validateUnmodifiedType(suppliedTypeUnwrapped, requiredTypeUnwrapped)
        }
    }

    private fun validateUnmodifiedType(
        suppliedTypeUnwrapped: GraphQLUnmodifiedType,
        requiredTypeUnwrapped: GraphQLUnmodifiedType,
    ): NadelHydrationArgumentTypeValidationError? {
        if (requiredTypeUnwrapped is GraphQLEnumType && suppliedTypeUnwrapped is GraphQLEnumType) {
            return null
        } else if (requiredTypeUnwrapped is GraphQLInputObjectType && suppliedTypeUnwrapped is GraphQLInputObjectType) {
            return null
        } else if (requiredTypeUnwrapped is GraphQLScalarType && suppliedTypeUnwrapped is GraphQLScalarType) {
            return null
        }

        return NadelHydrationArgumentTypeValidationError.IncompatibleInputType(
            suppliedType = suppliedTypeUnwrapped,
            requiredType = requiredTypeUnwrapped as GraphQLInputType, // No intersection types…
        )
    }

    private fun validateInputObject(
        suppliedType: GraphQLUnmodifiedType,
        requiredObjectType: GraphQLInputObjectType,
    ): NadelHydrationArgumentTypeValidationError? {
        val suppliedObjectType = suppliedType as? GraphQLFieldsContainer
            ?: return NadelHydrationArgumentTypeValidationError.IncompatibleInputType(
                suppliedType = suppliedType,
                requiredType = requiredObjectType,
            )

        for (requiredField in requiredObjectType.fields) {
            val suppliedField = suppliedType.getField(requiredField.name)
                ?: return NadelHydrationArgumentTypeValidationError.MissingInputField(
                    suppliedFieldContainer = suppliedObjectType,
                    requiredFieldContainer = requiredObjectType,
                    requiredField = requiredField,
                )

            val typeError = validateArgumentType(
                suppliedType = suppliedField.type,
                requiredType = requiredField.type
            )
            if (typeError != null) {
                return typeError
            }
        }

        return null
    }
}
