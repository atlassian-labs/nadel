package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.execution.ExecutionId
import graphql.execution.nextgen.ExecutionHelper
import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.language.AstPrinter
import graphql.nadel.DefinitionRegistry
import graphql.nadel.FieldInfo
import graphql.nadel.FieldInfos
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.TestUtil
import graphql.nadel.dsl.ServiceDefinition
import graphql.schema.GraphQLFieldDefinition
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class NadelExecutionStrategyTest extends Specification {


    ExecutionHelper executionHelper = new ExecutionHelper()

    def "one call to one service"() {
        given:
        def underlyingSchema = TestUtil.schema("""
        type Query {
            foo: String  
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo: String
        }
        """)
        def serviceExecution = Mock(ServiceExecution)
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, serviceExecution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema)


        def query = "{foo}"
        def document = TestUtil.parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null);
        executionData.executionContext

        def expectedQuery = "query {foo}"

        when:
        nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * serviceExecution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery
        }) >> CompletableFuture.completedFuture(Mock(ServiceExecutionResult))
    }


    def "one hydration call"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: ID
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl("""
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: Bar <= \$innerQueries.service2.barById(id: \$source.barId)
            }
        }
        service service2 {
            type Query {
                barById(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        """)
        def service1Execution = Mock(ServiceExecution)
        def service2Execution = Mock(ServiceExecution)
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])
        def expectedQuery2 = "query {barById(id:\"barId\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])
        def document = TestUtil.parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null);
        executionData.executionContext


        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({
            println AstPrinter.printAstCompact(it.query)
            AstPrinter.printAstCompact(it.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)

        ResultNodesUtil.toExecutionResult(response.get()).data == [foo: [bar: [id: "barId", name: "Bar1"]]]
    }

    def "hydration list"() {
        given:
        def underlyingSchema1 = TestUtil.schema("""
        type Query {
            foo : Foo
        }
        type Foo {
            id: ID
            barId: [ID]
        }
        """)
        def underlyingSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl("""
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] <= \$innerQueries.service2.barById(id: \$source.barId)
            }
        }
        service service2 {
            type Query {
                barById(id: ID): Bar
            }
            type Bar {
                id: ID
                name: String
            }
        }
        """)
        def service1Execution = Mock(ServiceExecution)
        def service2Execution = Mock(ServiceExecution)
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query {barById(id:\"barId1\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId1", name: "Bar1"]])

        def expectedQuery3 = "query {barById(id:\"barId2\") {id name}}"
        def response3 = new ServiceExecutionResult([barById: [id: "barId2", name: "Bar3"]])

        def expectedQuery4 = "query {barById(id:\"barId3\") {id name}}"
        def response4 = new ServiceExecutionResult([barById: [id: "barId3", name: "Bar4"]])

        def document = TestUtil.parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null);
        executionData.executionContext


        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)
        1 * service2Execution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery3
        }) >> CompletableFuture.completedFuture(response3)
        1 * service2Execution.execute({
            AstPrinter.printAstCompact(it.query) == expectedQuery4
        }) >> CompletableFuture.completedFuture(response4)

        ResultNodesUtil.toExecutionResult(response.get()).data == [foo: [bar: [[id: "barId1", name: "Bar1"], [id: "barId2", name: "Bar3"], [id: "barId3", name: "Bar4"]]]]
    }

    FieldInfos topLevelFieldInfo(GraphQLFieldDefinition fieldDefinition, Service service) {
        FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, fieldDefinition)
        return new FieldInfos([(fieldDefinition): fieldInfo])
    }

}
