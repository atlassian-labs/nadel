package graphql.nadel;

import graphql.PublicApi;
import graphql.nadel.hints.AllDocumentVariablesHint;
import graphql.nadel.hints.LegacyOperationNamesHint;
import graphql.nadel.hints.NewDocumentCompiler;
import graphql.nadel.hints.RunCoerceTransform;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@PublicApi
public class NadelExecutionHints {
    private final LegacyOperationNamesHint legacyOperationNames;
    private final AllDocumentVariablesHint allDocumentVariablesHint;
    private final NewDocumentCompiler newDocumentCompiler;
    private final RunCoerceTransform runCoerceTransform;

    private NadelExecutionHints(Builder builder) {
        this.legacyOperationNames = builder.legacyOperationNames;
        this.allDocumentVariablesHint = builder.allDocumentVariablesHint;
        this.newDocumentCompiler = builder.newDocumentCompiler;
        this.runCoerceTransform = builder.runCoerceTransform;
    }

    /**
     * Flag to determine whether service query documents should use all variables for arguments
     */
    public AllDocumentVariablesHint getAllDocumentVariablesHint() {
        return allDocumentVariablesHint;
    }

    /**
     * Flag to determine whether nextgen will generate the traditional nadel_2_service_opName
     * operation names.
     */
    @NotNull
    public LegacyOperationNamesHint getLegacyOperationNames() {
        return legacyOperationNames;
    }

    /**
     * Flag to determine whether to use the new https://github.com/graphql-java/graphql-java/pull/2638
     * or the previous version.
     */
    @NotNull
    public NewDocumentCompiler getNewDocumentCompiler() {
        return newDocumentCompiler;
    }

    /**
     * Whether to run or disable graphql.nadel.enginekt.transform.NadelCoerceTransform
     */
    @NotNull
    public RunCoerceTransform getRunCoerceTransform() {
        return runCoerceTransform;
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
        private LegacyOperationNamesHint legacyOperationNames = service -> false;
        private AllDocumentVariablesHint allDocumentVariablesHint = service -> false;
        private NewDocumentCompiler newDocumentCompiler = service -> false;
        private RunCoerceTransform runCoerceTransform = service -> true;

        private Builder() {
        }

        private Builder(NadelExecutionHints nadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames;
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint;
            newDocumentCompiler = nadelExecutionHints.newDocumentCompiler;
            runCoerceTransform = nadelExecutionHints.runCoerceTransform;
        }

        public Builder legacyOperationNames(@NotNull LegacyOperationNamesHint flag) {
            Objects.requireNonNull(flag);
            this.legacyOperationNames = flag;
            return this;
        }

        public Builder allDocumentVariablesHint(@NotNull AllDocumentVariablesHint flag) {
            Objects.requireNonNull(flag);
            this.allDocumentVariablesHint = flag;
            return this;
        }

        public Builder newDocumentCompiler(@NotNull NewDocumentCompiler flag) {
            Objects.requireNonNull(flag);
            this.newDocumentCompiler = flag;
            return this;
        }

        public Builder runCoerceTransform(@NotNull RunCoerceTransform flag) {
            Objects.requireNonNull(flag);
            this.runCoerceTransform = flag;
            return this;
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(this);
        }
    }
}
