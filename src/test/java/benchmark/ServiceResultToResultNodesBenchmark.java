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
import graphql.nadel.engine.ServiceExecutor;
import graphql.nadel.engine.ServiceResultToResultNodes;
import graphql.nadel.engine.ServiceResultToResultNodesMutable;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class ServiceResultToResultNodesBenchmark {

    @State(Scope.Benchmark)
    public static class NadelInstance {
        Nadel nadel;
        String query;
        String json;
        ObjectMapper objectMapper;
        ServiceExecutor.DebugContext debugContext;

        ServiceResultToResultNodes serviceResultToResultNodes = new ServiceResultToResultNodes();
        ServiceResultToResultNodesMutable serviceResultToResultNodesMutable = new ServiceResultToResultNodesMutable();

        @Setup
        public void setup() throws IOException, ExecutionException, InterruptedException {

            objectMapper = new ObjectMapper();
            String schemaString = readFromClasspath("large_response_benchmark_schema.graphqls");
            TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaString);

            this.json = readFromClasspath("large_underlying_service_result.json");
            ServiceExecutionFactory serviceExecutionFactory = new ServiceExecutionFactory() {
                @Override
                public ServiceExecution getServiceExecution(String serviceName) {
                    Map responseMap = null;
                    try {
                        responseMap = objectMapper.readValue(json, Map.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    ServiceExecutionResult serviceExecutionResult = new ServiceExecutionResult((Map<String, Object>) responseMap.get("data"));
                    ServiceExecution serviceExecution = serviceExecutionParameters -> CompletableFuture.completedFuture(serviceExecutionResult);
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
            debugContext = new ServiceExecutor.DebugContext();
            NadelExecutionInput nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                    .forkJoinPool(ForkJoinPool.commonPool())
                    .context(debugContext)
                    .query(query)
                    .build();
            ExecutionResult executionResult = nadel.execute(nadelExecutionInput).get();

        }

        private String readFromClasspath(String file) throws IOException {
            URL url = Resources.getResource(file);
            return Resources.toString(url, Charsets.UTF_8);
        }
    }


    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    @Fork(3)
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public graphql.execution.nextgen.result.RootExecutionResultNode benchMarkAvgTime(NadelInstance nadelInstance) throws ExecutionException, InterruptedException {
        ServiceExecutor.DebugContext debugContext = nadelInstance.debugContext;
        graphql.execution.nextgen.result.RootExecutionResultNode rootExecutionResultNode = nadelInstance.serviceResultToResultNodes.resultToResultNode(
                debugContext.executionContextForService,
                debugContext.underlyingRootStepInfo,
                debugContext.transformedMergedFields,
                debugContext.serviceExecutionResult);
        return rootExecutionResultNode;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        NadelInstance nadelInstance = new NadelInstance();
        nadelInstance.setup();
        ServiceExecutor.DebugContext debugContext = nadelInstance.debugContext;
        long hashValue = 0;
        for (int i = 0; i < 100000000; i++) {
            graphql.nadel.execution.RootExecutionResultNode rootExecutionResultNode = nadelInstance.serviceResultToResultNodesMutable.resultToResultNode(
                    debugContext.executionContextForService,
                    debugContext.underlyingRootStepInfo,
                    debugContext.transformedMergedFields,
                    debugContext.serviceExecutionResult);
            hashValue = hashValue - rootExecutionResultNode.hashCode();
        }
        System.out.println(hashValue);
    }


}
