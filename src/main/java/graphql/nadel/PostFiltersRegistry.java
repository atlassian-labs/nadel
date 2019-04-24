package graphql.nadel;

public interface PostFiltersRegistry {
    void doFilter(ServiceExecutionResult executionResult);
}
