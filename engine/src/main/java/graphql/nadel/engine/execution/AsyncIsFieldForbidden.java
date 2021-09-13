package graphql.nadel.engine.execution;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.hooks.HydrationArguments;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.schema.GraphQLSchema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.nadel.util.FpKit.flatMap;
import static graphql.nadel.util.FpKit.map;

public class AsyncIsFieldForbidden {

    @NotNull
    private final Map<NormalizedQueryField, GraphQLError> fieldsToErrors = new ConcurrentHashMap<>();
    @NotNull
    private final ServiceExecutionHooks serviceExecutionHooks;
    @NotNull
    private final NadelContext nadelContext;
    @NotNull
    private final GraphQLSchema graphQLSchema;
    @NotNull
    private final HydrationArguments hydrationArguments;
    @NotNull
    private final Map<String, Object> variables;

    public AsyncIsFieldForbidden(
            @NotNull ServiceExecutionHooks serviceExecutionHooks,
            @NotNull NadelContext nadelContext,
            @NotNull GraphQLSchema graphQLSchema,
            @NotNull HydrationArguments hydrationArguments,
            @NotNull Map<String, Object> variables
    ) {
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.nadelContext = nadelContext;
        this.graphQLSchema = graphQLSchema;
        this.hydrationArguments = hydrationArguments;
        this.variables = variables;
    }

    @NotNull
    public CompletableFuture<Map<NormalizedQueryField, GraphQLError>> getForbiddenFields(Node<?> root) {
        List<NormalizedQueryField> normalisedFields = getNormalisedFields(root);
        List<CompletableFuture<Void>> visitNormalisedFields = map(normalisedFields, this::visitField);
        return allOf(visitNormalisedFields)
                .thenApply(ignored -> fieldsToErrors);
    }

    private List<NormalizedQueryField> getNormalisedFields(Node<?> node) {
        if (node instanceof Field) {
            return nadelContext.getNormalizedOverallQuery()
                    .getNormalizedFieldsByFieldId(getId(node));
        }
        return flatMap(node.getChildren(), this::getNormalisedFields);
    }

    private CompletableFuture<Void> visitField(NormalizedQueryField field) {
        if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
            return CompletableFuture.completedFuture(null);
        }
        return serviceExecutionHooks.isFieldForbidden(field, hydrationArguments, variables, graphQLSchema, nadelContext.getUserSuppliedContext())
                .thenCompose(graphQLError -> {
                    if (graphQLError.isPresent()) {
                        fieldsToErrors.put(field, graphQLError.get());
                        return CompletableFuture.completedFuture(null);
                    }
                    List<CompletableFuture<Void>> visitChildren = map(field.getChildren(), this::visitField);
                    return allOf(visitChildren);
                });
    }

    private static CompletableFuture<Void> allOf(List<CompletableFuture<Void>> cfs) {
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[0]));
    }
}
