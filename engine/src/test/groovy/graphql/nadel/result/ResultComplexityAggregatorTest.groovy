package graphql.nadel.result


import spock.lang.Specification

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
        resultComplexityAggregator.toString() == "ResultComplexityAggregator{totalNodeCount=24, serviceNodeCountsMap={service1=14, service2=10}, totalFieldRenameCount=0, totalTypeRenameCount=0}"
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
        complexityMap == [totalNodeCount:24, serviceNodeCounts:[service1:14, service2:10], fieldRenamesCount:0, typeRenamesCount:0]
    }

    def "test rename field method"() {
        when:
        resultComplexityAggregator.incrementFieldRenameCount(10);
        then:
        resultComplexityAggregator.getTotalNodeCount() == 0
        resultComplexityAggregator.getFieldRenamesCount() == 10

        when:
        resultComplexityAggregator.incrementFieldRenameCount(-2);
        then:
        resultComplexityAggregator.getFieldRenamesCount() == 8
    }

    def "test rename field type method"() {
        when:
        resultComplexityAggregator.incrementTypeRenameCount(3);
        resultComplexityAggregator.incrementTypeRenameCount(1);
        resultComplexityAggregator.incrementTypeRenameCount(2);

        then:
        resultComplexityAggregator.getTotalNodeCount() == 0
        resultComplexityAggregator.getTypeRenamesCount() == 6

        when:
        resultComplexityAggregator.incrementTypeRenameCount(-3);
        resultComplexityAggregator.incrementTypeRenameCount(4);

        then:
        resultComplexityAggregator.getTypeRenamesCount() == 7
    }

    def "test snapshotComplexity Data method for type/field renames"() {
        when:
        resultComplexityAggregator.incrementTypeRenameCount(3);
        resultComplexityAggregator.incrementTypeRenameCount(1);
        resultComplexityAggregator.incrementTypeRenameCount(2);
        resultComplexityAggregator.incrementFieldRenameCount(10);

        def complexityMap = resultComplexityAggregator.snapshotResultComplexityData()

        then:
        complexityMap == [totalNodeCount:0, serviceNodeCounts:[:], fieldRenamesCount:10, typeRenamesCount:6]
    }

}
