package graphql.nadel;

import graphql.ExecutionResult;
import graphql.PublicApi;
import graphql.language.Definition;
import graphql.language.SDLDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@PublicApi
public class Nadel {

    private final String nsdl;
    private final StitchingDsl stitchingDsl;
    private final ServiceDataFactory serviceDataFactory;
    private final NSDLParser NSDLParser = new NSDLParser();

    private Execution execution;
    private List<Service> services;
    private GraphQLSchema overallSchema;

    private OverallSchemaGenerator overallSchemaGenerator = new OverallSchemaGenerator();

    private Nadel(String nsdl, ServiceDataFactory serviceDataFactory) {
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
            DelegatedExecution delegatedExecution = serviceDataFactory.getDelegatedExecution(serviceName);
            GraphQLSchema privateSchema = serviceDataFactory.getPrivateSchema(serviceName);
            DefinitionRegistry definitionRegistry = buildServiceRegistry(serviceDefinition);

            Service service = new Service(serviceName, privateSchema, delegatedExecution, serviceDefinition, definitionRegistry);
            services.add(service);
        }
        this.services = services;
        this.overallSchema = overallSchemaGenerator.buildOverallSchema(services);
        this.execution = new Execution(services, overallSchema);

    }

    private DefinitionRegistry buildServiceRegistry(ServiceDefinition serviceDefinition) {
        DefinitionRegistry definitionRegistry = new DefinitionRegistry();
        for (Definition definition : serviceDefinition.getTypeDefinitions()) {
            definitionRegistry.add((SDLDefinition) definition);
        }
        return definitionRegistry;
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        // we need to actually validate the query with the normal graphql-java validation here
        return execution.execute(nadelExecutionInput);
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Builder newNadel() {
        return new Builder();
    }

    public static class Builder {
        private String nsdl;
        private ServiceDataFactory serviceDataFactory;

        public Builder dsl(String nsdl) {
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
