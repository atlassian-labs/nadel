package graphql.nadel;

import graphql.PublicApi;
import graphql.language.Document;


@PublicApi
public class DelegatedExecutionParameters {

    private final Document query;

    private DelegatedExecutionParameters(Document query) {
        this.query = query;
    }

    public Document getQuery() {
        return query;
    }

    public static Builder newDelegatedExecutionParameters() {
        return new Builder();
    }

    public static class Builder {
        private Document query;

        private Builder() {

        }

        public Builder query(Document query) {
            this.query = query;
            return this;
        }

        public DelegatedExecutionParameters build() {
            return new DelegatedExecutionParameters(query);
        }

    }
}
