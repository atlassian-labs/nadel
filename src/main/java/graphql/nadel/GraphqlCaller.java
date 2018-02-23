package graphql.nadel;

import graphql.language.Document;

public interface GraphqlCaller {

    GraphqlCallResult call(Document query);

}
