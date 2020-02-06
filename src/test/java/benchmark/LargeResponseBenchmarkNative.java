package benchmark;


import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.nadel.Nadel;
import graphql.nadel.NadelExecutionInput;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionFactory;
import graphql.nadel.ServiceExecutionResult;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class LargeResponseBenchmarkNative {


    public static class NadelInstance {
        Nadel nadel;
        String query;

        public void setup() throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            String schemaString = readFile("./large_response_benchmark_schema.graphqls");
            TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaString);

            String responseString = readFile("./large_underlying_service_result.json");
            Map responseMap = objectMapper.readValue(responseString, Map.class);
            ServiceExecutionResult serviceExecutionResult = new ServiceExecutionResult((Map<String, Object>) responseMap.get("data"));
            ServiceExecution serviceExecution = serviceExecutionParameters -> CompletableFuture.completedFuture(serviceExecutionResult);
            ServiceExecutionFactory serviceExecutionFactory = new ServiceExecutionFactory() {
                @Override
                public ServiceExecution getServiceExecution(String serviceName) {
                    return serviceExecution;
                }

                @Override
                public TypeDefinitionRegistry getUnderlyingTypeDefinitions(String serviceName) {
                    return typeDefinitionRegistry;
                }
            };
            String nsdl = "service activity{" + schemaString + "}";
            nadel = Nadel.newNadel().dsl(nsdl).serviceExecutionFactory(serviceExecutionFactory).build();
            query = readFile("./large_response_benchmark_query.graphql");
        }

        private String readFile(String file) throws IOException {
            FileInputStream is = new FileInputStream(file);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");


        }

    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        NadelInstance nadelInstance = new NadelInstance();
        nadelInstance.setup();
        for (int x = 0; x < 8; x++) {
            new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    long t = System.currentTimeMillis();
                    NadelExecutionInput nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                            .forkJoinPool(ForkJoinPool.commonPool())
                            .query(nadelInstance.query)
                            .build();
                    try {
                        ExecutionResult executionResult = nadelInstance.nadel.execute(nadelExecutionInput).get();
                        System.out.println("t:" + (System.currentTimeMillis() - t) + " thread: " + Thread.currentThread().getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }).start();
        }
    }


}
