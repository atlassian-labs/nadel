package graphql.nadel.validation.util

import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLTypeUtil

object NadelInputValidationUtil {
    /**
     * Gets the input type name, with brackets if it's a list type and "!" if it's a non-nullable type
     * eg [InputType!]!
     *
     * @param graphQLInputType the type to get the name of
     *
     * @return the input type name
     */
    fun getInputTypeName(graphQLInputType: GraphQLInputType): String {
        var typeName = graphQLInputType.toString()
        if (graphQLInputType is GraphQLEnumType) {
            typeName = graphQLInputType.name
        }
        if (graphQLInputType is GraphQLInputObjectType) {
            typeName = graphQLInputType.name
        }
        if (graphQLInputType is GraphQLScalarType) {
            typeName = graphQLInputType.name
        }
        return typeName
    }

    /**
     * Returns the original input type name of an overlying input type that has been renamed
     *
     * @param graphQLInputType the input type to get the original name of
     *
     * @return original name of the input type that has been renamed
     */
    fun getUnderlyingInputTypeName(graphQLInputType: GraphQLInputType): String {
        val overallName = getInputTypeName(graphQLInputType)
        val unwrappedType: GraphQLNamedType = GraphQLTypeUtil.unwrapAll(graphQLInputType)
        val underlyingOriginalName: String = getUnderlyingName(unwrappedType)
        return overallName.replace(unwrappedType.name, underlyingOriginalName)
    }

    /**
     * Checks the overall and underlying input type to see if they have matching names, allowing for renames of overall
     * type and allowing for underlying type to be more strict i.e underlying is non-nullable while overall type is
     * nullable
     * @param overallInputType  - the overall GraphQLInputType
     * @param underlyingInputType - the underlying GraphQLInputType
     * @return - returns true if overall and underlying input type have matching names allowing for aforementioned cases
     */
    fun isMatchingInputTypeNameIgnoringNullableRenameScalar(overallInputType: GraphQLInputType, underlyingInputType: GraphQLInputType): Boolean {
        val underlyingArgType = getInputTypeName(underlyingInputType)
        val originalOverallArgType = getUnderlyingInputTypeName(overallInputType)
        return (originalOverallArgType == underlyingArgType || isSameTypeIgnoringOverallNullable(originalOverallArgType, underlyingArgType))
    }

    private fun isSameTypeIgnoringOverallNullable(originalOverallArgType: String, underlyingArgType: String): Boolean {
        return makeArgTypeNullable(originalOverallArgType) == underlyingArgType
        // o! -> u! good
        // o -> u good
        // o -> u! bad
        // o! -> u good   this is ok because the overall can require something even if the underlying doesnt ask for it
    }

    fun isNonNullType(graphQLInputType: GraphQLInputType?): Boolean {
        return graphQLInputType is GraphQLNonNull
    }

    /**
     * Takes in a string and removes the ! if it exists at the end
     */
    fun makeArgTypeNullable(underlyingArgType: String): String {
        return if (underlyingArgType.endsWith("!")) {
            underlyingArgType.substring(0, underlyingArgType.length - 1)
        } else {
            underlyingArgType
        }
    }
}
