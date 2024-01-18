package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.dsl.NadelHydrationDefinition
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
        if (hydration.condition == null) {
            return null
        }

        val conditionSourceFieldName: String = hydration.condition.sourceField
        val conditionSourceField: GraphQLFieldDefinition =
            (parent.overall as GraphQLFieldsContainer).getField(conditionSourceFieldName)
                ?: return NadelSchemaValidationError.HydrationConditionSourceFieldDoesNotExist(
                    conditionSourceFieldName,
                    overallField,
                )

        val conditionSourceFieldType = conditionSourceField.type.unwrapNonNull()
        if (conditionSourceFieldType !is GraphQLScalarType) {
            return NadelSchemaValidationError.HydrationConditionUnsupportedFieldType(
                conditionSourceFieldName,
                GraphQLTypeUtil.simplePrint(conditionSourceField.type),
                overallField,
            )
        }

        val conditionSourceFieldTypeName: String = conditionSourceFieldType.name

        // Limit sourceField to simple values like String, Boolean, Int etc.
        if (!(conditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                conditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                conditionSourceFieldTypeName == Scalars.GraphQLID.name)
        ) {
            return NadelSchemaValidationError.HydrationConditionUnsupportedFieldType(
                conditionSourceFieldName,
                conditionSourceFieldTypeName,
                overallField,
            )
        }

        // Ensure predicate matches the field type used
        val predicateObject = hydration.condition.predicate

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
                    conditionSourceFieldName,
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
                    conditionSourceFieldName,
                    conditionSourceFieldTypeName,
                    predicateType,
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
