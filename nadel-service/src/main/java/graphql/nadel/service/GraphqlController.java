package graphql.nadel.service;

import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
public class GraphqlController {


    @Autowired
    NadelExecutor nadelExecutor;

    @RequestMapping(name = "graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object graphql(@RequestBody() Map<String, Object> body) {
        String query = (String) body.get("query");
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        String operation = (String) body.get("operationName");

        return nadelExecutor
                .execute(query, operation, variables)
                .thenApply(ExecutionResult::toSpecification);

    }

    @RequestMapping(path = "/graphiql", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String app() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("graphiql.html")) {
            return IOUtils.toString(resource, "UTF-8");
        }
    }

}
