package graphql.nadel;

import graphql.PublicApi;

@PublicApi
public class NadelExecutionHints {
    private final boolean optimizeExecution;

    private NadelExecutionHints(boolean optimizeExecution) {
        this.optimizeExecution = optimizeExecution;
    }

    public static Builder newHints() {
        return new Builder();
    }

    public boolean getOptimizeExecutionFlag() {
        return optimizeExecution;
    }

    public static class Builder {
        private boolean optimizeExecution;

        private Builder() {
        }

        public Builder optimizeExecution(boolean flag) {
            this.optimizeExecution = flag;
            return this;
        }

        public NadelExecutionHints build() {
            return new NadelExecutionHints(optimizeExecution);
        }
    }
}
