package graphql.nadel;


import graphql.PublicApi;

@PublicApi
public interface DelegatedExecution {

    DelegatedExecutionResult delegate(DelegatedExecutionParameters delegatedExecutionParameters);
}
