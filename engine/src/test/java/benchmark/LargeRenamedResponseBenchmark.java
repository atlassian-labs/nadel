package benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.nadel.Nadel;
import graphql.nadel.NadelEngine;
import graphql.nadel.NadelExecutionInput;
import graphql.nadel.NextgenEngine;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionFactory;
import graphql.nadel.ServiceExecutionResult;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.jetbrains.annotations.NotNull;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * See http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/ for more samples
 * on what you can do with JMH
 * <p>
 * You MUST have the JMH plugin for IDEA in place for this to work :  https://github.com/artyushov/idea-jmh-plugin
 * <p>
 * Install it and then just hit "Run" on a certain benchmark method
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LargeRenamedResponseBenchmark {
    @State(Scope.Benchmark)
    public static class TestFixture {
        private final ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Map> underlyingWorkedOn = new HashMap<>();
        Map<String, Map> underlyingViewed = new HashMap<>();

        Nadel nadel;

        String query;

        AtomicInteger numExecutions = new AtomicInteger();

        @Setup
        public void setup() throws IOException {
            String schemaString = readFromClasspath("large_response_benchmark_schema.graphqls");
            TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaString);

            String responseString = readFromClasspath("large_underlying_service_result.json");
            Map responseMap = objectMapper.readValue(responseString, Map.class);

            String ktResponseString = readFromClasspath("nadel_kt_large_renamed_response.json");
            Map ktResponseMap = objectMapper.readValue(ktResponseString, Map.class);

            boolean kt = true;

            ServiceExecution serviceExecution = serviceExecutionParameters -> {
                ServiceExecutionResult result;
                if (kt) {
                    result = new ServiceExecutionResult(
                            (Map<String, Object>) deepClone(ktResponseMap.get("data"))
                    );
                } else {
                    result = new ServiceExecutionResult(
                            (Map<String, Object>) deepClone(responseMap.get("data"))
                    );
                }
                return CompletableFuture.completedFuture(result);
            };

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

            String nsdl = readFromClasspath("renamed_large_response_benchmark_schema.nadel");
            nadel = Nadel.newNadel()
                    .engineFactory(kt ? NextgenEngine::new : NadelEngine::new)
                    .dsl("activity", nsdl)
                    .serviceExecutionFactory(serviceExecutionFactory)
                    .build();
            query = readFromClasspath("large_renamed_response_benchmark_query.graphql");

            List<Map> workedOnNodes = (List) getAt(responseMap, "data", "myActivities", "workedOn", "nodes");
            for (Map workedOnNode : workedOnNodes) {
                underlyingWorkedOn.put((String) workedOnNode.get("id"), (Map) deepClone(workedOnNode));
            }
            assertFalse(workedOnNodes.isEmpty());

            List<Map> viewedNodes = (List) getAt(responseMap, "data", "myActivities", "viewed", "nodes");
            for (Map viewedNode : viewedNodes) {
                underlyingViewed.put((String) viewedNode.get("id"), (Map) deepClone(viewedNode));
            }
            assertFalse(viewedNodes.isEmpty());
        }

        private File getResourceFile(String resource) {
            URL resourceUri = getClass().getClassLoader().getResource(resource);
            String path = Objects.requireNonNull(resourceUri, "Resource does not exist")
                    .toString()
                    .replace(resourceUri.getProtocol() + ":", "");

            return new File(path);
        }

        @NotNull
        private String readFromClasspath(String resource) throws IOException {
            File file = getResourceFile(resource);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            StringBuilder overall = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                overall.append(line).append('\n');
            }

            return overall.toString();
        }

        private static Object deepClone(Object input) {
            if (input instanceof Map) {
                Map<Object, Object> map = (Map) input;
                Map cloned = new HashMap(map.size());
                for (Map.Entry o : map.entrySet()) {
                    cloned.put(
                            o.getKey(),
                            deepClone(o.getValue())
                    );
                }
                return cloned;
            } else if (input instanceof List) {
                List list = (List) input;
                List cloned = new ArrayList(list.size());
                for (Object o : list) {
                    cloned.add(deepClone(o));
                }
                return cloned;
            } else {
                return input;
            }
        }
    }

    @Benchmark
    @Warmup(iterations = 2)
    @Measurement(iterations = 4, time = 10)
    @Threads(8)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public ExecutionResult benchMarkAvgTime(TestFixture fixture) throws ExecutionException, InterruptedException {
        NadelExecutionInput nadelExecutionInput = NadelExecutionInput.newNadelExecutionInput()
                .query(fixture.query)
                .build();
        ExecutionResult result = fixture.nadel.execute(nadelExecutionInput).get();

        assertTrue(result.getErrors().isEmpty());

        List workedOnNodes = (List) getAt(result.getData(), "myActivitiez", "workedOnz", "nodez");
        assertEquals(fixture.underlyingWorkedOn.size(), workedOnNodes.size());
        runValidation(workedOnNodes, fixture.underlyingWorkedOn);

        List viewedNodes = (List) getAt(result.getData(), "myActivitiez", "viewedz", "nodez");
        assertEquals(fixture.underlyingViewed.size(), viewedNodes.size());
        runValidation(viewedNodes, fixture.underlyingViewed);

        // For paranoid people like me who think the test is failing somewhere silently
        if (fixture.numExecutions.incrementAndGet() % 1000 == 0) {
            System.err.println("At " + fixture.numExecutions.get() + " executions");
        }

        // System.out.println("data:" + result.getData());
        return result;
    }

    private static void runValidation(List nodes, Map<String, Map> underlyingNodes) {
        for (Object nodeObj : nodes) {
            Map node = (Map) nodeObj;
            String idz = (String) node.get("idz");
            assertNotNull(idz);

            assertNotNull(node.get("timestampz"));
            assertFalse(node.containsKey("timestamp"));

            Map objectz = (Map) node.get("objectz");
            assertNotNull(objectz.get("idz"));
            assertNotNull(objectz.get("namez"));
            assertNotNull(objectz.get("cloudIDz"));
            assertNotNull(objectz.get("urlz"));
            assertNotNull(objectz.get("iconURLz"));

            assertFalse(objectz.containsKey("id"));
            assertFalse(objectz.containsKey("name"));
            assertFalse(objectz.containsKey("cloudID"));
            assertFalse(objectz.containsKey("url"));
            assertFalse(objectz.containsKey("iconURL"));

            assertEquals(node.get("id"), objectz.get("idz"));
            assertEquals(node.get("name"), objectz.get("namez"));
            assertEquals(node.get("cloudId"), objectz.get("cloudIDz"));
            assertEquals(node.get("url"), objectz.get("urlz"));

            assertTrue(node.containsKey("contributorz"));
            List<Map> contributorz = (List) node.get("contributorz");
            // These are null in the response
            if (contributorz != null) {
                for (Map contributor : contributorz) {
                    assertTrue(contributor.containsKey("profilez"));
                    assertFalse(contributor.containsKey("profile"));

                    Map profile = (Map) contributor.get("profilez");
                    assertNotNull(profile.get("accountIdz"));
                    assertFalse(profile.containsKey("accountId"));
                }
            }

            assertTrue(node.containsKey("containerz"));
            List<Map> containerz = (List) node.get("containerz");
            for (Map container : containerz) {
                assertTrue(container.containsKey("namez"));
                assertFalse(container.containsKey("name"));
            }

            Map underlying = underlyingNodes.get(idz);
            assertEquals(underlying.get("id"), node.get("idz"));
            assertEquals(underlying.get("timestamp"), node.get("timestampz"));

            Map object = (Map) underlying.get("object");
            assertEquals(object.get("id"), node.get("id"));
            assertEquals(object.get("name"), node.get("name"));
            assertEquals(object.get("cloudID"), node.get("cloudId"));
            assertEquals(object.get("url"), node.get("url"));

            Set underlyingContainerNames = mapToSet((List) underlying.get("containers"), (container) -> ((Map) container).get("name"));
            Set containerzNames = mapToSet(containerz, (container) -> ((Map) container).get("namez"));
            assertEquals(underlyingContainerNames, containerzNames);

            List<Map> underlyingContributors = (List) underlying.get("contributors");

            if (contributorz == null) {
                assertNull(underlyingContributors);
            } else {
                assertEquals(underlyingContributors.size(), contributorz.size());
                Map<String, Map> contributorzById = getProfilesById(contributorz, true);
                Map<String, Map> underlyingContributorsById = getProfilesById(underlyingContributors, false);
                assertEquals(underlyingContributorsById.size(), contributorzById.size());

                for (Map.Entry<String, Map> entry : contributorzById.entrySet()) {
                    Map profilez = entry.getValue();
                    Map underlyingProfile = underlyingContributorsById.get(entry.getKey());
                    assertEquals(underlyingProfile.size(), profilez.size());
                    assertEquals(underlyingProfile.get("accountId"), profilez.get("accountIdz"));
                    assertEquals(underlyingProfile.get("name"), profilez.get("namez"));
                    assertEquals(underlyingProfile.get("picture"), profilez.get("picturez"));
                }
            }
        }
    }

    private static Map<String, Map> getProfilesById(List<Map> contributors, boolean z) {
        Map<String, Map> profilesById = new HashMap(contributors.size());
        for (Map map : contributors) {
            Map profile = (Map) map.get(z ? "profilez" : "profile");
            if (profile != null) {
                profilesById.put((String) profile.get(z ? "accountIdz" : "accountId"), profile);
            }
        }
        return profilesById;
    }

    private static HashSet mapToSet(List input, Function<Object, Object> mapper) {
        HashSet output = new HashSet<>(input.size());

        for (Object o : input) {
            output.add(mapper.apply(o));
        }

        return output;
    }

    private static Object getAt(Map data, Object... path) {
        Object current = data;

        for (Object segment : path) {
            if (current instanceof Map) {
                Map map = (Map) current;
                if (map.get(segment) == null) {
                    throw new UnsupportedOperationException(current.toString());
                }
                current = map.get((String) segment);
            } else if (current instanceof List) {
                List list = (List) current;
                current = list.get((int) segment);
            } else {
                fail("Unable to find data: " + segment);
            }
        }

        return current;
    }
}
