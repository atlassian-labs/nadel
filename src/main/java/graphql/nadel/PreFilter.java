package graphql.nadel;

import graphql.execution.ExecutionContext;

public interface PreFilter extends Filter {
     void doFilter(ExecutionContext executionContext);
}
