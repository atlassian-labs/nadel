package graphql.nadel.engine;

import graphql.Internal;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.nadel.normalized.NormalizedQuery;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

/**
 * We use a wrapper Nadel context object over top of the calling users supplied one
 * so that we can get access to Nadel specific context properties in
 * a type safe manner
 */
@Internal
public class NadelContext {
    private final Object userSuppliedContext;
    private final String underscoreTypeNameAlias;
    private final String originalOperationName;
    private final String objectIdentifierAlias;
    private final ForkJoinPool forkJoinPool;
    private final NormalizedQuery normalizedQuery;

    private NadelContext(Object userSuppliedContext,
                         String underscoreTypeNameAlias,
                         String originalOperationName,
                         String objectIdentifierAlias,
                         ForkJoinPool forkJoinPool,
                         NormalizedQuery normalizedOverallQuery) {
        this.userSuppliedContext = userSuppliedContext;
        this.underscoreTypeNameAlias = underscoreTypeNameAlias;
        this.originalOperationName = originalOperationName;
        this.objectIdentifierAlias = objectIdentifierAlias;
        this.forkJoinPool = forkJoinPool;
        this.normalizedQuery = normalizedOverallQuery;
    }

    public Object getUserSuppliedContext() {
        return userSuppliedContext;
    }

    public String getUnderscoreTypeNameAlias() {
        return underscoreTypeNameAlias;
    }

    public String getObjectIdentifierAlias() {
        return objectIdentifierAlias;
    }

    public String getOriginalOperationName() {
        return originalOperationName;
    }

    public ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }

    public static Builder newContext() {
        return new Builder();
    }


    private static String mkUnderscoreTypeNameAlias(String uuid) {
        return String.format("typename__%s", uuid);
    }

    private static String createObjectIdentifierAlias(String uuid) {
        return String.format("object_identifier__%s", uuid);
    }

    public NormalizedQuery getNormalizedQuery() {
        return normalizedQuery;
    }

    public static class Builder {
        private Object userSuppliedContext;
        private String originalOperationName;
        private String artificialFieldsUUID;
        private ForkJoinPool forkJoinPool;
        private NormalizedQuery normalizedOverallQuery;


        public Builder normalizedOverallQuery(NormalizedQuery normalizedQuery) {
            this.normalizedOverallQuery = normalizedQuery;
            return this;
        }

        public Builder userSuppliedContext(Object userSuppliedContext) {
            this.userSuppliedContext = userSuppliedContext;
            return this;
        }

        public Builder originalOperationName(Document document, String executingOperation) {
            if (executingOperation != null) {
                this.originalOperationName = executingOperation;
            } else {
                Optional<OperationDefinition> opDef = document.getDefinitions().stream()
                        .filter(def -> def instanceof OperationDefinition).map(OperationDefinition.class::cast)
                        .findFirst();
                this.originalOperationName = opDef.map(OperationDefinition::getName).orElse(null);
            }
            return this;
        }

        public Builder artificialFieldsUUID(String artificialFieldsUUID) {
            this.artificialFieldsUUID = artificialFieldsUUID;
            return this;
        }

        public Builder forkJoinPool(ForkJoinPool forkJoinPool) {
            this.forkJoinPool = forkJoinPool;
            return this;
        }

        public NadelContext build() {
            String uuid = artificialFieldsUUID != null ? artificialFieldsUUID : UUID.randomUUID().toString().replaceAll("-", "_");
            return new NadelContext(userSuppliedContext, mkUnderscoreTypeNameAlias(uuid), originalOperationName, createObjectIdentifierAlias(uuid), forkJoinPool, normalizedOverallQuery);
        }
    }
}
