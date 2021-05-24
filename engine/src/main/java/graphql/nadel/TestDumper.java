package graphql.nadel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.language.AstPrinter;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.LITERAL_BLOCK_STYLE;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

public class TestDumper {
    private static Map<String, String> overallSchema;
    private static List<Service> services;
    private static ExecutionInput executionInput;
    private static List<Map.Entry<ServiceExecutionParameters, ServiceExecutionResult>> serviceCalls = new ArrayList<>();
    private static ExecutionResult response;
    private static Throwable throwable;

    public static Map<String, String> getOverallSchema() {
        return overallSchema;
    }

    public static List<Service> getServices() {
        return services;
    }

    public static ExecutionInput getExecutionInput() {
        return executionInput;
    }

    public static List<Map.Entry<ServiceExecutionParameters, ServiceExecutionResult>> getServiceCalls() {
        return serviceCalls;
    }

    public static ExecutionResult getResponse() {
        return response;
    }

    public static Throwable getThrowable() {
        return throwable;
    }

    public static void setOverallSchema(Map<String, String> overallSchema) {
        if (TestDumper.overallSchema != null) {
            throw new IllegalStateException("overallSchema has already been set for current test");
        }
        HashMap<String, String> copiedMap = new HashMap<>(overallSchema.size());
        overallSchema.forEach((key, value) -> {
            copiedMap.put(key, reformatSchema(value));
        });
        TestDumper.overallSchema = copiedMap;
    }

