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
 * (pinned version 0.0.0-2026-03-18T00-52-57-e53ab1a). Verbatim copy of the upstream logic with a single
 * behavioural addition: a {@link NadelExecutableNormalizedField} whose {@link
 * NadelExecutableNormalizedField#isForcePrintAsUnconditional()} flag is set is emitted bare (no {@code ... on Type}
 * inline fragment) even though its {@code objectTypeNames} is a strict subset of its parent's possible types.
 * This lets Nadel keep {@code objectTypeNames} honest while still sending an interface/union-level selection
 * bare to the underlying service (GQLGW-6377).
 *
 * <p>Unlike the external-set variant, the signal lives ON the field itself, so no per-request identity set is
 * threaded through the engine. The trade-off is that the whole {@code ExecutableNormalizedField} type had to be
 * forked (see {@link NadelExecutableNormalizedField}) because graphql-java's has a private constructor.
 *
 * <p>Keep in sync when bumping graphql-java.
 */
public class NadelExecutableNormalizedOperationToAstCompiler {

    public static CompilerResult compileToDocument(@NonNull GraphQLSchema schema,
                                                   OperationDefinition.@NonNull Operation operationKind,
                                                   @Nullable String operationName,
                                                   @NonNull List<NadelExecutableNormalizedField> topLevelFields,
                                                   @Nullable VariablePredicate variablePredicate) {
        return compileToDocument(schema, operationKind, operationName, topLevelFields, LinkedHashMapFactory.of(), variablePredicate, false);
    }

    public static CompilerResult compileToDocumentWithDeferSupport(@NonNull GraphQLSchema schema,
                                                                   OperationDefinition.@NonNull Operation operationKind,
                                                                   @Nullable String operationName,
                                                                   @NonNull List<NadelExecutableNormalizedField> topLevelFields,
                                                                   @Nullable VariablePredicate variablePredicate) {
        return compileToDocument(schema, operationKind, operationName, topLevelFields, LinkedHashMapFactory.of(), variablePredicate, true);
    }

    private static CompilerResult compileToDocument(@NonNull GraphQLSchema schema,
                                                    OperationDefinition.@NonNull Operation operationKind,
                                                    @Nullable String operationName,
                                                    @NonNull List<NadelExecutableNormalizedField> topLevelFields,
                                                    @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                    @Nullable VariablePredicate variablePredicate,
                                                    boolean deferSupport) {
        GraphQLObjectType operationType = getOperationType(schema, operationKind);

        VariableAccumulator variableAccumulator = new VariableAccumulator(variablePredicate);
        List<Selection<?>> selections = subselectionsForNormalizedField(schema, operationType.getName(), topLevelFields, normalizedFieldToQueryDirectives, variableAccumulator, deferSupport);
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
                                                                      List<NadelExecutableNormalizedField> executableNormalizedFields,
                                                                      @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                      VariableAccumulator variableAccumulator,
                                                                      boolean deferSupport) {
        if (deferSupport) {
            return subselectionsForNormalizedFieldWithDeferSupport(schema, parentOutputType, executableNormalizedFields, normalizedFieldToQueryDirectives, variableAccumulator);
        } else {
            return subselectionsForNormalizedFieldNoDeferSupport(schema, parentOutputType, executableNormalizedFields, normalizedFieldToQueryDirectives, variableAccumulator);
        }
    }

