package graphql.nadel.engine.compiler;

import graphql.execution.directives.QueryAppliedDirective;
import graphql.execution.directives.QueryAppliedDirectiveArgument;
import graphql.execution.directives.QueryDirectives;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.Value;
import graphql.normalized.NormalizedInputValue;
import graphql.normalized.VariableAccumulator;
import graphql.normalized.VariableValueWithDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.collect.ImmutableKit.emptyMap;
import static graphql.collect.ImmutableKit.map;
import static graphql.language.Argument.newArgument;

/**
 * Verbatim copy of graphql-java's package-private graphql.normalized.ArgumentMaker (pinned version
 * 0.0.0-2026-03-18T00-52-57-e53ab1a), re-homed into Nadel so {@link NadelExecutableNormalizedOperationToAstCompiler}
 * can reach it from this package. Keep in sync when bumping graphql-java.
 */
class NadelArgumentMaker {

    static List<Argument> createArguments(NadelExecutableNormalizedField executableNormalizedField,
                                          VariableAccumulator variableAccumulator) {
        List<Argument> result = new ArrayList<>();
        Map<String, NormalizedInputValue> normalizedArguments = executableNormalizedField.getNormalizedArguments();
        for (String argName : normalizedArguments.keySet()) {
            NormalizedInputValue normalizedInputValue = normalizedArguments.get(argName);
            Value<?> value = argValue(executableNormalizedField, null, argName, normalizedInputValue, variableAccumulator);
            Argument argument = newArgument()
                    .name(argName)
                    .value(value)
                    .build();
            result.add(argument);
        }
        return result;
    }

    static List<Argument> createDirectiveArguments(NadelExecutableNormalizedField executableNormalizedField,
                                                   QueryDirectives queryDirectives,
                                                   QueryAppliedDirective queryAppliedDirective,
                                                   VariableAccumulator variableAccumulator) {

        Map<String, NormalizedInputValue> argValueMap = queryDirectives.getNormalizedInputValueByImmediateAppliedDirectives().getOrDefault(queryAppliedDirective, emptyMap());

        List<Argument> result = new ArrayList<>();
        for (QueryAppliedDirectiveArgument directiveArgument : queryAppliedDirective.getArguments()) {
            String argName = directiveArgument.getName();
            if (argValueMap != null && argValueMap.containsKey(argName)) {
                NormalizedInputValue normalizedInputValue = argValueMap.get(argName);
                Value<?> value = argValue(executableNormalizedField, queryAppliedDirective, argName, normalizedInputValue, variableAccumulator);
                Argument argument = newArgument()
                        .name(argName)
                        .value(value)
                        .build();
                result.add(argument);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Value<?> argValue(NadelExecutableNormalizedField executableNormalizedField,
                                     QueryAppliedDirective queryAppliedDirective,
                                     String argName,
                                     @Nullable Object value,
                                     VariableAccumulator variableAccumulator) {
        if (value instanceof List) {
            ArrayValue.Builder arrayValue = ArrayValue.newArrayValue();
            arrayValue.values(map((List<Object>) value, val -> argValue(executableNormalizedField, queryAppliedDirective, argName, val, variableAccumulator)));
            return arrayValue.build();
        }
        if (value instanceof Map) {
            ObjectValue.Builder objectValue = ObjectValue.newObjectValue();
            Map<String, Object> map = (Map<String, Object>) value;
            for (String fieldName : map.keySet()) {
                Value<?> fieldValue = argValue(executableNormalizedField, queryAppliedDirective, argName, (NormalizedInputValue) map.get(fieldName), variableAccumulator);
                objectValue.objectField(ObjectField.newObjectField().name(fieldName).value(fieldValue).build());
            }
            return objectValue.build();
        }
        if (value == null) {
            return NullValue.newNullValue().build();
        }
        return (Value<?>) value;
    }

    @NonNull
    private static Value<?> argValue(NadelExecutableNormalizedField executableNormalizedField,
                                     QueryAppliedDirective queryAppliedDirective,
                                     String argName,
                                     NormalizedInputValue normalizedInputValue,
                                     VariableAccumulator variableAccumulator) {
        // graphql-java's VariableAccumulator/VariablePredicate SPI is keyed on graphql-java's
        // ExecutableNormalizedField, which we no longer have here. Nadel's own predicates
        // (see DocumentPredicates) ignore the field argument, so passing null is safe; the alternative
        // is to also fork VariableAccumulator + VariablePredicate + ValueToVariableValueCompiler.
        if (variableAccumulator.shouldMakeVariable(null, queryAppliedDirective, argName, normalizedInputValue)) {
            VariableValueWithDefinition variableWithDefinition = variableAccumulator.accumulateVariable(normalizedInputValue);
            return variableWithDefinition.getVariableReference();
        } else {
            return argValue(executableNormalizedField, queryAppliedDirective, argName, normalizedInputValue.getValue(), variableAccumulator);
        }
    }
}
