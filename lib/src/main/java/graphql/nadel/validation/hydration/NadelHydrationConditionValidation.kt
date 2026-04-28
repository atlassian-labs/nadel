package graphql.nadel.validation.hydration

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationResultFieldPredicateDefinition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.validation.NadelHydrationConditionIncompatibleValueError
import graphql.nadel.validation.NadelHydrationConditionInvalidEnumValueError
import graphql.nadel.validation.NadelHydrationConditionInvalidRegexError
import graphql.nadel.validation.NadelHydrationConditionMatchesPredicateRequiresStringFieldError
import graphql.nadel.validation.NadelHydrationConditionStartsWithPredicateRequiresStringFieldError
import graphql.nadel.validation.NadelHydrationMustAllHaveConditionError
import graphql.nadel.validation.NadelHydrationResultConditionReferencesNonExistentFieldError
import graphql.nadel.validation.NadelHydrationResultConditionUnsupportedFieldTypeError
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.ok
import graphql.nadel.validation.onErrorCast
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLType
import java.math.BigInteger

private data class NadelHydrationConditionValidationContext(
    val parent: NadelServiceSchemaElement.FieldsContainer,
    val virtualField: GraphQLFieldDefinition,
    val hydration: NadelHydrationDefinition,
    val condition: NadelHydrationConditionDefinition,
)

private sealed class NadelConditionFieldType {
    abstract val graphQLType: GraphQLType

    object StringType : NadelConditionFieldType() {
        override val graphQLType = GraphQLString
    }

    object IntType : NadelConditionFieldType() {
        override val graphQLType = GraphQLInt
    }

    object IdType : NadelConditionFieldType() {
        override val graphQLType = GraphQLID
    }

    data class EnumType(
        val enumType: GraphQLEnumType,
    ) : NadelConditionFieldType() {
        override val graphQLType = enumType
    }
}

internal class NadelHydrationConditionValidation {
    context(NadelValidationContext)
    fun validateHydrations(
        hydrations: List<NadelHydrationDefinition>,
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (hydrations.all { it.condition == null } || hydrations.all { it.condition != null }) {
            return ok()
        }

        return NadelHydrationMustAllHaveConditionError(parent, virtualField)
    }

