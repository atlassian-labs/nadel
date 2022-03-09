package graphql.nadel.validation

import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.isNonNull
import graphql.nadel.enginekt.util.isNotWrapped
import graphql.nadel.enginekt.util.isWrapped
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.unwrapNonNull
import graphql.nadel.enginekt.util.unwrapOne
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelInputValidation {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLInputObjectType && schemaElement.underlying is GraphQLInputObjectType) {
            validate(
                parent = schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    private fun validate(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLInputObjectField>,
        underlyingFields: List<GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields.flatMap { overallField ->
            validate(parent, overallField, underlyingFieldsByName)
        }
    }

    private fun validate(
            parent: NadelServiceSchemaElement,
            overallInputField: GraphQLInputObjectField,
            underlyingFieldsByName: Map<String, GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingInputField = underlyingFieldsByName[overallInputField.name]
        return if (underlyingInputField == null) {
            listOf(
                    MissingUnderlyingInputField(parent, overallInputField),
            )
        } else {
            validate(parent, overallInputField, underlyingInputField)
        }
    }

    private fun validate(parent: NadelServiceSchemaElement,
                         overallInputField: GraphQLInputObjectField,
                         underlyingInputField: GraphQLInputObjectField
    ): List<NadelSchemaValidationError>  {
        return if (!isInputTypeValid(overallInputField.type, underlyingInputField.type)) {
            listOf(IncompatibleFieldInputType(parent, overallInputField, underlyingInputField))
        } else {
            listOf()
        }
    }

    fun validate(parent: NadelServiceSchemaElement,
                 overallField: GraphQLFieldDefinition,
                 overallInputArgument: GraphQLArgument,
                 underlyingInputArgument: GraphQLArgument
    ): List<NadelSchemaValidationError>  {
        return if (!isInputTypeValid(overallInputArgument.type, underlyingInputArgument.type)) {
            listOf(IncompatibleArgumentInputType(parent, overallField, overallInputArgument, underlyingInputArgument))
        } else {
            listOf()
        }
    }

    /**
     * It checks whether the type name and type wrappings e.g. [graphql.schema.GraphQLNonNull] make sense.
     * Same as [NadelTypeValidation.isOutputTypeValid] but with the logic for acceptable nullability logic flipped
     * i.e. we allow the overall input type to be non-nullable and the underlying input type to be nullable
     */
    private fun isInputTypeValid(
            overallType: GraphQLInputType,
            underlyingType: GraphQLInputType,
    ): Boolean {
        var overall: GraphQLType = overallType
        var underlying: GraphQLType = underlyingType

        while (overall.isWrapped && underlying.isWrapped) {
            if (underlying.isNonNull && !overall.isNonNull) {
                // Overall type is allowed to have looser restrictions
                underlying = underlying.unwrapOne()
            } else if ((overall.isList && underlying.isList) || (overall.isNonNull && underlying.isNonNull)) {
                overall = overall.unwrapOne()
                underlying = underlying.unwrapOne()
            } else {
                return false
            }
        }

        if (overall.isNotWrapped && underlying.isNotWrapped) {
            return isInputTypeNameValid(
                    overallType = overall as GraphQLUnmodifiedType,
                    underlyingType = underlying as GraphQLUnmodifiedType,
            )
        } else if (overall.isWrapped && underlying.isNotWrapped) {
            if (overall.isNonNull && overall.unwrapNonNull().isNotWrapped) {
                return isInputTypeNameValid(
                        overallType = overall.unwrapNonNull() as GraphQLUnmodifiedType,
                        underlyingType = underlying as GraphQLUnmodifiedType,
                )
            }
            return false
        } else {
            return false
        }
    }

    private fun isInputTypeNameValid(
            overallType: GraphQLUnmodifiedType,
            underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        return NadelSchemaUtil.getUnderlyingName(overallType) == underlyingType.name
    }

}
