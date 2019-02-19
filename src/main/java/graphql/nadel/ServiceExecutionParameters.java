package graphql.nadel;

import graphql.PublicApi;
import graphql.language.Document;


@PublicApi
public class ServiceExecutionParameters {

    private final Document query;

    private ServiceExecutionParameters(Document query) {
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

        public ServiceExecutionParameters build() {
            return new ServiceExecutionParameters(query);
        }

    }
}
