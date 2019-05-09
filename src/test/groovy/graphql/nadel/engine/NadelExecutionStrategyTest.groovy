package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.execution.ExecutionId
import graphql.execution.ExecutionStepInfo
import graphql.execution.nextgen.ExecutionHelper
import graphql.execution.nextgen.result.ExecutionResultNode
import graphql.execution.nextgen.result.LeafExecutionResultNode
import graphql.execution.nextgen.result.ResultNodesUtil
import graphql.execution.nextgen.result.RootExecutionResultNode
import graphql.language.Argument
import graphql.language.StringValue
import graphql.nadel.DefinitionRegistry
import graphql.nadel.FieldInfo
import graphql.nadel.FieldInfos
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionHooks
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.testutils.TestUtil
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import graphql.util.TraverserVisitor
import graphql.util.TreeTransformerUtil
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.testutils.TestUtil.parseQuery

class NadelExecutionStrategyTest extends Specification {

    def executionHelper
    def service1Execution
    def service2Execution
    def serviceDefinition
    def definitionRegistry
    def instrumentation
    def serviceExecutionHooks

    void setup() {
        executionHelper = new ExecutionHelper()
        service1Execution = Mock(ServiceExecution)
        service2Execution = Mock(ServiceExecution)
        serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        definitionRegistry = Mock(DefinitionRegistry)
        instrumentation = new NadelInstrumentation() {}
        serviceExecutionHooks = new ServiceExecutionHooks() {}
    }

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
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo}"
        def executionData = createExecutionData(query, overallSchema)

        def expectedQuery = "query nadel_2_service {foo}"

        when:
        nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery
        } as ServiceExecutionParameters) >> CompletableFuture.completedFuture(new ServiceExecutionResult(null))
    }

    def "one call to one service with list result"() {
        given:
        def underlyingSchema = TestUtil.schema("""
        type Query {
            foo: [String]
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo: [String]
        }
        """)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo}"
        def executionData = createExecutionData(query, overallSchema)

        def expectedQuery = "query nadel_2_service {foo}"

        def serviceResultData = [foo: ["foo1", "foo2"]]

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery
        } as ServiceExecutionParameters) >> CompletableFuture.completedFuture(new ServiceExecutionResult(serviceResultData))
        resultData(response) == [foo: ["foo1", "foo2"]]
    }


    def underlyingHydrationSchema1 = TestUtil.schema("""
        type Query {
            foo(id : ID) : Foo
        }
        type Foo {
            id: ID
            barId: ID
        }
        """)
    def underlyingHydrationSchema2 = TestUtil.schema("""
        type Query {
            barById(id: ID): Bar
        }
        type Bar {
            id: ID
            name : String
        }
        """)

    def overallHydrationSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo(id : ID): Foo
            }
            type Foo {
                id: ID
                bar: Bar => hydrated from service2.barById(id: $source.barId)
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
        ''')


    def "one hydration call with variables defined"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query($var : ID, $unusedVar2 : ID) {foo(id : $var) {bar{id name}}}
        '''
        def expectedQuery1 = 'query nadel_2_service1($var:ID) {foo(id:$var) {barId}}'
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def executionData = createExecutionData(query, overallHydrationSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({
            printAstCompact(it.query) == expectedQuery1
        } as ServiceExecutionParameters) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({
            printAstCompact(it.query) == expectedQuery2
        } as ServiceExecutionParameters) >> CompletableFuture.completedFuture(response2)

        resultData(response) == [foo: [bar: [id: "barId", name: "Bar1"]]]
    }


    def "one hydration call with fragments defined"() {
        given:
        def hydrationService1 = new Service("service1", underlyingHydrationSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def hydrationService2 = new Service("service2", underlyingHydrationSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fooFieldDefinition = overallHydrationSchema.getQueryType().getFieldDefinition("foo")

        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, hydrationService1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([hydrationService1, hydrationService2], fieldInfos, overallHydrationSchema, instrumentation, serviceExecutionHooks)


        def query = '''
            query { foo { ... frag1 } } 
            
            fragment frag1 on Foo {
                bar { id name }
            }
            fragment unusedFrag2 on Foo {
                bar { id name }
            }

        '''
        def expectedQuery1 = 'query nadel_2_service1 {foo {...frag1}} fragment frag1 on Foo {barId}'
        def response1 = new ServiceExecutionResult([foo: [barId: "barId"]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId", name: "Bar1"]])

        def document = parseQuery(query)
        def executionInput = ExecutionInput.newExecutionInput().query(query).context(NadelContext.newContext().build()) build()
        def executionData = executionHelper.createExecutionData(document, overallHydrationSchema, ExecutionId.generate(), executionInput, null)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)

        resultData(response) == [foo: [bar: [id: "barId", name: "Bar1"]]]
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

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barById(id: $source.barId)
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
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId1\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId1", name: "Bar1"]])

        def expectedQuery3 = "query nadel_2_service2 {barById(id:\"barId2\") {id name}}"
        def response3 = new ServiceExecutionResult([barById: [id: "barId2", name: "Bar3"]])

        def expectedQuery4 = "query nadel_2_service2 {barById(id:\"barId3\") {id name}}"
        def response4 = new ServiceExecutionResult([barById: [id: "barId3", name: "Bar4"]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery3
        }) >> CompletableFuture.completedFuture(response3)
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery4
        }) >> CompletableFuture.completedFuture(response4)

        resultData(response) == [foo: [bar: [[id: "barId1", name: "Bar1"], [id: "barId2", name: "Bar3"], [id: "barId3", name: "Bar4"]]]]
    }

    def "hydration list with batching"() {
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
            barsById(id: [ID]): [Bar]
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barsById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barsById(id: [ID]): [Bar]
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{ name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barsById(id:[\"barId1\",\"barId2\",\"barId3\"]) {name object_identifier__UUID:id}}"
        def response2 = new ServiceExecutionResult([barsById: [[object_identifier__UUID: "barId1", name: "Bar1"], [object_identifier__UUID: "barId2", name: "Bar2"], [object_identifier__UUID: "barId3", name: "Bar3"]]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)

        resultData(response) == [foo: [bar: [[name: "Bar1"], [name: "Bar2"], [name: "Bar3"]]]]
    }

    def "hydration batching returns null"() {
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
            barsById(id: [ID]): [Bar]
        }
        type Bar {
            id: ID
            name : String
        }
        """)

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barsById(id: $source.barId)
            }
        }
        service service2 {
            type Query {
                barsById(id: [ID]): [Bar]
            }
            type Bar {
                id: ID
                name: String
            }
        }
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{ name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1", "barId2", "barId3"]]])

        def expectedQuery2 = "query nadel_2_service2 {barsById(id:[\"barId1\",\"barId2\",\"barId3\"]) {name object_identifier__UUID:id}}"
        def response2 = new ServiceExecutionResult(null)

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)

        resultData(response) == [foo: [bar: [null, null, null]]]
    }

    def "hydration list with one element"() {
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

        def overallSchema = TestUtil.schemaFromNdsl('''
        service service1 {
            type Query {
                foo: Foo
            }
            type Foo {
                id: ID
                bar: [Bar] => hydrated from service2.barById(id: $source.barId)
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
        ''')
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service1 = new Service("service1", underlyingSchema1, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service("service2", underlyingSchema2, service2Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service1)
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def query = "{foo {bar{id name}}}"
        def expectedQuery1 = "query nadel_2_service1 {foo {barId}}"
        def response1 = new ServiceExecutionResult([foo: [barId: ["barId1"]]])

        def expectedQuery2 = "query nadel_2_service2 {barById(id:\"barId1\") {id name}}"
        def response2 = new ServiceExecutionResult([barById: [id: "barId1", name: "Bar1"]])

        def executionData = createExecutionData(query, overallSchema)

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery1
        }) >> CompletableFuture.completedFuture(response1)

        then:
        1 * service2Execution.execute({ ServiceExecutionParameters sep ->
            printAstCompact(sep.query) == expectedQuery2
        }) >> CompletableFuture.completedFuture(response2)

        resultData(response) == [foo: [bar: [[id: "barId1", name: "Bar1"]]]]
    }

    FieldInfos topLevelFieldInfo(GraphQLFieldDefinition fieldDefinition, Service service) {
        FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, fieldDefinition)
        return new FieldInfos([(fieldDefinition): fieldInfo])
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, GraphQLSchema overallSchema) {
        def document = parseQuery(query)

        def nadelContext = NadelContext.newContext().artificialFieldsUUID("UUID").build()
        def executionInput = ExecutionInput.newExecutionInput().query(query)
                .context(nadelContext)
                .build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null)
        executionData
    }

    Object resultData(CompletableFuture<RootExecutionResultNode> response) {
        ResultNodesUtil.toExecutionResult(response.get()).data
    }

    def "service context created and arguments modified"() {
        given:
        def underlyingSchema = TestUtil.schema("""
        type Query {
            foo(id: String): String  
        }
        """)

        def overallSchema = TestUtil.schema("""
        type Query {
            foo(id: String): String
        }
        """)
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)

        def serviceContext = "Service-Context"

        def serviceExecutionHooks = new ServiceExecutionHooks() {
            @Override
            Object createServiceContext(Service s, ExecutionStepInfo topLevelStepInfo) {
                return serviceContext
            }

            @Override
            List<Argument> modifyArguments(Service s, Object sc, ExecutionStepInfo topLevelStepInfo, List<Argument> arguments) {
                return [Argument.newArgument("id", StringValue.newStringValue("modified").build()).build()]
            }
        }
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo(id: \"fullID\")}"
        def executionData = createExecutionData(query, overallSchema)

        def expectedQuery = "query nadel_2_service {foo(id:\"modified\")}"

        when:
        nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute({ it ->
            printAstCompact(it.query) == expectedQuery && it.serviceContext == serviceContext
        } as ServiceExecutionParameters) >> CompletableFuture.completedFuture(new ServiceExecutionResult(null))
    }


    def "service result can be modified"() {
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
        def fooFieldDefinition = overallSchema.getQueryType().getFieldDefinition("foo")

        def service = new Service("service", underlyingSchema, service1Execution, serviceDefinition, definitionRegistry)
        def fieldInfos = topLevelFieldInfo(fooFieldDefinition, service)

        def serviceContext = "Service-Context"

        def serviceExecutionHooks = new ServiceExecutionHooks() {
            @Override
            Object createServiceContext(Service s, ExecutionStepInfo topLevelStepInfo) {
                return serviceContext
            }

            @Override
            RootExecutionResultNode postServiceResult(Service s, Object sc, GraphQLSchema os, RootExecutionResultNode resultNode) {
                def transformer = new ResultNodesTransformer()
                return transformer.transform(resultNode, new TraverserVisitor<ExecutionResultNode>() {
                    @Override
                    TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                        if (context.thisNode() instanceof LeafExecutionResultNode) {
                            LeafExecutionResultNode leafExecutionResultNode = context.thisNode();
                            def fetchedValueAnalysis = leafExecutionResultNode.getFetchedValueAnalysis()
                            def completedValue = fetchedValueAnalysis.getCompletedValue()
                            def newFVA = fetchedValueAnalysis.transfrom({ builder -> builder.completedValue(completedValue + "-CHANGED") })
                            def newNode = leafExecutionResultNode.withNewFetchedValueAnalysis(newFVA)
                            return TreeTransformerUtil.changeNode(context, newNode)
                        }
                        return TraversalControl.CONTINUE
                    }

                    @Override
                    TraversalControl leave(TraverserContext<ExecutionResultNode> context) {
                        return TraversalControl.CONTINUE
                    }
                })
            }
        }
        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)

        def query = "{foo}"
        def executionData = createExecutionData(query, overallSchema)

        def data = [foo: "hello world"]

        when:
        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)


        then:
        1 * service1Execution.execute(_) >> CompletableFuture.completedFuture(new ServiceExecutionResult(data))

        resultData(response) == [foo: "hello world-CHANGED"]
    }


}
