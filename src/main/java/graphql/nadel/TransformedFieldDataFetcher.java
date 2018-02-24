package graphql.nadel;

import graphql.execution.DataFetcherResult;
import graphql.language.Document;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;

public class TransformedFieldDataFetcher implements DataFetcher {

    private GraphqlCaller graphqlCaller;
    private StitchingDsl stitchingDsl;

    public TransformedFieldDataFetcher(GraphqlCaller graphqlCaller, StitchingDsl stitchingDsl) {
        this.graphqlCaller = graphqlCaller;
        this.stitchingDsl = stitchingDsl;
    }


    @Override
    public CompletableFuture<DataFetcherResult> get(DataFetchingEnvironment environment) {
        FieldTransformation fieldTransformation = this.stitchingDsl.getTransformationsByFieldDefinition().get(environment.getFieldDefinition().getDefinition());
        assertNotNull(fieldTransformation, "expect field transformation");
        TransformedFieldQueryCreator transformedFieldQueryCreator = new TransformedFieldQueryCreator(environment.getFieldDefinition().getDefinition(), fieldTransformation);
        Document query = transformedFieldQueryCreator.createQuery(environment);
        CompletableFuture<GraphqlCallResult> callResultFuture = graphqlCaller.call(query);
        assertNotNull(callResultFuture, "callResult can't be null");
        return callResultFuture.thenApply(callResult -> new DataFetcherResult<>(callResult.getData().get(environment.getField().getName()), callResult.getErrors()));
    }
}
