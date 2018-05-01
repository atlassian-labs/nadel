package graphql.nadel;

import com.atlassian.braid.Braid;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.source.GraphQLRemoteRetriever;
import com.atlassian.braid.source.GraphQLRemoteSchemaSource;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.TypeDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@PublicApi
public class Nadel {

    private final String dsl;
    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory;
    private Braid braid;

    public Nadel(String dsl, GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory) {
        this.dsl = dsl;
        this.graphQLRemoteRetrieverFactory = graphQLRemoteRetrieverFactory;
        this.stitchingDsl = this.parser.parseDSL(dsl);
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();
        List<SchemaSource> schemaSources = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            GraphQLRemoteRetriever remoteRetriever = this.graphQLRemoteRetrieverFactory.createRemoteRetriever(serviceDefinition);
            SchemaSource schemaSource = createSchemaSource(typeDefinitionRegistry, schemaNamespace, remoteRetriever);
            schemaSources.add(schemaSource);
        }
        AsyncExecutionStrategy asyncExecutionStrategy = new AsyncExecutionStrategy();
        this.braid = Braid.builder()
                .executionStrategy(asyncExecutionStrategy)
                .schemaSources(schemaSources)
                .build();
    }

    SchemaSource createSchemaSource(TypeDefinitionRegistry typeDefinitionRegistry, SchemaNamespace schemaNamespace, GraphQLRemoteRetriever<Object> remoteRetriever) {
        return new GraphQLRemoteSchemaSource<>(schemaNamespace,
                typeDefinitionRegistry,
                typeDefinitionRegistry,
                remoteRetriever,
                Collections.emptyList(),
                DocumentMappers.identity());
    }

    public TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
        List<GraphQLError> errors = new ArrayList<>();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (TypeDefinition definition : serviceDefinition.getTypeDefinitions()) {
            typeRegistry.add(definition).ifPresent(errors::add);
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }


    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
//        return this.graphql.executeAsync(executionInput);
//        return null;
    }

}