    context(NadelValidationContext)
    fun validateCondition(
        parent: NadelServiceSchemaElement.FieldsContainer,
        virtualField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        condition: NadelHydrationConditionDefinition,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        val conditionContext = NadelHydrationConditionValidationContext(
            parent = parent,
            virtualField = virtualField,
            hydration = hydration,
            condition = condition,
        )

        with(conditionContext) {
            return validate(condition.result)
        }
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun validate(
        resultCondition: NadelHydrationResultConditionDefinition,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        val pathToConditionField = resultCondition.pathToSourceField

        val conditionField: GraphQLFieldDefinition = getConditionSourceField(pathToConditionField)
            .onErrorCast { return it }

        val requiredConditionType = getResultFieldType(pathToConditionField, conditionField)
            .onErrorCast { return it }

        return validateResultCondition(
            resultCondition = resultCondition,
            conditionFieldType = requiredConditionType,
            predicateDefinition = resultCondition.predicate,
        )
    }

    /**
     * The result field used in a condition must have a type supported by [NadelHydrationCondition].
     */
    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun getResultFieldType(
        pathToConditionField: List<String>,
        conditionField: GraphQLFieldDefinition,
    ): NadelValidationInterimResult<NadelConditionFieldType> {
        val conditionFieldOutputType = if (isConditionFieldSameAsBatchId(pathToConditionField)) {
            conditionField.type.unwrapAll() // Accept list if it's the batch ID, so that each batch ID can have its own instruction
        } else {
            conditionField.type.unwrapNonNull()  // We do not accept list, hence not unwrapAll
        }

        val conditionFieldType = when (conditionFieldOutputType) {
            GraphQLString -> NadelConditionFieldType.StringType
            GraphQLInt -> NadelConditionFieldType.IntType
            GraphQLID -> NadelConditionFieldType.IdType
            is GraphQLEnumType -> NadelConditionFieldType.EnumType(conditionFieldOutputType)
            else -> null
        } ?: return NadelHydrationResultConditionUnsupportedFieldTypeError(
            parentType = parent,
            virtualField = virtualField,
            hydration = hydration,
            pathToConditionField = pathToConditionField,
            conditionField = conditionField,
        ).asInterimError()

        return conditionFieldType.asInterimSuccess()
    }

    /**
     * For batch hydration, each input ID in an input list can have its own @hydrated instruction.
     *
     * If the condition field is the same as the batch input ID field, we should permit the type to be a list.
     */
    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun isConditionFieldSameAsBatchId(
        pathToConditionField: List<String>,
    ): Boolean {
        val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)!!

        // Not batch hydration
        if (!backingField.type.unwrapNonNull().isList) {
            return false
        }

        val batchIdFieldPath = hydration.arguments.asSequence()
            .filterIsInstance<NadelHydrationArgumentDefinition.ObjectField>()
            .map { it.pathToField }
            .single()

        return pathToConditionField == batchIdFieldPath
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun getConditionSourceField(
        pathToConditionSourceField: List<String>,
    ): NadelValidationInterimResult<GraphQLFieldDefinition> {
        val field = parent.overall.getFieldAt(pathToConditionSourceField)
            ?: return NadelHydrationResultConditionReferencesNonExistentFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration,
                pathToConditionField = pathToConditionSourceField,
            ).asInterimError()

        return field.asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun validateResultCondition(
        resultCondition: NadelHydrationResultConditionDefinition,
        conditionFieldType: NadelConditionFieldType,
        predicateDefinition: NadelHydrationResultFieldPredicateDefinition,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        if (predicateDefinition.equals != null) {
            return validateEqualsCondition(
                conditionFieldType,
                resultCondition,
                predicateDefinition.equals,
            )
        }

        if (predicateDefinition.startsWith != null) {
            return validateStartsWithCondition(
                conditionFieldType,
                resultCondition,
                predicateDefinition.startsWith,
            )
        }

        if (predicateDefinition.matches != null) {
            return validateMatchesCondition(
                conditionFieldType,
                resultCondition,
                predicateDefinition.matches,
            )
        }

        throw UnsupportedOperationException("Unsupported predicate")
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun validateStartsWithCondition(
        conditionFieldType: NadelConditionFieldType,
        resultCondition: NadelHydrationResultConditionDefinition,
        startsWith: String,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        return if (conditionFieldType == NadelConditionFieldType.StringType || conditionFieldType == NadelConditionFieldType.IdType) {
            NadelHydrationCondition.StringResultStartsWith(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                prefix = startsWith,
            ).asInterimSuccess()
        } else {
            NadelHydrationConditionStartsWithPredicateRequiresStringFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration,
                pathToConditionField = resultCondition.pathToSourceField,
            ).asInterimError()
        }
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun validateMatchesCondition(
        conditionFieldType: NadelConditionFieldType,
        resultCondition: NadelHydrationResultConditionDefinition,
        matches: String,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        return if (conditionFieldType == NadelConditionFieldType.StringType || conditionFieldType == NadelConditionFieldType.IdType) {
            val regex = try {
                matches.toRegex()
            } catch (e: Exception) {
                return NadelHydrationConditionInvalidRegexError(
                    parentType = parent,
                    virtualField = virtualField,
                    hydration = hydration,
                    regexString = matches,
                ).asInterimError()
            }

            NadelHydrationCondition.StringResultMatches(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                regex = regex,
            ).asInterimSuccess()
        } else {
            NadelHydrationConditionMatchesPredicateRequiresStringFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration,
                pathToConditionField = resultCondition.pathToSourceField,
            ).asInterimError()
        }
    }

    context(NadelValidationContext, NadelHydrationConditionValidationContext)
    private fun validateEqualsCondition(
        conditionFieldType: NadelConditionFieldType,
        resultCondition: NadelHydrationResultConditionDefinition,
        expectedValue: Any,
    ): NadelValidationInterimResult<NadelHydrationCondition> {
        if (expectedValue is String && (conditionFieldType == NadelConditionFieldType.StringType || conditionFieldType == NadelConditionFieldType.IdType)) {
            return NadelHydrationCondition.StringResultEquals(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                value = expectedValue,
            ).asInterimSuccess()
        } else if (expectedValue is String && conditionFieldType is NadelConditionFieldType.EnumType) {
            if (conditionFieldType.enumType.getValue(expectedValue) == null) {
                return NadelHydrationConditionInvalidEnumValueError(
                    parentType = parent,
                    virtualField = virtualField,
                    hydration = hydration,
                    pathToConditionField = resultCondition.pathToSourceField,
                    enumType = conditionFieldType.enumType,
                    suppliedValue = expectedValue,
                ).asInterimError()
            }

            return NadelHydrationCondition.StringResultEquals(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                value = expectedValue,
            ).asInterimSuccess()
        } else if (expectedValue is BigInteger && (conditionFieldType == NadelConditionFieldType.IntType || conditionFieldType == NadelConditionFieldType.IdType)) {
            return NadelHydrationCondition.LongResultEquals(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                value = expectedValue.toLong(),
            ).asInterimSuccess()
        } else {
            return NadelHydrationConditionIncompatibleValueError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration,
                pathToConditionField = resultCondition.pathToSourceField,
                requiredType = conditionFieldType.graphQLType,
                suppliedValue = expectedValue,
            ).asInterimError()
        }
    }
}
