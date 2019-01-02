package graphql.nadel.engine


import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.nadel.DelegatedExecutionResult
import graphql.nadel.TestUtil
import spock.lang.Specification

class DelegatedResultToResultNodeTest extends Specification {


    def "result data to nodes"() {

        def data = ["hello": "world"]
        def query = TestUtil.parseQuery("{hello}")
        def schema = TestUtil.schema("type Query{ hello: String }")

        def (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        DelegatedResultToResultNode resultToNodes = new DelegatedResultToResultNode()
        DelegatedExecutionResult delegatedResult = new DelegatedExecutionResult(data)


        when:
        def node = resultToNodes.resultToResultNode(executionContext, delegatedResult, fieldSubSelection)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == data

    }

}
