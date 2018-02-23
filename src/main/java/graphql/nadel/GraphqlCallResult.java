package graphql.nadel;

import java.util.List;
import java.util.Map;

public class GraphqlCallResult {
    private Map<String, Object> data;
    private List<String> errors;

    public GraphqlCallResult(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}