package graphql.nadel;

import graphql.GraphQLError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphqlCallResult {
    private Map<String, Object> data;
    private List<GraphQLError> errors = new ArrayList<>();

    public GraphqlCallResult(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
    }
}