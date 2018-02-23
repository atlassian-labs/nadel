package graphql.nadel;

import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import static graphql.Assert.assertNotNull;

public class TransformedFieldDataFetcher implements DataFetcher {

    private GraphqlCaller graphqlCaller;
    private StitchingDsl stitchingDsl;

    public TransformedFieldDataFetcher(GraphqlCaller graphqlCaller, StitchingDsl stitchingDsl) {
        this.graphqlCaller = graphqlCaller;
        this.stitchingDsl = stitchingDsl;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        FieldTransformation fieldTransformation = this.stitchingDsl.getTransformationsByFieldDefinition().get(environment.getFieldDefinition().getDefinition());
        assertNotNull(fieldTransformation, "expect field transformation");
        fieldTransformation.getTargetName();
        return null;
    }
}
