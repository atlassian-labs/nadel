package graphql.nadel;


import graphql.PublicApi;
import graphql.language.Document;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

@PublicApi
public class RemoteRootQueryDataFetcher implements DataFetcher {

    private GraphqlCaller graphqlCaller;
    private RootQueryCreator queryCreator;
    private ServiceDefinition serviceDefinition;

    public RemoteRootQueryDataFetcher(ServiceDefinition serviceDefinition) {
        this.serviceDefinition = serviceDefinition;
        this.queryCreator = new RootQueryCreator();
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        String fieldName = environment.getFields().get(0).getName();
        Document query = queryCreator.createQuery(environment);
        GraphqlCaller.GraphqlCallResult callResult = graphqlCaller.call(query);
        return callResult.data.get(fieldName);
    }

}
