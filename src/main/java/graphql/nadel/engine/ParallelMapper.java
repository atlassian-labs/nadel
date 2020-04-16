package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.ExecutionContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static graphql.nadel.util.FpKit.map;

@Internal
public class ParallelMapper {

    public static <T, U> List<U> mapParallel(ExecutionContext executionContext, List<T> list, Function<T, U> function, int partitionSize) {
        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        ForkJoinPool forkJoinPool = nadelContext.getForkJoinPool();
        if (list.size() == 0) {
            return Collections.emptyList();
        }
        if (list.size() <= partitionSize) {
            return map(list, function);
        }
        Object[] array = new Object[list.size()];
        CopyOnWriteArrayList<U> result = new CopyOnWriteArrayList<>((U[]) array);
        forkJoinPool.invoke(new CountedCompleter<List<U>>() {
            @Override
            public void compute() {
                for (int i = 0; i < list.size(); i += partitionSize) {
                    int count = i + partitionSize < list.size() ? partitionSize : list.size() - i;
                    Task<T, U> newTask = new Task<>(this, list, function, result, i, count);
                    if (i + partitionSize < list.size()) {
                        addToPendingCount(1);
                        newTask.fork();
                    } else {
                        newTask.compute();
                    }
                }
            }

        });
        return result;
    }


    private static class Task<T, U> extends CountedCompleter {

        private final List<T> list;
        private Function<T, U> function;
        private List<U> result;
        private int index;
        private int count;

        public Task(CountedCompleter parent, List<T> list, Function<T, U> function, List<U> result, int index, int count) {
            super(parent);
            this.list = list;
            this.function = function;
            this.result = result;
            this.index = index;
            this.count = count;
        }

        @Override
        public void compute() {
            for (int i = index; i < index + count; i++) {
                T value = list.get(i);
                U mapped = function.apply(value);
                result.set(i, mapped);
            }
            propagateCompletion();
        }
    }
}
