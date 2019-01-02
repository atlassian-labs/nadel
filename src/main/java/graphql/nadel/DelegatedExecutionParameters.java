package graphql.nadel;

import graphql.PublicApi;
import graphql.language.Node;


@PublicApi
public class DelegatedExecutionParameters {

    private final Node query;

    public DelegatedExecutionParameters(Node query) {
        this.query = query;
    }

    public static Builder newDelegatedExecutionParameters() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {

        }

        public DelegatedExecutionParameters build() {
//           return new DelegatedExecutionParameters();
            return null;
        }

    }
}
