package graphql.nadel.validation

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLString
import graphql.nadel.Service
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.isNotWrapped
import graphql.nadel.engine.util.isWrapped
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.util.getServiceTypes
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
) {
    private val fieldValidation = NadelFieldValidation(overallSchema, services, this)
    private val inputValidation = NadelInputValidation()
    private val unionValidation = NadelUnionValidation(this)
    private val enumValidation = NadelEnumValidation()
    private val interfaceValidation = NadelInterfaceValidation()

    fun validate(
        service: Service,
    ): List<NadelSchemaValidationError> {
        val (serviceTypes, serviceTypeErrors) = getServiceTypes(overallSchema, service)

        val fieldIssues = serviceTypes.asSequence()
            .flatMap(::validate)
            .toList()

        return serviceTypeErrors + fieldIssues
    }

    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        if (!visitElement(schemaElement)) {
            return emptyList()
        }

        if (schemaElement.overall.javaClass != schemaElement.underlying.javaClass) {
            return listOf(
                IncompatibleType(schemaElement),
            )
        }

        return fieldValidation.validate(schemaElement) +
            inputValidation.validate(schemaElement) +
            unionValidation.validate(schemaElement) +
            interfaceValidation.validate(schemaElement) +
            enumValidation.validate(schemaElement)
    }

    fun validateOutputType(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val overallType = overallField.type.unwrapAll()
        val underlyingType = underlyingField.type.unwrapAll()

        val typeServiceSchemaElement = NadelServiceSchemaElement(
            service = parent.service,
            overall = overallType,
            underlying = underlyingType,
        )

        // This checks whether the type is actually valid content wise
        val typeErrors = validate(typeServiceSchemaElement)

        // This checks whether the output type e.g. name or List or NonNull wrappings are valid
        val outputTypeError = when {
            isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type) -> null
            else -> IncompatibleFieldOutputType(parent, overallField, underlyingField)
        }

        return typeErrors + listOfNotNull(outputTypeError)
    }

    /**
     * Answers whether `rhs` assignable to `lhs`?
     *
     * i.e. does the following compile
     *
     * ```
     * vol output: lhs = rhs
     * ```
     *
     * Note: this assumes both types are from the same schema. This does NOT
     * deal with differences between overall and underlying schema.
     */
    fun isAssignableTo(lhs: GraphQLNamedOutputType, rhs: GraphQLNamedOutputType): Boolean {
        if (lhs.name == rhs.name) {
            return true
        }
        if (lhs.name == GraphQLID.name && rhs.name == GraphQLString.name) {
            return true
        }
        if (lhs is GraphQLInterfaceType && rhs is GraphQLImplementingType) {
            return rhs.interfaces.contains(lhs)
        }
        return false
    }

    /**
     * It checks whether the type name and type wrappings e.g. [graphql.schema.GraphQLNonNull] make sense.
     */
    private fun isOutputTypeValid(
        overallType: GraphQLOutputType,
        underlyingType: GraphQLOutputType,
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
            return isOutputTypeNameValid(
                overallType = overall as GraphQLUnmodifiedType,
                underlyingType = underlying as GraphQLUnmodifiedType,
            )
        } else if (overall.isNotWrapped && underlying.isWrapped) {
            if (underlying.isNonNull && underlying.unwrapNonNull().isNotWrapped) {
                return isOutputTypeNameValid(
                    overallType = overall as GraphQLUnmodifiedType,
                    underlyingType = underlying.unwrapNonNull() as GraphQLUnmodifiedType,
                )
            }
            return false
        } else {
            return false
        }
    }

    private fun isOutputTypeNameValid(
        overallType: GraphQLUnmodifiedType,
        underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        if (getUnderlyingName(overallType) == underlyingType.name) {
            return true
        }

        return false
    }

    private fun visitElement(schemaElement: NadelServiceSchemaElement): Boolean {
        val schemaElementRef = schemaElement.toRef()

        return if (schemaElementRef in context.visitedTypes) {
            false // Already visited
        } else {
            context.visitedTypes.add(schemaElementRef)
            true
        }
    }
}
