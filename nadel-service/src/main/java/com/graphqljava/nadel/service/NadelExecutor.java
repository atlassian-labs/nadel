package com.graphqljava.nadel.service;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.nadel.Nadel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class NadelExecutor {

    private static final Logger logger = LoggerFactory.getLogger(NadelExecutor.class);

    @Autowired
    GraphqlCallerFactory graphqlCallerFactory;


    Nadel nadel;

    @PostConstruct
    public void init() {
        File dslFile = new File("./nadel-service/nadel-dsl.txt");
        String dsl = readDsl(dslFile);
        nadel = new Nadel(dsl, graphqlCallerFactory);
        logger.info("successfully initialized nadel");
    }

    private String readDsl(File dslFile) {
        String dsl;
        try {
            dsl = Files.asCharSource(dslFile, Charsets.UTF_8).read();
        } catch (IOException e) {
            logger.error("failed to read nadel-dsl.txt", e);
            throw new RuntimeException(e);
        }
        return dsl;
    }

    public CompletableFuture<ExecutionResult> execute(String query, String operationName, Map<String, Object> variables) {
        ExecutionInput.Builder executionInput = ExecutionInput.newExecutionInput()
                .query(query);
        if (operationName != null) {
            executionInput.operationName(operationName);
        }
        if (variables != null) {
            executionInput.variables(variables);
        }
        return nadel.executeAsync(executionInput.build());

    }

}
