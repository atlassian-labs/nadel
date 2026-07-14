package graphql.nadel.engine.compiler;

import graphql.Assert;
import graphql.Directives;
import graphql.execution.directives.QueryAppliedDirective;
import graphql.execution.directives.QueryDirectives;
import graphql.introspection.Introspection;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.ExecutableNormalizedOperationToAstCompiler.CompilerResult;
import graphql.normalized.VariableAccumulator;
import graphql.normalized.VariablePredicate;
import graphql.normalized.incremental.NormalizedDeferredExecution;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.util.LinkedHashMapFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static graphql.collect.ImmutableKit.emptyList;
import static graphql.language.Argument.newArgument;
import static graphql.language.Field.newField;
import static graphql.language.InlineFragment.newInlineFragment;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

/**
 * Nadel fork of graphql-java's {@code graphql.normalized.ExecutableNormalizedOperationToAstCompiler}
 * (pinned version 0.0.0-2026-03-18T00-52-57-e53ab1a). It is a verbatim copy of the upstream logic with a
 * single behavioural addition: any {@link ExecutableNormalizedField} in {@code forcePrintBareFields} is emitted
 * bare (no {@code ... on Type} inline fragment) even though its {@code objectTypeNames} is a strict subset of
 * its parent's possible types. This lets Nadel keep {@code objectTypeNames} honest while still sending an
 * interface/union-level selection bare to the underlying service (GQLGW-6377). With an empty
 * {@code forcePrintBareFields} the output is byte-identical to upstream (see NadelAstCompilerDifferentialTest).
 * The one package-private helper it needs, {@link NadelArgumentMaker}, is copied alongside it.
 *
 * <p>Membership is by object identity ({@link ExecutableNormalizedField} does not override equals/hashCode);
 * callers must pass the exact instances that reach this compiler (the rebuilt underlying tree).
 *
 * <p>Keep in sync when bumping graphql-java.
 */
public class NadelExecutableNormalizedOperationToAstCompiler {

    public static CompilerResult compileToDocument(@NonNull GraphQLSchema schema,
                                                   OperationDefinition.@NonNull Operation operationKind,
                                                   @Nullable String operationName,
                                                   @NonNull List<ExecutableNormalizedField> topLevelFields,
                                                   @Nullable VariablePredicate variablePredicate,
                                                   @NonNull Set<ExecutableNormalizedField> forcePrintBareFields) {
        return compileToDocument(schema, operationKind, operationName, topLevelFields, LinkedHashMapFactory.of(), variablePredicate, forcePrintBareFields, false);
    }

    public static CompilerResult compileToDocumentWithDeferSupport(@NonNull GraphQLSchema schema,
                                                                   OperationDefinition.@NonNull Operation operationKind,
                                                                   @Nullable String operationName,
                                                                   @NonNull List<ExecutableNormalizedField> topLevelFields,
                                                                   @Nullable VariablePredicate variablePredicate,
                                                                   @NonNull Set<ExecutableNormalizedField> forcePrintBareFields) {
        return compileToDocument(schema, operationKind, operationName, topLevelFields, LinkedHashMapFactory.of(), variablePredicate, forcePrintBareFields, true);
    }

    private static CompilerResult compileToDocument(@NonNull GraphQLSchema schema,
                                                    OperationDefinition.@NonNull Operation operationKind,
                                                    @Nullable String operationName,
                                                    @NonNull List<ExecutableNormalizedField> topLevelFields,
                                                    @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                    @Nullable VariablePredicate variablePredicate,
                                                    @NonNull Set<ExecutableNormalizedField> forcePrintBareFields,
                                                    boolean deferSupport) {
        GraphQLObjectType operationType = getOperationType(schema, operationKind);

        VariableAccumulator variableAccumulator = new VariableAccumulator(variablePredicate);
        List<Selection<?>> selections = subselectionsForNormalizedField(schema, operationType.getName(), topLevelFields, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, deferSupport);
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

    private static List<Selection<?>> subselectionsForNormalizedField(GraphQLSchema schema,
                                                                      @NonNull String parentOutputType,
                                                                      List<ExecutableNormalizedField> executableNormalizedFields,
                                                                      @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                      VariableAccumulator variableAccumulator,
                                                                      @NonNull Set<ExecutableNormalizedField> forcePrintBareFields,
                                                                      boolean deferSupport) {
        if (deferSupport) {
            return subselectionsForNormalizedFieldWithDeferSupport(schema, parentOutputType, executableNormalizedFields, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields);
        } else {
            return subselectionsForNormalizedFieldNoDeferSupport(schema, parentOutputType, executableNormalizedFields, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields);
        }
    }

    private static List<Selection<?>> subselectionsForNormalizedFieldNoDeferSupport(GraphQLSchema schema,
                                                                                    @NonNull String parentOutputType,
                                                                                    List<ExecutableNormalizedField> executableNormalizedFields,
                                                                                    @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                                    VariableAccumulator variableAccumulator,
                                                                                    @NonNull Set<ExecutableNormalizedField> forcePrintBareFields) {
        List<Selection<?>> selections = new ArrayList<>();

        // All conditional fields go here instead of directly to selections, so they can be grouped together
        // in the same inline fragment in the output
        Map<String, List<Field>> fieldsByTypeCondition = new LinkedHashMap<>();

        for (ExecutableNormalizedField nf : executableNormalizedFields) {
            // Nadel: forcePrintBareFields are emitted bare even when isConditional would wrap them in a fragment.
            if (nf.isConditional(schema) && !forcePrintBareFields.contains(nf)) {
                selectionForNormalizedField(schema, nf, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, false)
                        .forEach((objectTypeName, field) ->
                                fieldsByTypeCondition
                                        .computeIfAbsent(objectTypeName, ignored -> new ArrayList<>())
                                        .add(field));
            } else {
                selections.add(selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, false));
            }
        }

        fieldsByTypeCondition.forEach((objectTypeName, fields) -> {
            TypeName typeName = newTypeName(objectTypeName).build();
            InlineFragment inlineFragment = newInlineFragment()
                    .typeCondition(typeName)
                    .selectionSet(selectionSet(fields))
                    .build();
            selections.add(inlineFragment);
        });

        return selections;
    }


