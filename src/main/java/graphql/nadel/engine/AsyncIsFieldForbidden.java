package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.nadel.util.FpKit.flatMap;
import static graphql.nadel.util.FpKit.map;

public class AsyncIsFieldForbidden {

    private final Map<NormalizedQueryField, GraphQLError> fieldIdsToErrors = new ConcurrentHashMap<>();
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final NadelContext nadelContext;

    public AsyncIsFieldForbidden(ServiceExecutionHooks serviceExecutionHooks, NadelContext nadelContext) {
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.nadelContext = nadelContext;
    }

    public CompletableFuture<Map<NormalizedQueryField, GraphQLError>> getForbiddenFields(Node<?> root) {
        List<CompletableFuture<Void>> visitAllFields = map(getNormalisedFields(root), this::visitField);
        return allOf(visitAllFields)
                .thenApply(ignored -> fieldIdsToErrors);
    }

    private List<NormalizedQueryField> getNormalisedFields(Node<?> root) {
        if (root instanceof Field) {
            return getNormalisedFieldsFromAstField(root);
        }
        return flatMap(root.getChildren(), this::getNormalisedFieldsFromAstField);
    }

    private List<NormalizedQueryField> getNormalisedFieldsFromAstField(Node<?> root) {
        return nadelContext.getNormalizedOverallQuery().getNormalizedFieldsByFieldId(getId(root));
    }

    private CompletableFuture<Void> visitField(NormalizedQueryField field) {
        if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
            return CompletableFuture.completedFuture(null);
        }

        return serviceExecutionHooks.isFieldForbidden(field, nadelContext.getUserSuppliedContext())
                .thenCompose(graphQLError -> {
                    if (graphQLError.isPresent()) {
                        fieldIdsToErrors.put(field, graphQLError.get());
                        return CompletableFuture.completedFuture(null);
                    }
                    List<CompletableFuture<Void>> visitAllChildren = map(field.getChildren(), this::visitField);
                    return allOf(visitAllChildren);
                });
    }

    private static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> cfs) {
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[0]));
    }
}
