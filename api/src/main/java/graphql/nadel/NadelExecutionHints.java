package graphql.nadel;

import graphql.PublicApi;

@PublicApi
public class NadelExecutionHints {
    private NadelExecutionHints(Builder builder) {
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
