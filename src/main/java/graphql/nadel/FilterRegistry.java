package graphql.nadel;

import graphql.execution.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class FilterRegistry implements PreFiltersRegistry, PostFiltersRegistry {
    private List<Filter> filters = new ArrayList<>();

    public void register(Filter filter) {
        if (filters.stream().anyMatch(f -> f.order() == filter.order() && (filter instanceof PreFilter && f instanceof PreFilter || filter instanceof PostFilter && f instanceof PostFilter))) {
            throw new IllegalArgumentException("Fitler has been registered with same order");
        }
        filters.add(filter);
    }

    @Override
    public void doFilter(ExecutionContext executionContext) {
        filters.stream().filter(filter -> filter instanceof PreFilter).sorted((a, b) -> a.order() < b.order() ? 1 : -1).forEachOrdered(f -> ((PreFilter) f).doFilter(executionContext));
    }

    @Override
    public void doFilter(ServiceExecutionResult executionResult) {
        filters.stream().filter(filter -> filter instanceof PostFilter).sorted((a, b) -> a.order() < b.order() ? 1 : -1).forEachOrdered(f -> ((PostFilter) f).doFilter(executionResult));
    }
}
