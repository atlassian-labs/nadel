package graphql.nadel.tests.hooks;

import graphql.language.StringValue;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecutionHydrationDetails;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.engine.NadelExecutionContext;
import graphql.nadel.engine.NadelServiceExecutionContext;
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint;
import graphql.nadel.engine.transform.NadelTransform;
import graphql.nadel.engine.transform.NadelTransformFieldResult;
import graphql.nadel.engine.transform.NadelTransformJavaCompat;
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext;
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat;
import graphql.nadel.engine.transform.result.NadelResultInstruction;
import graphql.nadel.engine.transform.result.json.JsonNodes;
import graphql.nadel.engine.util.CollectionUtilKt;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.NormalizedInputValue;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import kotlin.collections.CollectionsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.nadel.engine.util.GraphQLUtilKt.makeFieldCoordinates;
import static java.util.Collections.emptyList;

public class JavaAriTransform implements NadelTransformJavaCompat<Set<String>> {
    public static NadelTransform<Set<String>> create() {
        return NadelTransformJavaCompat.create(new JavaAriTransform());
    }

    public String getSingleObjectTypeName(ExecutableNormalizedField overallField) {
        // This single enforces there's actually one single entry
        return CollectionsKt.single(overallField.getObjectTypeNames());
    }

    @NotNull
    @Override
    public CompletableFuture<Set<String>> isApplicable(@NotNull NadelExecutionContext executionContext,
                                                       @NotNull NadelServiceExecutionContext serviceExecutionContext,
                                                       @NotNull NadelOverallExecutionBlueprint executionBlueprint,
                                                       @NotNull Map<String, ? extends Service> services,
                                                       @NotNull Service service,
                                                       @NotNull ExecutableNormalizedField overallField,
                                                       @Nullable NadelTransformServiceExecutionContext transformServiceExecutionContext,
                                                       @Nullable ServiceExecutionHydrationDetails hydrationDetails) {

        String singleObjectTypeName = getSingleObjectTypeName(overallField);
        FieldCoordinates fieldCoordinates = makeFieldCoordinates(singleObjectTypeName, overallField.getName());
        GraphQLFieldDefinition fieldDef = executionBlueprint.getEngineSchema().getFieldDefinition(fieldCoordinates);
        Objects.requireNonNull(fieldDef, "No field def");

        var argDefsByName = CollectionUtilKt.strictAssociateBy(fieldDef.getArguments(), GraphQLArgument::getName);

        var argsToTransform = overallField.getNormalizedArguments().keySet()
            .stream()
            .filter((argName) -> {
                return argDefsByName.get(argName).hasDirective("interpretAri");
            })
            .collect(Collectors.toSet());

        if (argsToTransform.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(argsToTransform);
    }

    @NotNull
    @Override
    public CompletableFuture<NadelTransformFieldResult> transformField(@NotNull NadelExecutionContext executionContext,
                                                                       @NotNull NadelServiceExecutionContext serviceExecutionContext,
                                                                       @NotNull NadelQueryTransformerJavaCompat transformer,
                                                                       @NotNull NadelOverallExecutionBlueprint executionBlueprint,
                                                                       @NotNull Service service,
                                                                       @NotNull ExecutableNormalizedField field,
                                                                       @NotNull Set<String> fieldsArgsToInterpret,
                                                                       @Nullable NadelTransformServiceExecutionContext transformServiceExecutionContext) {
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

    @NotNull
    @Override
    public CompletableFuture<List<NadelResultInstruction>> getResultInstructions(@NotNull NadelExecutionContext executionContext,
                                                                                 @NotNull NadelServiceExecutionContext serviceExecutionContext,
                                                                                 @NotNull NadelOverallExecutionBlueprint executionBlueprint,
                                                                                 @NotNull Service service,
                                                                                 @NotNull ExecutableNormalizedField overallField,
                                                                                 @Nullable ExecutableNormalizedField underlyingParentField,
                                                                                 @NotNull ServiceExecutionResult result,
                                                                                 @NotNull Set<String> strings,
                                                                                 @NotNull JsonNodes nodes,
                                                                                 @Nullable NadelTransformServiceExecutionContext transformServiceExecutionContext) {
        return CompletableFuture.completedFuture(emptyList());
    }
}
