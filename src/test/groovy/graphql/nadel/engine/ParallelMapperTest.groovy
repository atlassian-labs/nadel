package graphql.nadel.engine

import graphql.execution.ExecutionContext
import spock.lang.Specification

import java.util.concurrent.ForkJoinPool

class ParallelMapperTest extends Specification {

    def "parallel mapper with partitionSize =1"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = { 2 * it }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 1)

        then:
        result == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }

    def "parallel mapper with size = partitionSize"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = { 2 * it }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 10)

        then:
        result == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }

    def "parallel mapper with size < partitionSize"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = { 2 * it }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 15)

        then:
        result == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }

    def "parallel mapper with partitionSize > 1"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = { 2 * it }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 5)

        then:
        result == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }

    def "parallel mapper with partitionSize = 1"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = { 2 * it }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 1)

        then:
        result == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }

    def "parallel mapper with real delay"() {
        given:
        NadelContext nadelContext = NadelContext.newContext().forkJoinPool(ForkJoinPool.commonPool()).build();
        ExecutionContext executionContext = Mock(ExecutionContext)
        executionContext.getContext() >> nadelContext

        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
        def mapper = {
            Thread.sleep(50)
            2 * it
        }

        when:
        def result = ParallelMapper.mapParallel(executionContext, list, mapper, 3)

        then:
        new ArrayList<>(result) == [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    }
}
