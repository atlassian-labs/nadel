package graphql.nadel;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.language.Definition;
import graphql.language.SDLDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@PublicApi
public class Nadel {
    private final StitchingDsl stitchingDsl;
    private final Parser parser = new Parser();

    private final Map<String, TypeDefinitionRegistry> typesByService = new LinkedHashMap<>();

    private Nadel(String nsdl) {
        this.stitchingDsl = this.parser.parseDSL(nsdl);

        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.typesByService.put(serviceDefinition.getName(), typeDefinitionRegistry);

        }
    }

    public TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
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
        return null;
    }

    /**
     * @return a builder of Nadel objects
     */
    public static Builder newNadel() {
        return new Builder();
    }

    public static class Builder {
        private String nsdl;

        public Builder dsl(String nsdl) {
            this.nsdl = requireNonNull(nsdl);
            return this;
        }


        public Nadel build() {
            return new Nadel(nsdl);
        }
    }
}
