package graphql.nadel;

import graphql.execution.ExecutionContext;

public interface PreFiltersRegistry {
    void doFilter(ExecutionContext executionContext);
}
