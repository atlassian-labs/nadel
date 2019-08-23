package graphql.nadel.engine.transformation.variables;

import graphql.PublicApi;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLInputValueDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.transformation.variables.InputValueTransformer.unwrapOne;
import static graphql.schema.GraphQLTypeUtil.isList;
import static graphql.schema.GraphQLTypeUtil.isNonNull;


/**
 * This class can navigate a series of arguments and find values within them, descending down the input type
 * tree that naturally forms around input types.  It only follows type of the map of argument values actually
 * contains a value for that argument or input type field
 */
@PublicApi
public class InputValueFinder {

    /**
     * Builds a list of found values by running the input type hierarchy on a list of input value definitions
     * namely GraphQLArgument or GraphQLInputObjectField objects.
     *
     * @param valueDefinitions       the list of input definitions to search
     * @param coercedArgs            the coerced args for this list of input defs
     * @param inputValueFindFunction the find function
     * @param <T>                    for two
     * @return the list of found values
     */
    public static <T> List<T> find(List<? extends GraphQLInputValueDefinition> valueDefinitions, Map<String, Object> coercedArgs, InputValueFindFunction<T> inputValueFindFunction) {
        List<T> foundValues = new ArrayList<>();
        for (GraphQLInputValueDefinition valueDefinition : valueDefinitions) {
            String argumentName = valueDefinition.getName();
            if (coercedArgs.containsKey(argumentName)) {
                GraphQLInputType inputType = valueDefinition.getType();
                InputValueTree inputValueTree = new InputValueTree(null, argumentName, inputType, valueDefinition);
                Object coercedValue = coercedArgs.get(argumentName);
                findImpl(foundValues, inputType, coercedValue, inputValueTree, inputValueFindFunction);
            }
        }
        return foundValues;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static <T> void findImpl(List<T> foundValues, GraphQLInputType type, Object coercedValue, InputValueTree inputValueTree, InputValueFindFunction<T> inputValueFindFunction) {
        //
        // handle null values special since we cant descend on them should they be map / list types
        if (coercedValue == null) {
            Optional<T> result = inputValueFindFunction.apply(coercedValue, inputValueTree);
            result.ifPresent(foundValues::add);
        } else if (isNonNull(type)) {
            findImpl(foundValues, unwrapOne(type), coercedValue, inputValueTree.unwrapOne(), inputValueFindFunction);
        } else if (isList(type)) {
            assertTrue(coercedValue instanceof Iterable, "The value MUST be an Iterable");
            findListValue(foundValues, unwrapOne(type), (Iterable) coercedValue, inputValueTree.unwrapOne(), inputValueFindFunction);
        } else if (type instanceof GraphQLInputObjectType) {
            assertTrue(coercedValue instanceof Map, "The value MUST be an Map");
            Map<String, Object> coercedMap = (Map<String, Object>) coercedValue;
            findObjectValue(foundValues, (GraphQLInputObjectType) type, coercedMap, inputValueTree, inputValueFindFunction);
        } else {
            Optional<T> result = inputValueFindFunction.apply(coercedValue, inputValueTree);
            result.ifPresent(foundValues::add);
        }
    }

    private static <T> void findListValue(List<T> foundValues, GraphQLInputType unwrappedListType, Iterable coercedIterable, InputValueTree inputValueTree, InputValueFindFunction<T> inputValueFindFunction) {
        for (Object coercedValue : coercedIterable) {
            findImpl(foundValues, unwrappedListType, coercedValue, inputValueTree.unwrapOne(), inputValueFindFunction);
        }
    }

    private static <T> void findObjectValue(List<T> foundValues, GraphQLInputObjectType inputObjectType, Map<String, Object> coercedMap, InputValueTree inputValueTree, InputValueFindFunction<T> inputValueFindFunction) {
        for (GraphQLInputObjectField inputFieldDef : inputObjectType.getFieldDefinitions()) {
            String fieldName = inputFieldDef.getName();
            if (coercedMap.containsKey(fieldName)) {
                GraphQLInputType fieldType = inputFieldDef.getType();
                InputValueTree newInputValueTree = new InputValueTree(inputValueTree, fieldName, fieldType, inputFieldDef);
                Object coercedValue = coercedMap.get(fieldName);
                findImpl(foundValues, fieldType, coercedValue, newInputValueTree, inputValueFindFunction);
            }
        }
    }
}
