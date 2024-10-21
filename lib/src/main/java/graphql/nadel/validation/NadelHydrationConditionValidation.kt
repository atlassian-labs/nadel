package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.dsl.NadelHydrationResultConditionDefinition
import graphql.nadel.engine.blueprint.directives.NadelHydrationArgumentDefinition
import graphql.nadel.engine.blueprint.directives.NadelHydrationDefinition
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeUtil
import java.math.BigInteger

internal class NadelHydrationConditionValidation {
    fun validateHydrationCondition(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): NadelSchemaValidationError? {
        val resultCondition = hydration.condition?.result
            ?: return null

        val pathToConditionSourceField = resultCondition.pathToSourceField
        val conditionSourceField: GraphQLFieldDefinition =
            (parent.overall as GraphQLFieldsContainer).getFieldAt(pathToConditionSourceField)
                ?: return NadelSchemaValidationError.HydrationConditionSourceFieldDoesNotExist(
                    pathToConditionSourceField,
                    overallField,
                )

        val sourceInputField = hydration.arguments
            .asSequence()
            .map { it.value }
            .filterIsInstance<NadelHydrationArgumentDefinition.ValueSource.ObjectField>()
            .map { it.pathToField }
            .single()

        val conditionSourceFieldType = if (pathToConditionSourceField == sourceInputField) {
            conditionSourceField.type.unwrapAll()
        } else {
            conditionSourceField.type.unwrapNonNull()
        }

        if (conditionSourceFieldType !is GraphQLScalarType) {
            return NadelSchemaValidationError.HydrationConditionUnsupportedFieldType(
                pathToConditionSourceField,
                GraphQLTypeUtil.simplePrint(conditionSourceField.type),
                overallField,
            )
        }

        return validateConditionFieldType(
            overallField = overallField,
            pathToConditionSourceField = pathToConditionSourceField,
            conditionSourceFieldType = conditionSourceFieldType,
            condition = resultCondition,
        )
    }

    private fun validateConditionFieldType(
        overallField: GraphQLFieldDefinition,
        pathToConditionSourceField: List<String>,
        conditionSourceFieldType: GraphQLScalarType,
        condition: NadelHydrationResultConditionDefinition,
    ): NadelSchemaValidationError? {
        val conditionSourceFieldTypeName: String = conditionSourceFieldType.name

        // Limit sourceField to simple values like String, Boolean, Int etc.
        if (!(conditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                conditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                conditionSourceFieldTypeName == Scalars.GraphQLID.name)
        ) {
            return NadelSchemaValidationError.HydrationConditionUnsupportedFieldType(
                pathToConditionSourceField,
                conditionSourceFieldTypeName,
                overallField,
            )
        }

        // Ensure predicate matches the field type used
        val predicateObject = condition.predicate

        if (predicateObject.equals != null) {
            val predicateValue = predicateObject.equals
            if (!(
                    predicateValue is String && conditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                        predicateValue is BigInteger && conditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                        predicateValue is String && conditionSourceFieldTypeName == Scalars.GraphQLID.name ||
                        predicateValue is BigInteger && conditionSourceFieldTypeName == Scalars.GraphQLID.name
                    )
            ) {
                return NadelSchemaValidationError.HydrationConditionPredicateDoesNotMatchSourceFieldType(
                    pathToConditionSourceField,
                    conditionSourceFieldTypeName,
                    predicateValue.javaClass.simpleName,
                    overallField,
                )
            }
        }
        if (predicateObject.startsWith != null || predicateObject.matches != null) {
            if (!(conditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                    conditionSourceFieldTypeName == Scalars.GraphQLID.name
                    )
            ) {
                val predicateType = if (predicateObject.startsWith != null) "startsWith" else "matches"
                return NadelSchemaValidationError.HydrationConditionPredicateRequiresStringSourceField(
                    pathToConditionSourceField,
                    conditionSourceFieldTypeName,
                    predicateType,
                    overallField,
                )
            }
        }
        if (predicateObject.matches != null) {
            try {
                predicateObject.matches.toRegex()
            } catch (e: Exception) {
                return NadelSchemaValidationError.ConditionalHydrationPredicateContainsInvalidRegex(
                    predicateObject.matches,
                    overallField,
                )
            }
        }
        return null
    }

    fun validateConditionsOnAllHydrations(
        hydrations: List<NadelHydrationDefinition>,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationError.SomeHydrationsHaveMissingConditions? {
        if (hydrations.all { (it.condition == null) } ||
            hydrations.all { (it.condition != null) }) {
            return null
        }
        return NadelSchemaValidationError.SomeHydrationsHaveMissingConditions(parent, overallField)
    }
}
