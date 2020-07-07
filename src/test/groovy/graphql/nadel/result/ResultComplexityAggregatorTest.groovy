package graphql.nadel.result


import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap

class ResultComplexityAggregatorTest extends Specification {
    def resultComplexityAggregator = new ResultComplexityAggregator();

    def "test single service"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 10)

        then:
        resultComplexityAggregator.getNodeCountsForService("service1") == 10
        resultComplexityAggregator.getTotalNodeCount() == 10
    }

    def "test multiple services"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 10)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)


        then:
        resultComplexityAggregator.getNodeCountsForService("service1") == 10
        resultComplexityAggregator.getNodeCountsForService("service2") == 5

        resultComplexityAggregator.getTotalNodeCount() == 15
    }

    def "test multiple adds to single service"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 5)

        then:
        resultComplexityAggregator.getNodeCountsForService("service1") == 8
        resultComplexityAggregator.getTotalNodeCount() == 8
    }

    def "test multiple adds to multiple service"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)

        then:
        resultComplexityAggregator.getNodeCountsForService("service1") == 14
        resultComplexityAggregator.getNodeCountsForService("service2") == 10
        resultComplexityAggregator.getTotalNodeCount() == 24
    }

    def "test toString method"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)

        then:
        resultComplexityAggregator.toString() == "ResultComplexityAggregator{totalNodeCount=24, serviceNodeCountsMap={service2=10, service1=14}}"
    }

    def "test snapshotComplexityData method"() {
        when:
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 5)
        resultComplexityAggregator.incrementServiceNodeCount("service1", 3)
        resultComplexityAggregator.incrementServiceNodeCount("service2", 5)

        def complexityMap = resultComplexityAggregator.snapshotResultComplexityData()

        then:
        complexityMap == [totalNodeCount:24, serviceNodeCounts:[service2:10, service1:14]]
    }





}
