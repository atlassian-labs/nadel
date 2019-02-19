package graphql.nadel.engine

import graphql.execution.ExecutionContext
import graphql.execution.nextgen.FieldSubSelection
import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.TestUtil
import spock.lang.Specification

class ServiceResultToResultNodesTest extends Specification {


    def "simple query"() {

        def data = ["hello": "world"]
        def schema = TestUtil.schema("type Query{ hello: String }")
        def query = TestUtil.parseQuery("{hello}")
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == data
    }


    def "query with alias "() {

        def data = ["myAlias": "world"]
        def schema = TestUtil.schema("type Query{ hello: String }")
        def query = TestUtil.parseQuery("{myAlias: hello}")

        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == data
    }

    def "bigger query"() {

        def data = ["foo": ["bar": ["name": "myName", "id": "myId"]], "foo2": ["bar": ["name": "myName2", "id": "myId2"]]]
        def schema = TestUtil.schema("""
        type Query{ 
            foo: Foo
            foo2: Foo
        }
        type Foo {
            bar: Bar
        }
        type Bar {
            id: ID
            name: String
        }
        
        """)
        def query = TestUtil.parseQuery("""
        {foo {
            bar{
                name
                id
            }
        }
        foo2{
            bar{
                name
                id
            }
        }}
        """)
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == data
    }


    def "unknown field"() {
        def data = ["unknown field": "world"]
        def schema = TestUtil.schema("type Query{ hello: String }")
        def query = TestUtil.parseQuery("{hello}")

        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == ["hello": null]
    }

    def "invalid return type: List instead Int"() {
        def data = ["hello": ["world1", "world2"]]
        def schema = TestUtil.schema("type Query{ hello: Int }")
        def query = TestUtil.parseQuery("{hello}")

        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)


        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == ["hello": null]
        executionResult.errors.size() == 1
    }

    def "invalid return type: List instead String"() {
        def data = ["hello": ["world1", "world2"]]
        def schema = TestUtil.schema("type Query{ hello: String }")
        def query = TestUtil.parseQuery("{hello}")

        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == ["hello": null]
        executionResult.errors.size() == 1

    }

    def "invalid return type: String instead List"() {
        def data = ["hello": "world1"]
        def schema = TestUtil.schema("type Query{ hello: [String] }")
        def query = TestUtil.parseQuery("{hello}")
        FieldSubSelection fieldSubSelection
        ExecutionContext executionContext
        (executionContext, fieldSubSelection) = TestUtil.executionData(schema, query)

        ServiceResultToResultNodes resultToNodes = new ServiceResultToResultNodes()
        ServiceExecutionResult delegatedResult = new ServiceExecutionResult(data)

        when:
        def node = resultToNodes.resultToResultNode(executionContext,
                delegatedResult,
                fieldSubSelection.getExecutionStepInfo(),
                fieldSubSelection.getMergedSelectionSet().getSubFieldsList(),
                schema)
        def executionResult = ResultNodesUtil.toExecutionResult(node)

        then:
        executionResult.data == ["hello": null]
        executionResult.errors.size() == 1

    }


}
