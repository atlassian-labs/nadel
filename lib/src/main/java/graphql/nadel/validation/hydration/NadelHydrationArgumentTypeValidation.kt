package graphql.nadel.validation.hydration

import graphql.Scalars
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.engine.util.isNullable
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelTypeWrappingValidation
import graphql.nadel.validation.NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME
import graphql.nadel.validation.getListCardinality
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

    /**
     * Batch argument must be a 1D array e.g. `userIds: [ID]`
     */
    data object InvalidBackingFieldBatchIdArg : Error()

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

internal class NadelHydrationArgumentTypeValidation(
    private val typeWrappingValidation: NadelTypeWrappingValidation,
) {
    fun isAssignable(
        isBatchHydration: Boolean,
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
        suppliedType: GraphQLType,
        requiredType: GraphQLInputType,
    ): NadelHydrationArgumentTypeValidationResult {
        if (isBatchHydration) {
            // Object must be fed into 1D array
            if (hydrationArgumentDefinition is NadelHydrationArgumentDefinition.ObjectField) {
                validateBatchHydrationSourceFieldBackingArgument(
                    requiredType = requiredType,
                ).onError { return it }
            }

            // Batch hydration is the wild west
            validateArgumentType(
                suppliedType = suppliedType.unwrapAll(),
                requiredType = requiredType.unwrapAll(),
            ).onError { return it }
        } else {
            if (hydrationArgumentDefinition is NadelHydrationArgumentDefinition.ObjectField) {
                // todo: this should only unwrap not-null but we don't handle NadelHydrationStrategy.ManyToOne
                validateArgumentType(
                    suppliedType = suppliedType.unwrapAll(),
                    requiredType = requiredType.unwrapAll(),
                ).onError { return it }
            } else {
                validateArgumentType(
                    suppliedType = suppliedType,
                    requiredType = requiredType,
                ).onError { return it }
            }
        }

        validateArgumentTypeRecursively(
            suppliedType = suppliedType.unwrapAll(),
            requiredType = requiredType.unwrapAll(),
        ).onError { return it }

        return NadelHydrationArgumentTypeValidationResult.Success
    }

    private fun validateBatchHydrationSourceFieldBackingArgument(
        requiredType: GraphQLInputType,
    ): NadelHydrationArgumentTypeValidationResult {
        if (requiredType.getListCardinality() != 1) {
            return NadelHydrationArgumentTypeValidationResult.InvalidBackingFieldBatchIdArg
        }

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
     * We are only comparing object types that are not obviously equal
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
            if (isEnumAssignable(suppliedTypeUnwrapped, requiredTypeUnwrapped)) {
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

    private fun isEnumAssignable(
        suppliedType: GraphQLEnumType,
        requiredType: GraphQLEnumType,
    ): Boolean {
        if (suppliedType.name == requiredType.name) {
            return true
        }

        // I hate to bake this into here… but ugh. They're not assignable. Will remove at some point
        if (suppliedType.name == "DevOpsProviderType" && requiredType.name == "ToolchainProviderType") {
            return true
        }

        // All values from supplied enum can be used as values in the required type
        return requiredType.values.map { it.name }
            .containsAll(suppliedType.values.map { it.name })
    }

    private fun isScalarCompatible(
        suppliedType: GraphQLNamedInputType,
        requiredType: GraphQLNamedInputType,
    ): Boolean {
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
