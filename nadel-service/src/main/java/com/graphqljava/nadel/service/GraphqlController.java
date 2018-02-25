package com.graphqljava.nadel.service;

import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
}
