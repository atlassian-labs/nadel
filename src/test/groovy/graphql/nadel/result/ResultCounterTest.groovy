package graphql.nadel.result

import spock.lang.Specification

class ResultCounterTest extends Specification {

    def "test nodeCount counter"() {
        def resultCounter = new ResultCounter()

        when:
        resultCounter.incrementNodeCount()
        resultCounter.incrementNodeCount(3)
        then:
        resultCounter.getNodeCount() == 4

        when:
        resultCounter.decrementNodeCount()
        then:
        resultCounter.getNodeCount() == 3

        when:
        resultCounter.incrementNodeCount(-2)
        then:
        resultCounter.getNodeCount() == 1
    }

    def "test field rename counter"() {
        def resultCounter = new ResultCounter()

        when:
        resultCounter.incrementFieldRenameCount()
        resultCounter.incrementFieldRenameCount(2)
        then:
        resultCounter.getFieldRenameCount() == 3

        when:
        resultCounter.decrementFieldRenameCount()
        then:
        resultCounter.getFieldRenameCount() == 2

        when:
        resultCounter.incrementFieldRenameCount(-5)
        then:
        resultCounter.getFieldRenameCount() == -3
    }

    def "test type rename counter"() {
        def resultCounter = new ResultCounter()

        when:
        resultCounter.incrementTypeRenameCount()
        resultCounter.incrementTypeRenameCount(10)
        then:
        resultCounter.getTypeRenameCount() == 11

        when:
        resultCounter.decrementTypeRenameCount()
        then:
        resultCounter.getTypeRenameCount() == 10

        when:
        resultCounter.incrementTypeRenameCount(-1)
        then:
        resultCounter.getTypeRenameCount() == 9
    }
}
