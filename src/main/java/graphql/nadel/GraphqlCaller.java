package graphql.nadel;

import graphql.language.Document;

import java.util.List;
import java.util.Map;

public class GraphqlCaller {


    public static class GraphqlCallResult {
        Map<String, Object> data;
        List<String> errors;

    }


    public GraphqlCallResult call(Document query) {
        return new GraphqlCallResult();
    }


}
