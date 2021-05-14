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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestDumper {
    private static GraphQLSchema overallSchema;
    private static List<Service> services;
    private static ExecutionInput executionInput;
    private static List<Map.Entry<ServiceExecutionParameters, ServiceExecutionResult>> serviceCalls = new ArrayList<>();
    private static ExecutionResult response;
    private static Throwable throwable;

    public static GraphQLSchema getOverallSchema() {
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

    public static void setOverallSchema(GraphQLSchema overallSchema) {
        if (TestDumper.overallSchema != null) {
            throw new IllegalStateException("overallSchema has already been set for current test");
        }
        TestDumper.overallSchema = overallSchema;
    }

    public static void setServices(List<Service> services) {
        if (TestDumper.services != null) {
            throw new IllegalStateException("services has already been set for current test");
        }
        TestDumper.services = services;
    }

    public static void setExecutionInput(ExecutionInput query) {
        if (TestDumper.executionInput != null) {
            throw new IllegalStateException("query has already been set for current test");
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

        printSectionHeader("Overall schema");
        System.out.println(new SchemaPrinter(schemaPrinterOptions).print(overallSchema));

        printSectionHeader("Service schema");
        for (Service service : services) {
            printSubsectionHeader("Service " + service.getName());
            GraphQLSchema underlyingSchema = service.getUnderlyingSchema();
            System.out.println(new SchemaPrinter(schemaPrinterOptions).print(underlyingSchema));
        }

        printSectionHeader("Service schema");
        System.out.println(
                AstPrinter.printAst(
                        Parser.parse(
                                executionInput.getQuery()
                        )
                )
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
    }

    private static String toJsonString(ExecutionResult response) {
        return toJson(response.toSpecification());
    }

    private static String toJsonString(ServiceExecutionResult result) {
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

    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    private static final SchemaPrinter.Options schemaPrinterOptions = SchemaPrinter.Options.defaultOptions()
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
            });
}
