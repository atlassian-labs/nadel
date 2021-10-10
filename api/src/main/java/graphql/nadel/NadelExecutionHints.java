package graphql.nadel;

import graphql.PublicApi;

@PublicApi
public class NadelExecutionHints {
    private NadelExecutionHints(Builder builder) {
    }

    private boolean legacyOperationNames;

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

    public static Builder newHints() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(this);
        }
    }
}
