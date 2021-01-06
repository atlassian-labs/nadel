package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.engine.transformation.OverallTypeInfo;
import graphql.nadel.engine.transformation.OverallTypeInformation;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.schema.GraphQLFieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.nadel.dsl.NodeId.getId;

public class AsyncIsFieldAllowed {

    private Map<String, GraphQLError> fieldIdsToErrors = new ConcurrentHashMap<>();
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final NadelContext nadelContext;
    private final OverallTypeInformation<?> overallTypeInformation;

    public AsyncIsFieldAllowed(ServiceExecutionHooks serviceExecutionHooks, NadelContext nadelContext, OverallTypeInformation<?> overallTypeInformation) {
        this.serviceExecutionHooks = serviceExecutionHooks;
        this.nadelContext = nadelContext;
        this.overallTypeInformation = overallTypeInformation;
    }

    public CompletableFuture<Map<String, GraphQLError>> isFieldAllowed(Node<?> root) {
        return visitChildren(root).thenApply(unused -> {
            return fieldIdsToErrors;
        });
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

        return serviceExecutionHooks.isFieldAllowed(field, fieldDefinitionOverall, nadelContext.getUserSuppliedContext()).thenCompose(graphQLError -> {
            if (graphQLError.isPresent()) {
                fieldIdsToErrors.put(fieldId, graphQLError.get());
                return CompletableFuture.completedFuture(null);
            }
            return visitChildren(field);
        });
    }
}