    private static String reformatSchema(String value) {
        int indentLevel = 0;
        StringBuilder builder = new StringBuilder();
        String[] lines = value.split("\n");
        for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
            String s = lines[i];
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.endsWith("}")) {
                indentLevel--;
            }
            String indent = repeat("  ", indentLevel);
            builder.append(indent).append(line).append('\n');
            if (line.endsWith("{")) {
                indentLevel++;
            }
        }
        return builder.toString();
    }

    public static void setServices(List<Service> services) {
        if (TestDumper.services != null) {
            throw new IllegalStateException("services has already been set for current test");
        }
        TestDumper.services = services;
    }

    public static void setExecutionInput(ExecutionInput query) {
        if (TestDumper.executionInput != null) {
            throw new IllegalStateException("executionInput has already been set for current test");
        }
        TestDumper.executionInput = query;
    }

    public static void addServiceCall(ServiceExecutionParameters params, ServiceExecutionResult result) {
        addServiceCall(new AbstractMap.SimpleEntry<>(params, result));
    }

    public static void addServiceCall(Map.Entry<ServiceExecutionParameters, ServiceExecutionResult> serviceCall) {
        TestDumper.serviceCalls.add(serviceCall);
    }

    public static void setResponse(ExecutionResult response) {
        if (TestDumper.response != null) {
            throw new IllegalStateException("response has already been set for current test");
        }
        TestDumper.response = response;
    }

    public static void setThrowable(Throwable throwable) {
        TestDumper.throwable = throwable;
    }

    public static void reset() {
        TestDumper.overallSchema = null;
        TestDumper.services = null;
        TestDumper.executionInput = null;
        TestDumper.serviceCalls = new ArrayList<>();
        TestDumper.response = null;
        TestDumper.throwable = null;
    }

    public static void dump(String testName) {
        System.out.println("\n\n\n  ___      " + repeat(" ", testName.length()) + "___  \n" +
                " (o o)    " + repeat(" ", testName.length()) + "(o o) \n" +
                "(  V  ) " + testName + " (  V  )\n" +
                "--m-m" + repeat("-", testName.length()) + "------m-m--\n\n\n");

        Objects.requireNonNull(overallSchema);
        Objects.requireNonNull(services);
        Objects.requireNonNull(executionInput);
        Objects.requireNonNull(serviceCalls);
        Objects.requireNonNull(response);

        printSectionHeader("Overall schema");
        // System.out.println(schemaPrinter.print(overallSchema));

        printSectionHeader("Service schema");
        for (Service service : services) {
            printSubsectionHeader("Service " + service.getName());
            GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
            System.out.println(schemaPrinter.print(underlyingSchema));
        }

        printSectionHeader("Service schema");
        String processedQuery = AstPrinter.printAst(
                Parser.parse(
                        executionInput.getQuery()
                )
        );
        System.out.println(
                processedQuery
        );

        printSectionHeader("Underlying service calls");
        for (Map.Entry<ServiceExecutionParameters, ServiceExecutionResult> serviceCall : serviceCalls) {
            ServiceExecutionParameters serviceCallParams = serviceCall.getKey();
            ServiceExecutionResult serviceCallResult = serviceCall.getValue();

            printSubsectionHeader("Service call query");
            Document serviceQueryDocument = serviceCallParams.getQuery();
            System.out.println(AstPrinter.printAst(serviceQueryDocument));

            printSubsectionHeader("Service call response");
            System.out.println(toJsonString(serviceCallResult));
        }

        System.out.println("\n");
        printSectionHeader("Overall Result");
        System.out.println(toJsonString(response));

        System.out.println("\n\n\n ___           _       \n" +
                "| __| _ _   __| |      \n" +
                "| _| | ' \\ / _` |      \n" +
                "|___||_||_|\\__/_|      \n");

        try {
            String testDump = yamlObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new TestFixture(
                    testName,
                    overallSchema,
                    new HashMap<String, String>() {
                        {
                            for (Service service : services) {
                                GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
                                put(service.getName(), schemaPrinter.print(underlyingSchema));
                            }
                        }
                    },
                    processedQuery,
                    executionInput.getVariables(),
                    serviceCalls.stream().map((serviceCall) -> {
                        ServiceExecutionParameters serviceExecParams = serviceCall.getKey();
                        String serviceQuery = AstPrinter.printAst(
                                serviceExecParams.getQuery()
                        );
                        return new TestServiceCall(
                                new TestServiceCall.Request(
                                        serviceQuery,
                                        serviceExecParams.getVariables(),
                                        serviceExecParams.getOperationDefinition().getName()
                                ),
                                toJsonString(
                                        serviceCall.getValue()
                                )
                        );
                    }).collect(Collectors.toList()),
                    toJson(response.toSpecification())
            ));

            String slugTestName = toSlug(testName);
            File outputFile = new File("/Users/fwang/Documents/GraphQL/nadel/test/src/test/resources/fixtures/" + slugTestName + ".yml");
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(testDump);
            }

            System.out.println(testDump);
        } catch (IOException e) {
            throw new RuntimeException("I hate checked exceptions", e);
        }
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private static String toJsonString(ExecutionResult response) {
        return toJson(response.toSpecification());
    }

    private static Map<String, Object> toJsonMap(ServiceExecutionResult result) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();

        List<Map<String, Object>> errors = result.getErrors();
        if (errors != null && !errors.isEmpty()) {
            jsonMap.put("errors", errors);
        }

        // No way to see if data is present, who cares
        jsonMap.put("data", result.getData());

        Map<String, Object> extensions = result.getExtensions();
        if (extensions != null) {
            jsonMap.put("extensions", extensions);
        }

        return jsonMap;
    }

    private static String toJsonString(ServiceExecutionResult result) {
        Map<String, Object> jsonMap = toJsonMap(result);

        return toJson(jsonMap);
    }

    private static String toJson(Object object) {
        try {
            DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
            pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
            pp.indentObjectsWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

            return objectMapper.writer(pp).writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("I hate checked exceptions", e);
        }
    }

    private static void printSubsectionHeader(String title) {
        System.out.println("\n\n" + repeat("=", title.length()));
        System.out.println(title + "\n\n");
    }

    private static void printSectionHeader(String title) {
        System.out.println(" / \\-------" + repeat("-", title.length()) + "-,\n" +
                " \\_,|       " + repeat(" ", title.length()) + "|\n" +
                "    |    " + title + "   |\n" +
                "    |  ,---" + repeat("-", title.length()) + "---\n" +
                "    \\_/__" + repeat("_", title.length()) + "___/\n");
    }

    private static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder(text.length() * times);
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory()
            .enable(LITERAL_BLOCK_STYLE)
            .enable(MINIMIZE_QUOTES)
            .disable(WRITE_DOC_START_MARKER));

    private static final SchemaPrinter schemaPrinter = new SchemaPrinter(
            SchemaPrinter.Options.defaultOptions()
                    .includeDirectives((graphQLDirective) -> {
                        return !(
                                graphQLDirective.getName().equals("include") ||
                                        graphQLDirective.getName().equals("skip") ||
                                        graphQLDirective.getName().equals("deprecated") ||
                                        graphQLDirective.getName().equals("specifiedBy") ||
                                        graphQLDirective.getName().equals("hydrated") ||
                                        graphQLDirective.getName().equals("renamed")
                        );
                    })
                    .includeSchemaElement((graphQLSchemaElement) -> {
                        if (graphQLSchemaElement instanceof GraphQLInputObjectType) {
                            GraphQLInputObjectType type = (GraphQLInputObjectType) graphQLSchemaElement;
                            return !type.getName().equals("NadelHydrationArgument");
                        }

                        return true;
                    })
    );

    static class TestFixture {
        private final String name;
        private final Map<String, String> overallSchema;
        private final Map<String, String> services;
        private final String query;
        private final Map<String, Object> variables;
        private final List<TestServiceCall> calls;
        private final String response;

        public String getName() {
            return name;
        }

        public Map<String, String> getOverallSchema() {
            return overallSchema;
        }

        public Map<String, String> getServices() {
            return services;
        }

        public String getQuery() {
            return query;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public List<TestServiceCall> getCalls() {
            return calls;
        }

        public String getResponse() {
            return response;
        }

        private TestFixture(
                String name,
                Map<String, String> overallSchema,
                Map<String, String> services,
                String query,
                Map<String, Object> variables,
                List<TestServiceCall> calls,
                String response
        ) {
            this.name = name;
            this.overallSchema = overallSchema;
            this.services = services;
            this.query = query;
            this.variables = variables;
            this.calls = calls;
            this.response = response;
        }
    }

    static class TestServiceCall {
        private final Request request;
        private final String response;

        public Request getRequest() {
            return request;
        }

        public String getResponse() {
            return response;
        }

        TestServiceCall(Request request, String response) {
            this.request = request;
            this.response = response;
        }

        static class Request {
            private final String query;
            private final Map<String, Object> variables;
            private final String operationName;

            public String getQuery() {
                return query;
            }

            public Map<String, Object> getVariables() {
                return variables;
            }

            public String getOperationName() {
                return operationName;
            }

            private Request(String query, Map<String, Object> variables, String operationName) {
                this.query = query;
                this.variables = variables;
                this.operationName = operationName;
            }
        }
    }
}
