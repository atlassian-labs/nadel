package graphql.nadel.engine;

import graphql.Internal;
import graphql.language.Document;
import graphql.language.OperationDefinition;

import java.util.Optional;
import java.util.UUID;

/**
 * We use a wrapper Nadel context object over top of the calling users supplied one
 * so that we can get access to Nadel specific context properties in
 * a type safe manner
 */
@Internal
public class NadelContext {
    final Object userSuppliedContext;
    final String underscoreTypeNameAlias;
    final String originalOperationName;

    private NadelContext(Object userSuppliedContext, String underscoreTypeNameAlias, String originalOperationName) {
        this.userSuppliedContext = userSuppliedContext;
        this.underscoreTypeNameAlias = underscoreTypeNameAlias;
        this.originalOperationName = originalOperationName;
    }

    public Object getUserSuppliedContext() {
        return userSuppliedContext;
    }

    public String getUnderscoreTypeNameAlias() {
        return underscoreTypeNameAlias;
    }

    public String getOriginalOperationName() {
        return originalOperationName;
    }

    public static Builder newContext() {
        return new Builder();
    }

    private static String mkUnderscoreTypeNameAlias() {
        String uuid = UUID.randomUUID().toString();
        return String.format("typename__%s", uuid.replaceAll("-", "_"));
    }


    public static class Builder {
        Object userSuppliedContext;
        String originalOperationName;

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

        public NadelContext build() {
            return new NadelContext(userSuppliedContext, mkUnderscoreTypeNameAlias(), originalOperationName);
        }
    }
}
