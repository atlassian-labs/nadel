package graphql.nadel.compat;

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
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler.CompilerResult;
import graphql.normalized.NormalizedInputValue;
import graphql.normalized.VariableAccumulator;
import graphql.normalized.VariablePredicate;
import graphql.normalized.VariableValueWithDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.collect.ImmutableKit.map;
import static graphql.language.Argument.newArgument;
import static graphql.language.Field.newField;
import static graphql.language.InlineFragment.newInlineFragment;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

@Internal
public class ExecutableNormalizedOperationToAstCompiler {
    public static CompilerResult compileToDocument(
        GraphQLSchema schema,
        OperationDefinition.Operation operationKind,
        String operationName,
        List<ExecutableNormalizedField> topLevelFields,
        VariablePredicate variablePredicate
    ) {
        VariableAccumulator variableAccumulator = new VariableAccumulator(variablePredicate);
        List<Selection<?>> selections = selectionsForNormalizedFields(schema, topLevelFields, variableAccumulator);
        SelectionSet selectionSet = new SelectionSet(selections);


        OperationDefinition.Builder definitionBuilder = OperationDefinition.newOperationDefinition()
            .name(operationName)
            .operation(operationKind)
            .selectionSet(selectionSet);

        definitionBuilder.variableDefinitions(variableAccumulator.getVariableDefinitions());

        return new CompilerResult(
            Document.newDocument()
                .definition(definitionBuilder.build())
                .build(),
            variableAccumulator.getVariablesMap()
        );
    }

    private static List<Selection<?>> selectionsForNormalizedFields(
        GraphQLSchema schema,
        List<ExecutableNormalizedField> executableNormalizedFields,
        VariableAccumulator variableAccumulator
    ) {
        ImmutableList.Builder<Selection<?>> selections = ImmutableList.builder();

        // All conditional fields go here instead of directly to selections so they can be grouped together
        // in the same inline fragement in the output
        Map<String, List<Field>> conditionalFieldsByObjectTypeName = new LinkedHashMap<>();

        for (ExecutableNormalizedField nf : executableNormalizedFields) {
            Map<String, List<Field>> groupFieldsForChild = selectionForNormalizedField(schema, nf, variableAccumulator);
            if (isConditionalCompat(nf, schema)) {
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
        VariableAccumulator variableAccumulator
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

    private static List<Argument> createArguments(ExecutableNormalizedField executableNormalizedField, VariableAccumulator variableAccumulator) {
        ImmutableList.Builder<Argument> result = ImmutableList.builder();
        ImmutableMap<String, NormalizedInputValue> normalizedArguments = executableNormalizedField.getNormalizedArguments();
        for (String argName : normalizedArguments.keySet()) {
            NormalizedInputValue normalizedInputValue = normalizedArguments.get(argName);
            Value<?> value = argValue(executableNormalizedField, argName, normalizedInputValue, variableAccumulator);
            Argument argument = newArgument()
                .name(argName)
                .value(value)
                .build();
            result.add(argument);
        }
        return result.build();
    }

    @SuppressWarnings("unchecked")
    private static Value<?> argValue(ExecutableNormalizedField executableNormalizedField, String argName, @Nullable Object value, VariableAccumulator variableAccumulator) {
        if (value instanceof List) {
            ArrayValue.Builder arrayValue = ArrayValue.newArrayValue();
            arrayValue.values(map((List<Object>) value, val -> argValue(executableNormalizedField, argName, val, variableAccumulator)));
            return arrayValue.build();
        }
        if (value instanceof Map) {
            ObjectValue.Builder objectValue = ObjectValue.newObjectValue();
            Map<String, Object> map = (Map<String, Object>) value;
            for (String fieldName : map.keySet()) {
                Value<?> fieldValue = argValue(executableNormalizedField, argName, (NormalizedInputValue) map.get(fieldName), variableAccumulator);
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
    private static Value<?> argValue(ExecutableNormalizedField executableNormalizedField, String argName, NormalizedInputValue normalizedInputValue, VariableAccumulator variableAccumulator) {
        if (variableAccumulator.shouldMakeVariable(executableNormalizedField, argName, normalizedInputValue)) {
            VariableValueWithDefinition variableWithDefinition = variableAccumulator.accumulateVariable(normalizedInputValue);
            return variableWithDefinition.getVariableReference();
        } else {
            return argValue(executableNormalizedField, argName, normalizedInputValue.getValue(), variableAccumulator);
        }
    }

    private static boolean isConditionalCompat(ExecutableNormalizedField nf, GraphQLSchema schema) {
        if (nf.getParent() == null) {
            return false;
        }
        return nf.getObjectTypeNames().size() > 1 || unwrapAll(nf.getParent().getType(schema)) != getOneObjectTypeCompat(nf, schema);
    }

    public static GraphQLObjectType getOneObjectTypeCompat(ExecutableNormalizedField nf, GraphQLSchema schema) {
        return (GraphQLObjectType) schema.getType(nf.getObjectTypeNames().iterator().next());
    }
}
