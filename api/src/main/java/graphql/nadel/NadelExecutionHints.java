package graphql.nadel;

import graphql.PublicApi;

@PublicApi
public class NadelExecutionHints {
    private NadelExecutionHints(Builder builder) {
        this.transformsOnHydrationFields = builder.transformsOnHydrationFields;
    }

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

    public static Builder newHints() {
        return new Builder();
    }

    public static class Builder {
        private boolean transformsOnHydrationFields;

        private Builder() {
        }

        public Builder transformsOnHydrationFields(boolean flag) {
            this.transformsOnHydrationFields = flag;
            return this;
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(this);
        }
    }
}
