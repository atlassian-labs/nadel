package graphql.nadel.normalized;

import graphql.AssertException;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.normalized.NormalizedInputValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.nadel.util.FpKit.map;

public class ValueToVariableValueCompiler {

    static VariableValueWithDefinition normalizedInputValueToVariable(NormalizedInputValue normalizedInputValue, int queryVariableCount) {
        Object variableValue;
        Object inputValue = normalizedInputValue.getValue();
        if (inputValue instanceof Value) {
            variableValue = toVariableValue((Value) inputValue);
        } else if (inputValue instanceof List) {
            variableValue = toVariableValues((List) inputValue);
        } else {
            throw new AssertException("Should never happen. Did not expect NormalizedInputValue.getValue() of type: " + inputValue.getClass());
        }
        String varName = getVarName(queryVariableCount);
        return new VariableValueWithDefinition(
                variableValue,
                VariableDefinition.newVariableDefinition()
                        .name(varName)
                        .type(TypeName.newTypeName(normalizedInputValue.getTypeName()).build())
                        .build(),
                VariableReference.newVariableReference().name(varName).build());
    }


    private static Map<String, Object> toVariableValue(ObjectValue objectValue) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<ObjectField> objectFields = objectValue.getObjectFields();
        for (ObjectField objectField : objectFields) {
            String objectFieldName = objectField.getName();
            Value<?> objectFieldValue = objectField.getValue();
            map.put(objectFieldName, toVariableValue(objectFieldValue));
        }
        return map;
    }

    @NotNull
    private static List<Object> toVariableValues(List<Value> arrayValues) {
        return map(arrayValues, ValueToVariableValueCompiler::toVariableValue);
    }

    @Nullable
    private static Object toVariableValue(Value<?> value) {
        if (value instanceof ObjectValue) {
            return toVariableValue((ObjectValue) value);
        } else if (value instanceof ArrayValue) {
            return toVariableValues(((ArrayValue) value).getValues());
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        } else if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        } else if (value instanceof NullValue) {
            return null;
        }
        throw new AssertException("Should never happen. Cannot handle JSON node of type: " + value.getClass());
    }

    private static String getVarName(int variableOrdinal) {
        return "var_" + variableOrdinal;
    }

    static class VariableValueWithDefinition {
        final Object value;
        final VariableDefinition definition;
        final VariableReference variableReference;

        public VariableValueWithDefinition(Object value, VariableDefinition definition, VariableReference variableReference) {
            this.value = value;
            this.definition = definition;
            this.variableReference = variableReference;
        }
    }

}
