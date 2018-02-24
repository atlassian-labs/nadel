package graphql.nadel;


import graphql.PublicApi;
import graphql.execution.DataFetcherResult;
import graphql.language.Document;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;

@PublicApi
public class RemoteRootQueryDataFetcher implements DataFetcher {

    private GraphqlCaller graphqlCaller;
    private RootQueryCreator queryCreator;
    private ServiceDefinition serviceDefinition;

    public RemoteRootQueryDataFetcher(ServiceDefinition serviceDefinition, GraphqlCaller graphqlCaller, StitchingDsl stitchingDsl) {
        this.serviceDefinition = assertNotNull(serviceDefinition);
        this.queryCreator = new RootQueryCreator(serviceDefinition, stitchingDsl);
        this.graphqlCaller = assertNotNull(graphqlCaller);
    }

    @Override
    public CompletableFuture<DataFetcherResult> get(DataFetchingEnvironment environment) {
        String fieldName = environment.getField().getName();
        Document query = queryCreator.createQuery(environment);
        CompletableFuture<GraphqlCallResult> callResultFuture = graphqlCaller.call(query);
        assertNotNull(callResultFuture, "call result can't be null");
        return callResultFuture.thenApply(callResult -> new DataFetcherResult<>(callResult.getData().get(fieldName), callResult.getErrors()));
    }

}