    private static List<Selection<?>> subselectionsForNormalizedFieldWithDeferSupport(GraphQLSchema schema,
                                                                                      @NonNull String parentOutputType,
                                                                                      List<ExecutableNormalizedField> executableNormalizedFields,
                                                                                      @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                                      VariableAccumulator variableAccumulator,
                                                                                      @NonNull Set<ExecutableNormalizedField> forcePrintBareFields) {
        List<Selection<?>> selections = new ArrayList<>();

        // All conditional and deferred fields go here instead of directly to selections, so they can be grouped together
        // in the same inline fragment in the output
        //
        Map<ExecutionFragmentDetails, List<Field>> fieldsByFragmentDetails = new LinkedHashMap<>();

        for (ExecutableNormalizedField nf : executableNormalizedFields) {
            LinkedHashSet<NormalizedDeferredExecution> deferredExecutions = nf.getDeferredExecutions();

            // Nadel: forcePrintBareFields are emitted bare even when isConditional would wrap them in a fragment.
            if (nf.isConditional(schema) && !forcePrintBareFields.contains(nf)) {
                selectionForNormalizedField(schema, nf, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, true)
                        .forEach((objectTypeName, field) -> {
                            if (deferredExecutions == null || deferredExecutions.isEmpty()) {
                                fieldsByFragmentDetails
                                        .computeIfAbsent(new ExecutionFragmentDetails(objectTypeName, null), ignored -> new ArrayList<>())
                                        .add(field);
                            } else {
                                deferredExecutions.forEach(deferredExecution -> {
                                    fieldsByFragmentDetails
                                            .computeIfAbsent(new ExecutionFragmentDetails(objectTypeName, deferredExecution), ignored -> new ArrayList<>())
                                            .add(field);
                                });
                            }
                        });

            } else if (deferredExecutions != null && !deferredExecutions.isEmpty()) {
                Field field = selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, true);

                deferredExecutions.forEach(deferredExecution -> {
                    fieldsByFragmentDetails
                            .computeIfAbsent(new ExecutionFragmentDetails(null, deferredExecution), ignored -> new ArrayList<>())
                            .add(field);
                });
            } else {
                selections.add(selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, true));
            }
        }

        fieldsByFragmentDetails.forEach((typeAndDeferPair, fields) -> {
            InlineFragment.Builder fragmentBuilder = newInlineFragment()
                    .selectionSet(selectionSet(fields));

            if (typeAndDeferPair.typeName != null) {
                TypeName typeName = newTypeName(typeAndDeferPair.typeName).build();
                fragmentBuilder.typeCondition(typeName);
            }

            if (typeAndDeferPair.deferredExecution != null) {
                Directive.Builder deferBuilder = Directive.newDirective().name(Directives.DeferDirective.getName());

                if (typeAndDeferPair.deferredExecution.getLabel() != null) {
                    deferBuilder.argument(newArgument().name("label").value(StringValue.of(typeAndDeferPair.deferredExecution.getLabel())).build());
                }

                fragmentBuilder.directive(deferBuilder.build());
            }


            selections.add(fragmentBuilder.build());
        });

        return selections;
    }

    /**
     * @return Map of object type names to list of fields
     */
    private static Map<String, Field> selectionForNormalizedField(GraphQLSchema schema,
                                                                  ExecutableNormalizedField executableNormalizedField,
                                                                  @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                  VariableAccumulator variableAccumulator,
                                                                  @NonNull Set<ExecutableNormalizedField> forcePrintBareFields,
                                                                  boolean deferSupport) {
        Map<String, Field> groupedFields = new LinkedHashMap<>();

        for (String objectTypeName : executableNormalizedField.getObjectTypeNames()) {
            groupedFields.put(objectTypeName, selectionForNormalizedField(schema, objectTypeName, executableNormalizedField, normalizedFieldToQueryDirectives, variableAccumulator, forcePrintBareFields, deferSupport));
        }

        return groupedFields;
    }

    /**
     * @return a single {@link Field} resolved in the context of the given object type
     */
    private static Field selectionForNormalizedField(GraphQLSchema schema,
                                                     String objectTypeName,
                                                     ExecutableNormalizedField executableNormalizedField,
                                                     @NonNull Map<ExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                     VariableAccumulator variableAccumulator,
                                                     @NonNull Set<ExecutableNormalizedField> forcePrintBareFields,
                                                     boolean deferSupport) {
        final List<Selection<?>> subSelections;
        if (executableNormalizedField.getChildren().isEmpty()) {
            subSelections = emptyList();
        } else {
            GraphQLFieldDefinition fieldDef = getFieldDefinition(schema, objectTypeName, executableNormalizedField);
            GraphQLUnmodifiedType fieldOutputType = unwrapAll(fieldDef.getType());

            subSelections = subselectionsForNormalizedField(
                    schema,
                    fieldOutputType.getName(),
                    executableNormalizedField.getChildren(),
                    normalizedFieldToQueryDirectives,
                    variableAccumulator,
                    forcePrintBareFields,
                    deferSupport
            );
        }

        SelectionSet selectionSet = selectionSetOrNullIfEmpty(subSelections);
        List<Argument> arguments = NadelArgumentMaker.createArguments(executableNormalizedField, variableAccumulator);

        QueryDirectives queryDirectives = normalizedFieldToQueryDirectives.get(executableNormalizedField);

        Field.Builder builder = newField()
                .name(executableNormalizedField.getFieldName())
                .alias(executableNormalizedField.getAlias())
                .selectionSet(selectionSet)
                .arguments(arguments);

        List<Directive> directives = buildDirectives(executableNormalizedField, queryDirectives, variableAccumulator);
        return builder
                .directives(directives)
                .build();
    }

    private static @NonNull List<Directive> buildDirectives(ExecutableNormalizedField executableNormalizedField, QueryDirectives queryDirectives, VariableAccumulator variableAccumulator) {
        if (queryDirectives == null || queryDirectives.getImmediateAppliedDirectivesByField().isEmpty()) {
            return emptyList();
        }
        return queryDirectives.getImmediateAppliedDirectivesByField().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(queryAppliedDirective -> buildDirective(executableNormalizedField, queryDirectives, queryAppliedDirective, variableAccumulator))
                .collect(Collectors.toList());
    }

    private static Directive buildDirective(ExecutableNormalizedField executableNormalizedField, QueryDirectives queryDirectives, QueryAppliedDirective queryAppliedDirective, VariableAccumulator variableAccumulator) {

        List<Argument> arguments = NadelArgumentMaker.createDirectiveArguments(executableNormalizedField, queryDirectives, queryAppliedDirective, variableAccumulator);
        return Directive.newDirective()
                .name(queryAppliedDirective.getName())
                .arguments(arguments).build();
    }

    @Nullable
    private static SelectionSet selectionSetOrNullIfEmpty(List<Selection<?>> selections) {
        return selections.isEmpty() ? null : newSelectionSet().selections(selections).build();
    }

    private static SelectionSet selectionSet(List<Field> fields) {
        return newSelectionSet().selections(fields).build();
    }


    @NonNull
    private static GraphQLFieldDefinition getFieldDefinition(GraphQLSchema schema,
                                                             String parentType,
                                                             ExecutableNormalizedField nf) {
        return Introspection.getFieldDef(schema, (GraphQLCompositeType) schema.getType(parentType), nf.getName());
    }


    @Nullable
    private static GraphQLObjectType getOperationType(@NonNull GraphQLSchema schema,
                                                      OperationDefinition.@NonNull Operation operationKind) {
        switch (operationKind) {
            case QUERY:
                return schema.getQueryType();
            case MUTATION:
                return schema.getMutationType();
            case SUBSCRIPTION:
                return schema.getSubscriptionType();
        }

        return Assert.assertShouldNeverHappen("Unknown operation kind " + operationKind);
    }

    /**
     * Represents important execution details that can be associated with a fragment.
     */
    private static class ExecutionFragmentDetails {
        private final String typeName;
        private final NormalizedDeferredExecution deferredExecution;

        public ExecutionFragmentDetails(String typeName, NormalizedDeferredExecution deferredExecution) {
            this.typeName = typeName;
            this.deferredExecution = deferredExecution;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExecutionFragmentDetails that = (ExecutionFragmentDetails) o;
            return Objects.equals(typeName, that.typeName) && Objects.equals(deferredExecution, that.deferredExecution);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName, deferredExecution);
        }
    }
}
