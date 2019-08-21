package graphql.nadel.engine.transformation.variables;

import graphql.Assert;
import graphql.PublicApi;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;

/**
 * This can take a variables map and a list of arguments and possibly transform the values that are present in that variables map
 * following the structure of the argument input types.
 */
@PublicApi
public class VariablesTransformer {

    /**
     * Transforms the variables map in terms of the list of graphql arguments.
     *
     * @param arguments           the list of graphql arguments
     * @param variables           the variables to transform
     * @param inputValueTransform the transformer function
     * @return a possibly transformed map of variables
     */
    public static Map<String, Object> transform(List<GraphQLArgument> arguments, Map<String, Object> variables, InputValueTransform inputValueTransform) {
        return transformImpl(arguments, variables, inputValueTransform);
    }

    private static Map<String, Object> transformImpl(List<GraphQLArgument> arguments, Map<String, Object> variables, InputValueTransform inputValueTransform) {

        Map<String, Object> newVariables = new LinkedHashMap<>(variables);
        for (GraphQLArgument argument : arguments) {
            String argumentName = argument.getName();
            if (variables.containsKey(argumentName)) {
                Object startingValue = variables.get(argumentName);

                InputValueTree inputValueTree = new InputValueTree(null, argumentName, argument.getType(), argument);
                Object newValue = transformValue(startingValue, argumentName, argument.getType(), inputValueTree, inputValueTransform);

                newVariables.put(argumentName, newValue);
            }
        }
        return newVariables;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static Object transformValue(Object startingValue, String name, GraphQLInputType type, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        if (startingValue == null) {
            return inputValueTransform.transformValue(startingValue, inputValueTree);
        }
        if (isNonNull(type)) {
            return transformValue(startingValue, name, unwrapOne(type), inputValueTree.unwrapOne(), inputValueTransform);
        }
        if (isList(type)) {
            Assert.assertTrue(startingValue instanceof Iterable, "The value MUST be an Iterable");
            Object newValue = transformListValue((Iterable) startingValue, name, unwrapOne(type), inputValueTree.unwrapOne(), inputValueTransform);
            return inputValueTransform.transformValue(newValue, inputValueTree);
        }
        if (type instanceof GraphQLInputObjectType) {
            Assert.assertTrue(startingValue instanceof Map, "The value MUST be an Map");
            Object newValue = transformObjectValue((Map<String, Object>) startingValue, (GraphQLInputObjectType) type, inputValueTree, inputValueTransform);
            return inputValueTransform.transformValue(newValue, inputValueTree);
        }
        if (type instanceof GraphQLScalarType) {
            return inputValueTransform.transformValue(startingValue, inputValueTree);
        }
        if (type instanceof GraphQLEnumType) {
            return inputValueTransform.transformValue(startingValue, inputValueTree);
        }
        return Assert.assertShouldNeverHappen("Have we missed a type case?");
    }

    private static Object transformObjectValue(Map<String, Object> startingValue, GraphQLInputObjectType inputObjectType, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        for (GraphQLInputObjectField inputFieldDef : inputObjectType.getFieldDefinitions()) {
            String fieldName = inputFieldDef.getName();
            GraphQLInputType fieldType = inputFieldDef.getType();
            InputValueTree newInputValueTree = new InputValueTree(inputValueTree, fieldName, fieldType, inputFieldDef);
            if (startingValue.containsKey(fieldName)) {
                Object value = startingValue.get(fieldName);
                Object newValue = transformValue(value, fieldName, fieldType, newInputValueTree, inputValueTransform);
                newMap.put(fieldName, newValue);
            }
        }
        return newMap;
    }

    private static Object transformListValue(Iterable startingIterable, String name, GraphQLInputType unwrappedListType, InputValueTree inputValueTree, InputValueTransform inputValueTransform) {
        List<Object> newList = new ArrayList<>();
        for (Object value : startingIterable) {
            Object newValue = transformValue(value, name, unwrappedListType, inputValueTree.unwrapOne(), inputValueTransform);
            newList.add(newValue);
        }
        return newList;
    }

    private static GraphQLInputType unwrapOne(GraphQLInputType type) {
        return (GraphQLInputType) GraphQLTypeUtil.unwrapOne(type);
    }
}
