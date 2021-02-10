package graphql.nadel.engine

import graphql.execution.ExecutionPath
import graphql.nadel.result.ExecutionResultNode
import spock.lang.Specification

class PathMapperTest extends Specification {
    def "it uses the parent path in the output"() {
        given:
        def mapper = new PathMapper()
        def parentPath = ExecutionPath.parse("/top")
        def childPath = ExecutionPath.parse("/unexpected/sub")
        def parentNode = Mock(ExecutionResultNode)
        def environment = new UnapplyEnvironment(parentNode, false, false, null, null)
        1 * parentNode.executionPath >> parentPath

        when:
        def result = mapper.mapPath(childPath, "sub", environment)

        then:
        result.toString() == "/top/sub"
    }

    def "it keeps the index segment from the child path"() {
        given:
        def mapper = new PathMapper()
        def parentPath = ExecutionPath.parse("/top/sub")
        def childPath = ExecutionPath.parse("/unexpected/sub[0]")
        def parentNode = Mock(ExecutionResultNode)
        def environment = new UnapplyEnvironment(parentNode, false, false, null, null)
        1 * parentNode.executionPath >> parentPath

        when:
        def result = mapper.mapPath(childPath, "sub", environment)

        then:
        result.toString() == "/top/sub[0]"
    }

    def "it replaces the last segment with the result key"() {
        given:
        def mapper = new PathMapper()
        def parentPath = ExecutionPath.parse("/top")
        def childPath = ExecutionPath.parse("/top/sub")
        def parentNode = Mock(ExecutionResultNode)
        def environment = new UnapplyEnvironment(parentNode, false, false, null, null)
        1 * parentNode.executionPath >> parentPath

        when:
        def result = mapper.mapPath(childPath, "key", environment)

        then:
        result.toString() == "/top/key"
    }

    def "it replaces the last string segment with the result key"() {
        given:
        def mapper = new PathMapper()
        def parentPath = ExecutionPath.parse("/top/sub")
        def childPath = ExecutionPath.parse("/top/sub[0]")
        def parentNode = Mock(ExecutionResultNode)
        def environment = new UnapplyEnvironment(parentNode, false, false, null, null)
        1 * parentNode.executionPath >> parentPath

        when:
        def result = mapper.mapPath(childPath, "key", environment)

        then:
        result.toString() == "/top/key[0]"
    }
}
