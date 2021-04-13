package graphql.nadel.engine.transformation.variables;

import graphql.PublicApi;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;

/**
 * This can take a variable and possibly transform the value
 * following the structure of the argument or input field elements.
 */
@PublicApi
public class InputValueTransformer {

    /**
     * Transforms a variable value starting from a GraphQLArgument or GraphQLInputObjectField and then descending its input type true
     * allowing you to rebuilds is value
     *
     * @param valueDefinition     the input value definition to transform for
     * @param coercedValue        the value to transform
     * @param inputValueTransform the transformer function
     *
     * @return a transformed object
     */
    public static Object transform(GraphQLInputValueDefinition valueDefinition, Object coercedValue, InputValueTransform inputValueTransform) {

        String argumentName = valueDefinition.getName();
        GraphQLInputType inputType = valueDefinition.getType();
        InputValueTree inputValueTree = new InputValueTree(null, argumentName, inputType, valueDefinition);

        return transformValue(coercedValue, argumentName, inputType, inputValueTree, inputValueTransform);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static Object transformValue(Object coercedValue, String name, GraphQLInputType type, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        if (coercedValue == null) {
            return inputValueTransform.transformValue(coercedValue, inputValueTree);
        }
        if (isNonNull(type)) {
            return transformValue(coercedValue, name, unwrapOne(type), inputValueTree.unwrapOne(), inputValueTransform);
        }
        if (type instanceof GraphQLScalarType) {
            return inputValueTransform.transformValue(coercedValue, inputValueTree);
        }
        if (type instanceof GraphQLEnumType) {
            return inputValueTransform.transformValue(coercedValue, inputValueTree);
        }
        if (isList(type)) {
            assertTrue(coercedValue instanceof Iterable, () -> "The value MUST be an Iterable");
            Object newValue = transformListValue((Iterable) coercedValue, name, unwrapOne(type), inputValueTree.unwrapOne(), inputValueTransform);
            return inputValueTransform.transformValue(newValue, inputValueTree);
        }
        if (type instanceof GraphQLInputObjectType) {
            assertTrue(coercedValue instanceof Map, () -> "The value MUST be an Map");
            Object newValue = transformObjectValue((Map<String, Object>) coercedValue, (GraphQLInputObjectType) type, inputValueTree, inputValueTransform);
            return inputValueTransform.transformValue(newValue, inputValueTree);
        }
        return assertShouldNeverHappen("Have we missed a type case?");
    }

    private static Object transformObjectValue(Map<String, Object> coercedMap, GraphQLInputObjectType inputObjectType, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        for (GraphQLInputObjectField inputFieldDef : inputObjectType.getFieldDefinitions()) {
            String fieldName = inputFieldDef.getName();
            GraphQLInputType fieldType = inputFieldDef.getType();
            InputValueTree newInputValueTree = new InputValueTree(inputValueTree, fieldName, fieldType, inputFieldDef);
            if (coercedMap.containsKey(fieldName)) {
                Object coercedValue = coercedMap.get(fieldName);
                Object newValue = transformValue(coercedValue, fieldName, fieldType, newInputValueTree, inputValueTransform);
                newMap.put(fieldName, newValue);
            }
        }
        return newMap;
    }

    private static Object transformListValue(Iterable coercedIterable, String name, GraphQLInputType unwrappedListType, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        List<Object> newList = new ArrayList<>();
        for (Object value : coercedIterable) {
            Object newValue = transformValue(value, name, unwrappedListType, inputValueTree.unwrapOne(), inputValueTransform);
            newList.add(newValue);
        }
        return newList;
    }

    static GraphQLInputType unwrapOne(GraphQLInputType type) {
        return (GraphQLInputType) GraphQLTypeUtil.unwrapOne(type);
    }
}
