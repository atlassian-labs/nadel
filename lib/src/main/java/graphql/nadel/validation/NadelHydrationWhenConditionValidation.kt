package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeUtil
import java.math.BigInteger

internal class NadelHydrationWhenConditionValidation() {

    fun validateHydrationWhenConditionInput(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelSchemaValidationError? {
        if (hydration.conditionalHydration == null) {
            return null
        }

        val whenConditionSourceFieldName: String = hydration.conditionalHydration.sourceField
        val whenConditionSourceField: GraphQLFieldDefinition =
            (parent.overall as GraphQLFieldsContainer).getField(whenConditionSourceFieldName)
                ?: return NadelSchemaValidationError.WhenConditionSourceFieldDoesNotExist(
                    whenConditionSourceFieldName,
                    overallField
                )

        if (whenConditionSourceField.type.unwrapNonNull() !is GraphQLScalarType) {
            return NadelSchemaValidationError.WhenConditionUnsupportedFieldType(
                whenConditionSourceFieldName,
                GraphQLTypeUtil.simplePrint(whenConditionSourceField.type),
                overallField
            )
        }
        val whenConditionSourceFieldTypeName: String = (whenConditionSourceField.type as GraphQLScalarType).name

        // Limit sourceField to simple values like String, Boolean, Int etc.
        if (!(whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                whenConditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                whenConditionSourceFieldTypeName == Scalars.GraphQLID.name)
        ) {
            return NadelSchemaValidationError.WhenConditionUnsupportedFieldType(
                whenConditionSourceFieldName,
                whenConditionSourceFieldTypeName,
                overallField
            )
        }

        // Ensure predicate matches the field type used
        val predicateObject = hydration.conditionalHydration.predicate

        if (predicateObject.equals != null) {
            val predicateValue = predicateObject.equals
            if (!(
                    predicateValue is String && whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                        predicateValue is BigInteger && whenConditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                        predicateValue is String && whenConditionSourceFieldTypeName == Scalars.GraphQLID.name ||
                        predicateValue is BigInteger && whenConditionSourceFieldTypeName == Scalars.GraphQLID.name
                    )
            ) {
                return NadelSchemaValidationError.WhenConditionPredicateDoesNotMatchSourceFieldType(
                    whenConditionSourceFieldName,
                    whenConditionSourceFieldTypeName,
                    predicateValue.javaClass.simpleName,
                    overallField
                )
            }
        }
        if (predicateObject.startsWith != null || predicateObject.matches != null ) {
            val predicateType = if (predicateObject.startsWith != null) "startsWith" else "matches"
            if (!(whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                    whenConditionSourceFieldTypeName == Scalars.GraphQLID.name
                    )
            ) {
                return NadelSchemaValidationError.WhenConditionPredicateRequiresStringSourceField(
                    whenConditionSourceFieldName,
                    whenConditionSourceFieldTypeName,
                    predicateType,
                    overallField
                )
            }
        }
        return null
    }

    fun validateConditionsOnAllHydrations(
        hydrations: List<UnderlyingServiceHydration>,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationError.SomeHydrationsHaveMissingConditions? {
        if (hydrations.all { (it.conditionalHydration == null) } ||
            hydrations.all { (it.conditionalHydration != null) }) {
            return null
        }
        return NadelSchemaValidationError.SomeHydrationsHaveMissingConditions(parent, overallField)
    }
}