package graphql.nadel.normalized;

import graphql.Internal;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.nadel.normalized.ValueToVariableValueCompiler.VariableValueWithDefinition;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;
import graphql.schema.GraphQLSchema;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.collect.ImmutableKit.map;
import static graphql.language.Argument.newArgument;
import static graphql.language.Field.newField;
import static graphql.language.InlineFragment.newInlineFragment;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.normalized.ValueToVariableValueCompiler.normalizedInputValueToVariable;

@Internal
public class ExecutableNormalizedOperationToAstCompiler {

    private static final String JSON_SCALAR_TYPENAME = "JSON";

    public static Pair<Document, Map<String, Object>> compileToDocument(
            GraphQLSchema schema,
            OperationDefinition.Operation operationKind,
            String operationName,
            List<ExecutableNormalizedField> topLevelFields
    ) {
        List<VariableValueWithDefinition> variables = new ArrayList<>();
        List<Selection<?>> selections = selectionsForNormalizedFields(schema, topLevelFields, variables);
        SelectionSet selectionSet = new SelectionSet(selections);

        OperationDefinition.Builder definitionBuilder = OperationDefinition.newOperationDefinition()
                .name(operationName)
                .operation(operationKind)
                .selectionSet(selectionSet);

        definitionBuilder.variableDefinitions(map(variables, (variableWithDefinition -> variableWithDefinition.definition)));

        Map<String, Object> variableValuesByNames = variables.stream()
                .collect(Collectors.toMap(
                        variableWithDefinition -> variableWithDefinition.definition.getName(),
                        variableWithDefinition -> variableWithDefinition.value
                ));

        return new Pair<>(
                Document.newDocument()
                        .definition(definitionBuilder.build())
                        .build(),
                variableValuesByNames
        );
    }

    private static List<Selection<?>> selectionsForNormalizedFields(
            GraphQLSchema schema,
            List<ExecutableNormalizedField> executableNormalizedFields,
            List<VariableValueWithDefinition> variables
    ) {
        ImmutableList.Builder<Selection<?>> selections = ImmutableList.builder();

        // All conditional fields go here instead of directly to selections so they can be grouped together
        // in the same inline fragement in the output
        Map<String, List<Field>> conditionalFieldsByObjectTypeName = new LinkedHashMap<>();

        for (ExecutableNormalizedField nf : executableNormalizedFields) {
            Map<String, List<Field>> groupFieldsForChild = selectionForNormalizedField(schema, nf, variables);
            if (nf.isConditional(schema)) {
                groupFieldsForChild.forEach((objectTypeName, fields) -> {
                    List<Field> fieldList = conditionalFieldsByObjectTypeName.computeIfAbsent(objectTypeName, ignored -> new ArrayList<>());
                    fieldList.addAll(fields);
                });
            } else {
                List<Field> fields = groupFieldsForChild.values().iterator().next();
                selections.addAll(fields);
            }
        }

        conditionalFieldsByObjectTypeName.forEach((objectTypeName, fields) -> {
            TypeName typeName = newTypeName(objectTypeName).build();
            InlineFragment inlineFragment = newInlineFragment().
                    typeCondition(typeName)
                    .selectionSet(selectionSet(fields))
                    .build();
            selections.add(inlineFragment);
        });

        return selections.build();
    }

    private static Map<String, List<Field>> selectionForNormalizedField(
            GraphQLSchema schema,
            ExecutableNormalizedField executableNormalizedField,
            List<VariableValueWithDefinition> variableAccumulator
    ) {
        Map<String, List<Field>> groupedFields = new LinkedHashMap<>();
        for (String objectTypeName : executableNormalizedField.getObjectTypeNames()) {
            List<Selection<?>> subSelections = selectionsForNormalizedFields(schema, executableNormalizedField.getChildren(), variableAccumulator);
            SelectionSet selectionSet = null;
            if (subSelections.size() > 0) {
                selectionSet = newSelectionSet()
                        .selections(subSelections)
                        .build();
            }
            List<Argument> arguments = createArguments(executableNormalizedField, variableAccumulator);
            Field field = newField()
                    .name(executableNormalizedField.getFieldName())
                    .alias(executableNormalizedField.getAlias())
                    .selectionSet(selectionSet)
                    .arguments(arguments)
                    .build();

            groupedFields.computeIfAbsent(objectTypeName, ignored -> new ArrayList<>()).add(field);
        }
        return groupedFields;
    }

    private static SelectionSet selectionSet(List<Field> fields) {
        return newSelectionSet().selections(fields).build();
    }

    private static List<Argument> createArguments(ExecutableNormalizedField executableNormalizedField, List<VariableValueWithDefinition> variables) {
        ImmutableList.Builder<Argument> result = ImmutableList.builder();
        ImmutableMap<String, NormalizedInputValue> normalizedArguments = executableNormalizedField.getNormalizedArguments();
        for (String argName : normalizedArguments.keySet()) {
            NormalizedInputValue normalizedInputValue = normalizedArguments.get(argName);
            Value<?> value = argValue(normalizedInputValue, variables);
            Argument argument = newArgument()
                    .name(argName)
                    .value(value)
                    .build();
            result.add(argument);
        }
        return result.build();
    }

    private static Value<?> argValue(Object value, List<VariableValueWithDefinition> variables) {
        if (value instanceof List) {
            ArrayValue.Builder arrayValue = ArrayValue.newArrayValue();
            arrayValue.values(map((List<Object>) value, val -> argValue(val, variables)));
            return arrayValue.build();
        }
        if (value instanceof Map) {
            ObjectValue.Builder objectValue = ObjectValue.newObjectValue();
            Map<String, Object> map = (Map<String, Object>) value;
            for (String fieldName : map.keySet()) {
                Value<?> fieldValue = argValue((NormalizedInputValue) map.get(fieldName), variables);
                objectValue.objectField(ObjectField.newObjectField().name(fieldName).value(fieldValue).build());
            }
            return objectValue.build();
        }
        if (value == null) {
            return NullValue.newNullValue().build();
        }
        return (Value<?>) value;
    }

    @NotNull
    private static Value<?> argValue(NormalizedInputValue normalizedInputValue, List<VariableValueWithDefinition> variableDefinitionAccumulator) {
        if (JSON_SCALAR_TYPENAME.equals(normalizedInputValue.getUnwrappedTypeName())) {
            VariableValueWithDefinition variableWithDefinition = normalizedInputValueToVariable(normalizedInputValue);
            variableDefinitionAccumulator.add(variableWithDefinition);
            return variableWithDefinition.variableReference;
        } else {
            return argValue(normalizedInputValue.getValue(), variableDefinitionAccumulator);
        }
    }
}
