package graphql.nadel;

import graphql.ExecutionResult;
import graphql.PublicApi;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.GraphQLSchema;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static graphql.nadel.Util.buildServiceRegistry;
import static java.util.Objects.requireNonNull;

@PublicApi
public class Nadel {

    private final Reader nsdl;
    private final StitchingDsl stitchingDsl;
    private final ServiceDataFactory serviceDataFactory;
    private final NSDLParser NSDLParser = new NSDLParser();

    private Execution execution;
    private List<Service> services;
    private GraphQLSchema overallSchema;

    private OverallSchemaGenerator overallSchemaGenerator = new OverallSchemaGenerator();

    private Nadel(Reader nsdl, ServiceDataFactory serviceDataFactory) {
        this.nsdl = nsdl;
        this.stitchingDsl = this.NSDLParser.parseDSL(nsdl);
        this.serviceDataFactory = serviceDataFactory;
        this.init();
    }

    private void init() {
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        List<Service> services = new ArrayList<>();


        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            String serviceName = serviceDefinition.getName();
            ServiceExecution serviceExecution = serviceDataFactory.getDelegatedExecution(serviceName);
            GraphQLSchema underlyingSchema = serviceDataFactory.getUnderlyingSchema(serviceName);
            DefinitionRegistry definitionRegistry = buildServiceRegistry(serviceDefinition);

            Service service = new Service(serviceName, underlyingSchema, serviceExecution, serviceDefinition, definitionRegistry);
            services.add(service);
        }
        this.services = services;
        List<DefinitionRegistry> registries = services.stream()
                .map(Service::getDefinitionRegistry)
                .collect(Collectors.toList());
        this.overallSchema = overallSchemaGenerator.buildOverallSchema(registries);
        this.execution = new Execution(services, overallSchema);

    }


    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        // we need to actually validate the query with the normal graphql-java validation here
        return execution.execute(nadelExecutionInput);
    }

    public GraphQLSchema getOverallSchema() {
        return overallSchema;
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Builder newNadel() {
        return new Builder();
    }

    public static class Builder {
        private Reader nsdl;
        private ServiceDataFactory serviceDataFactory;

        public Builder dsl(Reader nsdl) {
            this.nsdl = requireNonNull(nsdl);
            return this;
        }

        public Builder serviceDataFactory(ServiceDataFactory serviceDataFactory) {
            this.serviceDataFactory = serviceDataFactory;
            return this;
        }


        public Nadel build() {
            return new Nadel(nsdl, serviceDataFactory);
        }
    }
}
