package graphql.nadel;

import graphql.PublicApi;

import java.util.function.Consumer;

@PublicApi
public class NadelExecutionHints {
    private NadelExecutionHints(Builder builder) {
        this.transformsOnHydrationFields = builder.transformsOnHydrationFields;
        this.legacyOperationNames = builder.legacyOperationNames;
    }

    // we need to be able to change the value of "legacyOperationNames" inside transforms
    // so the field needs to be mutable.
    private boolean legacyOperationNames;
    private final boolean transformsOnHydrationFields;

    /**
     * Flag to determine whether nextgen will generate the traditional nadel_2_service_opName
     * operation names.
     */
    public boolean getLegacyOperationNames() {
        return legacyOperationNames;
    }

    /**
     * See {@link #getLegacyOperationNames()}
     */
    public void setLegacyOperationNames(boolean legacyOperationNames) {
        this.legacyOperationNames = legacyOperationNames;
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
     * Utility method to transform this object.
     */
    public NadelExecutionHints transform(Consumer<NadelExecutionHints.Builder> builderConsumer) {
        NadelExecutionHints.Builder builder = new NadelExecutionHints.Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    /**
     * Create a shallow copy of this object.
     */
    public NadelExecutionHints copy() {
        return this.transform(builder -> {
            // noop
        });
    }

    public static Builder newHints() {
        return new Builder();
    }

    public static class Builder {
        private boolean transformsOnHydrationFields;
        private boolean legacyOperationNames;

        private Builder() {
        }

        private Builder(NadelExecutionHints nadelExecutionHints) {
            this.transformsOnHydrationFields = nadelExecutionHints.transformsOnHydrationFields;
            this.legacyOperationNames = nadelExecutionHints.legacyOperationNames;
        }

        public Builder transformsOnHydrationFields(boolean flag) {
            this.transformsOnHydrationFields = flag;
            return this;
        }

        public Builder legacyOperationNames(boolean flag) {
            this.legacyOperationNames = flag;
            return this;
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(this);
        }
    }
}
