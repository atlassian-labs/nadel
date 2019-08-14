package graphql.nadel.hooks;

import graphql.PublicApi;
import graphql.language.Document;

import java.util.Collections;
import java.util.Map;

import static graphql.Assert.assertNotNull;

@PublicApi
public class QueryRewriteResult {

    private final Document document;
    private final Map<String, Object> variables;

    private QueryRewriteResult(Builder builder) {
        this.document = assertNotNull(builder.document);
        this.variables = assertNotNull(builder.variables);
    }

    public Document getDocument() {
        return document;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public static Builder newResult() {
        return new Builder();
    }

    public static class Builder {
        private Document document;
        private Map<String, Object> variables = Collections.emptyMap();

        public Builder from(QueryRewriteParams queryRewriteParams) {
            this.document = queryRewriteParams.getDocument();
            this.variables = queryRewriteParams.getVariables();
            return this;
        }

        public Builder document(Document document) {
            this.document = document;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public QueryRewriteResult build() {
            return new QueryRewriteResult(this);
        }
    }
}
