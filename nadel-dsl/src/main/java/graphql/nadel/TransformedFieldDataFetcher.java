package graphql.nadel;

import graphql.execution.DataFetcherResult;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.validation.ValidationUtil;

import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;

public class TransformedFieldDataFetcher implements DataFetcher {

    private GraphqlCallerFactory graphqlCallerFactory;
    private StitchingDsl stitchingDsl;
    private ValidationUtil validationUtil = new ValidationUtil();

    public TransformedFieldDataFetcher(GraphqlCallerFactory graphqlCallerFactory, StitchingDsl stitchingDsl) {
        this.graphqlCallerFactory = graphqlCallerFactory;
        this.stitchingDsl = stitchingDsl;
    }


    @Override
    public CompletableFuture<DataFetcherResult> get(DataFetchingEnvironment environment) {
        FieldTransformation fieldTransformation = this.stitchingDsl.getTransformationsByFieldDefinition().get(environment.getFieldDefinition().getDefinition());
        assertNotNull(fieldTransformation, "expect field transformation");
        TransformedFieldQueryCreator transformedFieldQueryCreator = new TransformedFieldQueryCreator(environment.getFieldDefinition().getDefinition(), fieldTransformation, stitchingDsl);
        TransformedFieldQueryCreator.QueryForTransformedField query = transformedFieldQueryCreator.createQuery(environment);

        // todo: don't create new factory every time
        GraphqlCaller graphqlCaller = graphqlCallerFactory.createGraphqlCaller(query.targetService);
        CompletableFuture<GraphqlCallResult> callResultFuture = graphqlCaller.call(query.query);
        assertNotNull(callResultFuture, "callResult can't be null");
        return callResultFuture.thenApply(callResult -> new DataFetcherResult<>(callResult.getData().get(query.rootFieldName), callResult.getErrors()));
    }
}
