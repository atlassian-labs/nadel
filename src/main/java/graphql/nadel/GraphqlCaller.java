package graphql.nadel;

import graphql.language.Document;

import java.util.concurrent.CompletableFuture;

public interface GraphqlCaller {

    CompletableFuture<GraphqlCallResult> call(Document query);

}
