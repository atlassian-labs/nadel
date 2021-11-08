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
    }

    private final LegacyOperationNamesHint legacyOperationNames;
    private final boolean transformsOnHydrationFields;

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
