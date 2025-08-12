package graphql.nadel.tests.hooks;

import graphql.language.StringValue;
import graphql.nadel.engine.NadelOperationExecutionContext;
import graphql.nadel.engine.transform.NadelTransform;
import graphql.nadel.engine.transform.NadelTransformFieldContext;
import graphql.nadel.engine.transform.NadelTransformFieldResult;
import graphql.nadel.engine.transform.NadelTransformJavaCompat;
import graphql.nadel.engine.transform.NadelTransformOperationContext;
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat;
import graphql.nadel.engine.transform.result.NadelResultInstruction;
import graphql.nadel.engine.transform.result.json.JsonNodes;
import graphql.nadel.engine.util.CollectionUtilKt;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;
import graphql.schema.GraphQLArgument;
import kotlin.collections.CollectionsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.nadel.engine.util.GraphQLUtilKt.makeFieldCoordinates;
import static java.util.Collections.emptyList;

public class JavaAriTransform implements NadelTransformJavaCompat<JavaAriTransform.TransformOperationContext, JavaAriTransform.TransformFieldContext> {
    public static class TransformOperationContext extends NadelTransformOperationContext {

        private final NadelOperationExecutionContext parentContext;

        TransformOperationContext(NadelOperationExecutionContext parentContext) {
            this.parentContext = parentContext;
        }

        @Override
        public @NotNull NadelOperationExecutionContext getParentContext() {
            return parentContext;
        }

    }

    public static class TransformFieldContext extends NadelTransformFieldContext<TransformOperationContext> {

        private final TransformOperationContext parentContext;
        private final ExecutableNormalizedField overallField;
        final Set<String> argsToTransform;

        TransformFieldContext(
            TransformOperationContext parentContext,
            ExecutableNormalizedField overallField,
            Set<String> argsToTransform
        ) {
            this.parentContext = parentContext;
            this.overallField = overallField;
            this.argsToTransform = argsToTransform;
        }

        @Override
        public @NotNull TransformOperationContext getParentContext() {
            return parentContext;
        }

        @Override
        public @NotNull ExecutableNormalizedField getOverallField() {
            return overallField;
        }

    }

    public static NadelTransform<?, ?> create() {
        return NadelTransformJavaCompat.create(new JavaAriTransform());
    }

    public String getSingleObjectTypeName(ExecutableNormalizedField overallField) {
        // This single enforces there's actually one single entry
        return CollectionsKt.single(overallField.getObjectTypeNames());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull TransformOperationContext> getTransformOperationContext(
        @NotNull NadelOperationExecutionContext operationExecutionContext
    ) {
        return CompletableFuture.completedFuture(new TransformOperationContext(operationExecutionContext));
    }

    @Override
    public @NotNull CompletableFuture<@Nullable TransformFieldContext> getTransformFieldContext(
        @NotNull TransformOperationContext transformContext,
        @NotNull ExecutableNormalizedField overallField
    ) {
        var executionBlueprint = transformContext.getExecutionBlueprint();
        var singleObjectTypeName = getSingleObjectTypeName(overallField);
        var fieldCoordinates = makeFieldCoordinates(singleObjectTypeName, overallField.getName());
        var fieldDef = executionBlueprint.getEngineSchema().getFieldDefinition(fieldCoordinates);
        Objects.requireNonNull(fieldDef, "No field def");

        var argDefsByName = CollectionUtilKt.strictAssociateBy(fieldDef.getArguments(), GraphQLArgument::getName);

        var argsToTransform = overallField.getNormalizedArguments().keySet()
            .stream()
            .filter((argName) -> argDefsByName.get(argName).hasAppliedDirective("interpretAri"))
            .collect(Collectors.toSet());

        if (argsToTransform.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(
            new TransformFieldContext(transformContext, overallField, argsToTransform)
        );
    }

    @Override
    public @NotNull CompletableFuture<@NotNull NadelTransformFieldResult> transformField(
        @NotNull TransformFieldContext transformContext,
        @NotNull NadelQueryTransformerJavaCompat transformer,
        @NotNull ExecutableNormalizedField field
    ) {
        var fieldsArgsToInterpret = transformContext.argsToTransform;

        return CompletableFuture.completedFuture(
            new NadelTransformFieldResult(
                field.transform((builder) -> {
                    LinkedHashMap<String, NormalizedInputValue> newArgs = new LinkedHashMap<>();

                    field.getNormalizedArguments()
                        .entrySet()
                        .stream()
                        .map((entry) -> {
                            String name = entry.getKey();
                            var value = entry.getValue();
                            if (fieldsArgsToInterpret.contains(name)) {
                                var str = ((StringValue) value.getValue()).getValue();

                                return new SimpleEntry<>(
                                    name,
                                    new NormalizedInputValue(
                                        value.getTypeName(),
                                        new StringValue(
                                            StringsKt.substringAfterLast(str, "/", str)
                                        )
                                    )
                                );
                            } else {
                                return entry;
                            }
                        })
                        .forEach((entry) -> {
                            newArgs.put(entry.getKey(), entry.getValue());
                        });

                    builder.normalizedArguments(newArgs);
                }),
                emptyList()
            )
        );
    }

    @Override
    public @NotNull CompletableFuture<@NotNull List<? extends @NotNull NadelResultInstruction>> transformResult(
        @NotNull TransformFieldContext transformContext,
        @Nullable ExecutableNormalizedField underlyingParentField,
        @NotNull JsonNodes resultNodes
    ) {
        return CompletableFuture.completedFuture(emptyList());
    }
}
