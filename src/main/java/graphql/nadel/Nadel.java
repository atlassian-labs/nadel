package graphql.nadel;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.language.Definition;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;

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


    private Nadel(String nsdl, ServiceDataFactory serviceDataFactory) {
        this.nsdl = nsdl;
        this.stitchingDsl = this.NSDLParser.parseDSL(nsdl);
        this.serviceDataFactory = serviceDataFactory;
        this.init();
    }

    private void init() {
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        List<Service> services = new ArrayList<>();

        TypeDefinitionRegistry overallRegistry = new TypeDefinitionRegistry();
        overallRegistry.add(new ObjectTypeDefinition("Query"));

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            String serviceName = serviceDefinition.getName();
            DelegatedExecution delegatedExecution = serviceDataFactory.getDelegatedExecution(serviceName);
            GraphQLSchema privateSchema = serviceDataFactory.getPrivateSchema(serviceName);
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);

            Service service = new Service(serviceName, privateSchema, delegatedExecution, serviceDefinition, typeDefinitionRegistry);
            services.add(service);
            mergeRegistries(overallRegistry, typeDefinitionRegistry);
        }
        this.services = services;
        this.overallSchema = buildSchema(overallRegistry);
        this.execution = new Execution(services, overallSchema);

    }

    private void mergeRegistries(TypeDefinitionRegistry target, TypeDefinitionRegistry source) {
        ObjectTypeDefinition currentQueryType = Util.getQueryType(target);
        ObjectTypeDefinition sourceQueryType = Util.getQueryType(source);
        for (FieldDefinition addField : sourceQueryType.getFieldDefinitions()) {
            currentQueryType = currentQueryType.transform(builder -> builder.fieldDefinition(addField));
        }
        //TODO: this is just ugly that we remove sourceQueryType and then add it again
        source.remove(sourceQueryType);
        target.remove(currentQueryType);
        target.add(currentQueryType);
        target.merge(source);
        source.add(sourceQueryType);
    }

    private GraphQLSchema buildSchema(TypeDefinitionRegistry typeDefinitionRegistry) {
        //TODO: This will not work for Unions and interfaces as they require TypeResolver
        // need to loose this requirement or add dummy versions
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    private TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
        List<GraphQLError> errors = new ArrayList<>();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (Definition definition : serviceDefinition.getTypeDefinitions()) {
            if (definition instanceof SDLDefinition) {
                typeRegistry.add((SDLDefinition) definition).ifPresent(errors::add);
            }
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
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
