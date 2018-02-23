package graphql.nadel;


import graphql.PublicApi;
import graphql.language.Document;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

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
    public Object get(DataFetchingEnvironment environment) {
        String fieldName = environment.getFields().get(0).getName();
        Document query = queryCreator.createQuery(environment);
        GraphqlCallResult callResult = graphqlCaller.call(query);
        assertNotNull(callResult, "call result can't be null");
        return callResult.getData().get(fieldName);
    }

}
