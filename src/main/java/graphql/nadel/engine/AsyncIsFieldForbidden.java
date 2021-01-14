package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.engine.transformation.OverallTypeInfo;
import graphql.nadel.engine.transformation.OverallTypeInformation;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.schema.GraphQLFieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.nadel.dsl.NodeId.getId;

public class AsyncIsFieldForbidden {

    private final Map<String, GraphQLError> fieldIdsToErrors = new ConcurrentHashMap<>();
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final NadelContext nadelContext;
    private final OverallTypeInformation<?> overallTypeInformation;

    public AsyncIsFieldForbidden(ServiceExecutionHooks serviceExecutionHooks, NadelContext nadelContext, OverallTypeInformation<?> overallTypeInformation) {
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.nadelContext = nadelContext;
        this.overallTypeInformation = overallTypeInformation;
    }

    public CompletableFuture<Map<String, GraphQLError>> getForbiddenFields(Node<?> root) {
        if (root instanceof Field) {
            return visitField((Field) root)
                    .thenApply(unused -> fieldIdsToErrors);
        }
        return visitChildren(root)
                .thenApply(unused -> fieldIdsToErrors);
    }

    private CompletableFuture<Void> visitChildren(Node<?> root) {
        List<CompletableFuture<Void>> childCFs = new ArrayList<>();
        for (Node<?> child : root.getChildren()) {
            if (child instanceof Field) {
                childCFs.add(visitField((Field) child));
            }
            childCFs.add(visitChildren(child));
        }
        return CompletableFuture.allOf(childCFs.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> visitField(Field field) {
        if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
            return CompletableFuture.completedFuture(null);
        }

        String fieldId = getId(field);
        OverallTypeInfo overallTypeInfo = overallTypeInformation.getOverallTypeInfo(fieldId);
        // this means we have a new field which was added by a transformation and we don't have overall type info about it
        if (overallTypeInfo == null) {
            return visitChildren(field);
        }

        GraphQLFieldDefinition fieldDefinitionOverall = overallTypeInfo.getFieldDefinition();

        List<NormalizedQueryField> normalizedFields = nadelContext.getNormalizedOverallQuery().getNormalizedFieldsByFieldId(fieldId);

        return serviceExecutionHooks.isFieldForbidden(field, normalizedFields, fieldDefinitionOverall, nadelContext.getUserSuppliedContext())
                .thenCompose(graphQLError -> {
                    if (graphQLError.isPresent()) {
                        fieldIdsToErrors.put(fieldId, graphQLError.get());
                        return CompletableFuture.completedFuture(null);
                    }
                    return visitChildren(field);
                });
    }
}
