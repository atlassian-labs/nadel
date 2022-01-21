package graphql.nadel.normalized;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.normalized.NormalizedInputValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ValueToVariableValueCompiler {

    static VariableValueWithDefinition normalizedInputValueToVariable(NormalizedInputValue normalizedInputValue) {
        Object variableValue = null;
        Object normalizedInputValueValue = normalizedInputValue.getValue();
        if (normalizedInputValueValue instanceof ObjectValue) {
            variableValue = toVariableValue((ObjectValue) normalizedInputValueValue);
        }
        if (normalizedInputValueValue instanceof ArrayValue) {
            variableValue = toVariableValues(((ArrayValue) normalizedInputValueValue).getValues());
        }
        if (normalizedInputValueValue instanceof List) {
            variableValue = toVariableValues((List) normalizedInputValueValue);
        }
        String varName = getVarName();
        return new VariableValueWithDefinition(
                variableValue,
                VariableDefinition.newVariableDefinition()
                        .name(varName)
                        .type(TypeName.newTypeName(normalizedInputValue.getTypeName()).build())
                        .build(),
                VariableReference.newVariableReference().name(varName).build());
    }

    @NotNull
    private static List<Object> toVariableValues(List<Value> values) {
        ArrayList<Object> objects = new ArrayList<>();
        for (Value value : values) {
            objects.add(toVariableValue(value));
        }
        return objects;
    }

    private static Map<String, Object> toVariableValue(ObjectValue objectValue) {
        HashMap<String, Object> map = new HashMap<>();
        List<ObjectField> objectFields = objectValue.getObjectFields();
        for (ObjectField objectField : objectFields) {
            String objectFieldName = objectField.getName();
            Value<?> objectFieldValue = objectField.getValue();
            if (objectFieldValue instanceof ArrayValue) {
                List<Object> objects = toVariableValues(((ArrayValue) objectFieldValue).getValues());
                map.put(objectFieldName, objects);
                continue;
            }
            map.put(objectFieldName, toVariableValue(objectFieldValue));
        }
        return map;
    }

    private static Object toVariableValue(Value value) {
        if (value instanceof ObjectValue) {
            return toVariableValue((ObjectValue) value);
        }
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue();
        }
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue();
        }
        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        }
        return null; //todo
    }

    private static String getVarName() {
        return "var_" + UUID.randomUUID().toString().replace("-", "_");
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
