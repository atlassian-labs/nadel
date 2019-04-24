package graphql.nadel;

public interface PostFilter extends Filter {
    void doFilter(ServiceExecutionResult executionResult);
}
