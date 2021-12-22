package graphql.nadel;

import graphql.PublicApi;
import graphql.nadel.hints.LegacyOperationNamesHint;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@PublicApi
public class NadelExecutionHints {
    private NadelExecutionHints(Builder builder) {
        this.transformsOnHydrationFields = builder.transformsOnHydrationFields;
        this.legacyOperationNames = builder.legacyOperationNames;
        this.newJsonNodeTraversal = builder.newJsonNodeTraversal;
        this.asyncResultTransform = builder.asyncResultTransform;
    }

    private final LegacyOperationNamesHint legacyOperationNames;
    private final boolean transformsOnHydrationFields;
    private final boolean newJsonNodeTraversal;
    private final boolean asyncResultTransform;

    /**
     * Flag to determine whether nextgen will generate the traditional nadel_2_service_opName
     * operation names.
     */
    @NotNull
    public LegacyOperationNamesHint getLegacyOperationNames() {
        return legacyOperationNames;
    }

    /**
     * Flag to activate nextgen transforms for hydration fields.
     * <p>
     * This flag is temporary. Ultimately the code will be changed to the
     * "enabled" state of this flag.
     */
    public boolean getTransformsOnHydrationFields() {
        return transformsOnHydrationFields;
    }

    /**
     * Flag to use JsonNodes with traversal caching over the slower but battle-tested JsonNodeExtractor.
     */
    public boolean getNewJsonNodeTraversal() {
        return newJsonNodeTraversal;
    }

    /**
     * Flag to use faster async result transform. Flagged due to concerns.
     */
    public boolean getAsyncResultTransform() {
        return asyncResultTransform;
    }

    /**
     * Returns a builder with the same field values as this object.
     * <p>
     * This is useful for transforming the object.
     */
    public NadelExecutionHints.Builder toBuilder() {
        return new NadelExecutionHints.Builder(this);
    }

    /**
     * Create a shallow copy of this object.
     */
    public NadelExecutionHints copy() {
        return this.toBuilder().build();
    }

    public static Builder newHints() {
        return new Builder();
    }

    public static class Builder {
        private boolean transformsOnHydrationFields;
        private boolean newJsonNodeTraversal;
        private boolean asyncResultTransform;
        private LegacyOperationNamesHint legacyOperationNames = service -> false;

        private Builder() {
        }

        private Builder(NadelExecutionHints nadelExecutionHints) {
            this.transformsOnHydrationFields = nadelExecutionHints.transformsOnHydrationFields;
        }

        public Builder transformsOnHydrationFields(boolean flag) {
            this.transformsOnHydrationFields = flag;
            return this;
        }

        /**
         * @see NadelExecutionHints#getNewJsonNodeTraversal()
         */
        public Builder newJsonNodeTraversal(boolean flag) {
            this.newJsonNodeTraversal = flag;
            return this;
        }

        /**
         * @see NadelExecutionHints#getAsyncResultTransform()
         */
        public Builder asyncResultTransform(boolean flag) {
            this.asyncResultTransform = flag;
            return this;
        }

        public Builder legacyOperationNames(@NotNull LegacyOperationNamesHint flag) {
            Objects.requireNonNull(flag);
            this.legacyOperationNames = flag;
            return this;
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(this);
        }
    }
}
