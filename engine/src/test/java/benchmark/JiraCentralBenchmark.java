package benchmark;


import benchmark.support.GatewaySchemaWiringFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.language.Document;
import graphql.nadel.Nadel;
import graphql.nadel.NadelEngine;
import graphql.nadel.NadelExecutionInput;
import graphql.nadel.NadelGraphQLParser;
import graphql.nadel.NextgenEngine;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionFactory;
import graphql.nadel.ServiceExecutionResult;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static benchmark.support.TestResources.readFromClasspath;
import static benchmark.support.TestResources.toServiceResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * See http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/ for more samples
 * on what you can do with JMH
 * <p>
 * You MUST have the JMH plugin for IDEA in place for this to work :  https://github.com/artyushov/idea-jmh-plugin
 * <p>
 * Install it and then just hit "Run" on a certain benchmark method
 */

@SuppressWarnings("UnstableApiUsage")
public class JiraCentralBenchmark {
    @State(Scope.Benchmark)
    public static class TestFixture {
        private final ObjectMapper objectMapper = new ObjectMapper();

        Nadel nadel;

        String query;

        AtomicInteger numExecutions = new AtomicInteger();

        Document document;

        Map<String, Object> variables = new HashMap<>() {{
            put("key", "AOEU-1");
            put("cloudId", "a4e6a8a0-0490-42b7-a42a-a7ed0221e3a7");
        }};

        @Setup
        public void setup() throws IOException {
            String underlyingSchemaString = readFromClasspath("central/underlying_central.graphqls.json");
            Map underlyingSchema = objectMapper.readValue(underlyingSchemaString, Map.class);
            for (Object key : underlyingSchema.keySet().toArray()) {
                String schemaText = (String) underlyingSchema.get(key);
                underlyingSchema.put(key, new SchemaParser().parse(schemaText));
            }

            String responseString = readFromClasspath("central/jira_agg_main_response.json");
            Map<?, ?> responseMap = objectMapper.readValue(responseString, Map.class);

            boolean kt = true;

            ServiceExecutionResult result = toServiceResult(responseMap);
            ServiceExecution serviceExecution = serviceExecutionParameters -> completedFuture(result);

            ServiceExecutionFactory serviceExecutionFactory = new ServiceExecutionFactory() {
                @Override
                public ServiceExecution getServiceExecution(String serviceName) {
                    return serviceExecution;
                }

                @Override
                public TypeDefinitionRegistry getUnderlyingTypeDefinitions(String serviceName) {
                    return (TypeDefinitionRegistry) underlyingSchema.get(serviceName);
                }
            };

            String nadelSchemaText = readFromClasspath("central/overall_central.nadel.json");
            Map<String, String> nadelSchema = objectMapper.readValue(nadelSchemaText, Map.class);
            nadel = Nadel.newNadel()
                    .engineFactory(kt ? NextgenEngine::new : NadelEngine::new)
                    .dsl(nadelSchema)
                    .serviceExecutionFactory(serviceExecutionFactory)
                    .underlyingWiringFactory(new GatewaySchemaWiringFactory())
                    .overallWiringFactory(new GatewaySchemaWiringFactory())
                    .build();
            query = readFromClasspath("central/jira_agg_main.graphql");

            document = new NadelGraphQLParser().parseDocument(query);
        }

    }

    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 4, time = 10)
    @Threads(1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Object benchMarkAvgTime(TestFixture fixture) throws ExecutionException, InterruptedException {
        NadelExecutionInput nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                .query(fixture.query)
                .variables(fixture.variables)
                .build();
        ExecutionResult result = fixture.nadel.execute(nadelExecutionInput).get();

        // For paranoid people like me who think the test is failing somewhere silently
        if (fixture.numExecutions.incrementAndGet() % 100 == 0) {
            System.err.println("At " + fixture.numExecutions.get() + " executions");
            System.out.println(result);
        }

        return result;
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        JiraCentralBenchmark benchmark = new JiraCentralBenchmark();
        TestFixture testFixture = new TestFixture();
        testFixture.setup();
        benchmark.benchMarkAvgTime(testFixture);
    }
}
