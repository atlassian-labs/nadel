package benchmark;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.ExecutionResult;
import graphql.nadel.Nadel;
import graphql.nadel.NadelExecutionInput;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionFactory;
import graphql.nadel.ServiceExecutionResult;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * See http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/ for more samples
 * on what you can do with JMH
 *
 * You MUST have the JMH plugin for IDEA in place for this to work :  https://github.com/artyushov/idea-jmh-plugin
 *
 * Install it and then just hit "Run" on a certain benchmark method
 */

public class LargeResponseBenchmark {

    static ObjectMapper objectMapper = new ObjectMapper();
    static Nadel nadel;
    static String query;

    static {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @Warmup(iterations = 2, time = 5, batchSize = 1)
    @Measurement(iterations = 3, time = 10)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @OperationsPerInvocation()
    public void benchMarkAvgTime() {
        executeQuery();
    }

//    public static void main(String[] args) {
//        for (int i = 0; i < 10000; i++) {
//            executeQuery();
//        }
//    }

    public static ExecutionResult executeQuery() {
        try {
            long t = System.currentTimeMillis();

            NadelExecutionInput nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                    .forkJoinPool(ForkJoinPool.commonPool())
                    .query(query)
                    .build();
            ExecutionResult executionResult = nadel.execute(nadelExecutionInput).get();
//            System.out.println("TOTAL: " + (System.currentTimeMillis() - t));
            return executionResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static void init() throws IOException {

        String schemaString = readFromClasspath("large_response_benchmark_schema.graphqls");
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaString);

        String responseString = readFromClasspath("large_underlying_service_result.json");
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

        query = readFromClasspath("large_response_benchmark_query.graphql");

    }


    private static String readFromClasspath(String file) {
        try {
            URL url = Resources.getResource(file);
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