    private static List<Selection<?>> subselectionsForNormalizedFieldNoDeferSupport(GraphQLSchema schema,
                                                                                    @NonNull String parentOutputType,
                                                                                    List<NadelExecutableNormalizedField> executableNormalizedFields,
                                                                                    @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                                    VariableAccumulator variableAccumulator) {
        List<Selection<?>> selections = new ArrayList<>();

        // All conditional fields go here instead of directly to selections, so they can be grouped together
        // in the same inline fragment in the output
        Map<String, List<Field>> fieldsByTypeCondition = new LinkedHashMap<>();

        for (NadelExecutableNormalizedField nf : executableNormalizedFields) {
            // Nadel: a field flagged forcePrintAsUnconditional is emitted bare even when isConditional would wrap it in a fragment.
            if (nf.isConditional(schema) && !nf.isForcePrintAsUnconditional()) {
                selectionForNormalizedField(schema, nf, normalizedFieldToQueryDirectives, variableAccumulator, false)
                        .forEach((objectTypeName, field) ->
                                fieldsByTypeCondition
                                        .computeIfAbsent(objectTypeName, ignored -> new ArrayList<>())
                                        .add(field));
            } else {
                selections.add(selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, false));
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
                                                                                      List<NadelExecutableNormalizedField> executableNormalizedFields,
                                                                                      @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                                      VariableAccumulator variableAccumulator) {
        List<Selection<?>> selections = new ArrayList<>();

        // All conditional and deferred fields go here instead of directly to selections, so they can be grouped together
        // in the same inline fragment in the output
        //
        Map<ExecutionFragmentDetails, List<Field>> fieldsByFragmentDetails = new LinkedHashMap<>();

        for (NadelExecutableNormalizedField nf : executableNormalizedFields) {
            LinkedHashSet<NormalizedDeferredExecution> deferredExecutions = nf.getDeferredExecutions();

            // Nadel: a field flagged forcePrintAsUnconditional is emitted bare even when isConditional would wrap it in a fragment.
            if (nf.isConditional(schema) && !nf.isForcePrintAsUnconditional()) {
                selectionForNormalizedField(schema, nf, normalizedFieldToQueryDirectives, variableAccumulator, true)
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
                Field field = selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, true);

                deferredExecutions.forEach(deferredExecution -> {
                    fieldsByFragmentDetails
                            .computeIfAbsent(new ExecutionFragmentDetails(null, deferredExecution), ignored -> new ArrayList<>())
                            .add(field);
                });
            } else {
                selections.add(selectionForNormalizedField(schema, parentOutputType, nf, normalizedFieldToQueryDirectives, variableAccumulator, true));
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
                                                                  NadelExecutableNormalizedField executableNormalizedField,
                                                                  @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                                  VariableAccumulator variableAccumulator,
                                                                  boolean deferSupport) {
        Map<String, Field> groupedFields = new LinkedHashMap<>();

        for (String objectTypeName : executableNormalizedField.getObjectTypeNames()) {
            groupedFields.put(objectTypeName, selectionForNormalizedField(schema, objectTypeName, executableNormalizedField, normalizedFieldToQueryDirectives, variableAccumulator, deferSupport));
        }

        return groupedFields;
    }

    /**
     * @return a single {@link Field} resolved in the context of the given object type
     */
    private static Field selectionForNormalizedField(GraphQLSchema schema,
                                                     String objectTypeName,
                                                     NadelExecutableNormalizedField executableNormalizedField,
                                                     @NonNull Map<NadelExecutableNormalizedField, QueryDirectives> normalizedFieldToQueryDirectives,
                                                     VariableAccumulator variableAccumulator,
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

    private static @NonNull List<Directive> buildDirectives(NadelExecutableNormalizedField executableNormalizedField, QueryDirectives queryDirectives, VariableAccumulator variableAccumulator) {
        if (queryDirectives == null || queryDirectives.getImmediateAppliedDirectivesByField().isEmpty()) {
            return emptyList();
        }
        return queryDirectives.getImmediateAppliedDirectivesByField().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .map(queryAppliedDirective -> buildDirective(executableNormalizedField, queryDirectives, queryAppliedDirective, variableAccumulator))
                .collect(Collectors.toList());
    }

    private static Directive buildDirective(NadelExecutableNormalizedField executableNormalizedField, QueryDirectives queryDirectives, QueryAppliedDirective queryAppliedDirective, VariableAccumulator variableAccumulator) {

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
                                                             NadelExecutableNormalizedField nf) {
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
