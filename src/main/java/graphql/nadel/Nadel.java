package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.PublicApi;
import graphql.nadel.dsl.StitchingDsl;

import java.util.concurrent.CompletableFuture;

@PublicApi
public class Nadel {

    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();

    public Nadel(String dsl) {
        StitchingDsl stitchingDsl = this.parser.parseDSL(dsl);

    }

    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return null;
    }

}
