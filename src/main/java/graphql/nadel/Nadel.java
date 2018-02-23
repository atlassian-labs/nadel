package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.PublicApi;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.GraphQLSchema;

import java.util.concurrent.CompletableFuture;

@PublicApi
public class Nadel {

    private final String dsl;
    private final GraphQLSchema schema;
    private final GraphQL graphql;
    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private GraphqlCallerFactory graphqlCallerFactory;

    public Nadel(String dsl, GraphqlCallerFactory graphqlCallerFactory) {
        this.graphqlCallerFactory = graphqlCallerFactory;
        this.dsl = dsl;
        this.stitchingDsl = this.parser.parseDSL(dsl);
        NadelTypeDefinitionRegistry nadelTypeDefinitionRegistry = new NadelTypeDefinitionRegistry(stitchingDsl);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        this.schema = schemaGenerator.makeExecutableSchema(nadelTypeDefinitionRegistry, graphqlCallerFactory);
        this.graphql = GraphQL.newGraphQL(this.schema).build();
    }

    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.graphql.executeAsync(executionInput);
    }

}
