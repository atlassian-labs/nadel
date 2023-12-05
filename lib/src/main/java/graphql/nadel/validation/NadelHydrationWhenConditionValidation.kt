package graphql.nadel.validation

import graphql.GraphQLContext
import graphql.Scalars
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLType
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLScalarType
import graphql.validation.ValidationUtil
import java.math.BigInteger
import java.util.Locale

internal class NadelHydrationWhenConditionValidation() {
    private val validationUtil = ValidationUtil()

    fun validateHydrationWhenConditionInput(
        hydrationSourceType: GraphQLType,
        actorFieldArgType: GraphQLInputType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: RemoteArgumentDefinition,
        hydration: UnderlyingServiceHydration,
        isBatchHydration: Boolean,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        if (hydration.conditionalHydration == null) {
            return null
        }

        val whenConditionSourceFieldName: String = hydration.conditionalHydration?.get("sourceField") as String
        val whenConditionSourceField = (parent.overall as GraphQLFieldsContainer).getField(whenConditionSourceFieldName)

        val whenConditionSourceFieldTypeName: String = ((whenConditionSourceField as GraphQLFieldDefinition).type as GraphQLScalarType).name


        // Limit sourceField to simple values like String, Boolean, Int etc.
        if( ! (whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                whenConditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                whenConditionSourceFieldTypeName == Scalars.GraphQLID.name)) {
            return NadelSchemaValidationError.WhenConditionSourceFieldNotASimpleType(whenConditionSourceFieldName, whenConditionSourceFieldTypeName, overallField)
        }

        // Ensure predicate matches the field type used
        val predicateObject = hydration.conditionalHydration?.get("predicate") as LinkedHashMap<String, Any>
        val predicateType = predicateObject.keys.first()
        val predicateValue = predicateObject.getValue(predicateType)

        if (predicateType == "equals" ) {
            if (!(
                predicateValue is String && whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                predicateValue is BigInteger && whenConditionSourceFieldTypeName == Scalars.GraphQLInt.name ||
                predicateValue is String && whenConditionSourceFieldTypeName == Scalars.GraphQLID.name ||
                predicateValue is BigInteger && whenConditionSourceFieldTypeName == Scalars.GraphQLID.name
            )) {
                return NadelSchemaValidationError.WhenConditionPredicateDoesNotMatchSourceFieldType(
                    whenConditionSourceFieldName,
                    whenConditionSourceFieldTypeName,
                    predicateValue.javaClass.simpleName,
                    overallField
                )
            }
        }
        if (predicateType == "startsWith" || predicateType == "matches") {
            if (!(whenConditionSourceFieldTypeName == Scalars.GraphQLString.name ||
                whenConditionSourceFieldTypeName == Scalars.GraphQLID.name
                )){
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
}