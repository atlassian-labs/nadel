package graphql.nadel

import graphql.ErrorType
import graphql.GraphQLError
import graphql.execution.ExecutionId
import graphql.execution.ExecutionIdProvider
import graphql.nadel.testutils.TestUtil
import graphql.schema.idl.TypeDefinitionRegistry
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {

    def simpleNDSL = '''
         service MyService {
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
         }
        '''

    def simpleUnderlyingSchema = typeDefinitions('''
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
            type Mutation{
                hello: String  
            } 
        ''')

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)

    def "query to one service with execution input passed down"() {

        given:
        def query = '''
        query OpName { hello {name} hello {id} }
        '''

        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(["var1": "val1"])
                .context("contextObj")
                .operationName("OpName")
                .build()
        def data = [hello: [id: "3", name: "earth"]]

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "query nadel_2_MyService_OpName {hello {name} hello {id}}"
            assert params.context == "contextObj"
            assert params.operationDefinition.name == "nadel_2_MyService_OpName"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.join().data == data
    }

    def "graphql-java validation is invoked"() {
        given:
        def query = '''
        query OpName($unusedVariable : String) { hello {name} hello {id} }
        '''

        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        !result.errors.isEmpty()
        def error = result.errors[0] as GraphQLError
        error.errorType == ErrorType.ValidationError

    }

    def "query to two services with field rename"() {

        def nsdl = '''
         service Foo {
            type Query{
                foo: Foo  => renamed from fooOriginal 
            } 
            type Foo {
                name: String
            }
         }
         service Bar {
            type Query{
                bar: Bar 
            } 
            type Bar {
                name: String => renamed from title
            }
         }
        '''
        def query = '''
        { otherFoo: foo {name} bar{name}}
        '''
        def underlyingSchema1 = typeDefinitions('''
            type Query{
                fooOriginal: Foo  
                
            } 
            type Foo {
                name: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query{
                bar: Bar 
            } 
            type Bar {
                title: String
            }
        ''')
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Foo: new Tuple2(delegatedExecution1, underlyingSchema1),
                Bar: new Tuple2(delegatedExecution2, underlyingSchema2)]
        )

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [otherFoo: [name: "Foo"]]
        def data2 = [bar: [title: "Bar"]]
        ServiceExecutionResult delegatedExecutionResult1 = new ServiceExecutionResult(data1)
        ServiceExecutionResult delegatedExecutionResult2 = new ServiceExecutionResult(data2)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.execute(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.execute(_) >> completedFuture(delegatedExecutionResult2)
        result.join().data == [otherFoo: [name: "Foo"], bar: [name: "Bar"]]
    }

    def "query with three nested hydrations"() {

        def nsdl = '''
         service Foo {
            type Query{
                foos: [Foo]  
            } 
            type Foo {
                name: String
                bar: Bar => hydrated from Bar.barsById(id: $source.barId) object identified by barId, batch size 2
            }
         }
         service Bar {
            type Query{
                bar: Bar 
            } 
            type Bar {
                barId: ID
                name: String 
                nestedBar: Bar => hydrated from Bar.barsById(id: $source.nestedBarId) object identified by barId
            }
         }
        '''
        def underlyingSchema1 = typeDefinitions('''
            type Query{
                foos: [Foo]  
            } 
            type Foo {
                name: String
                barId: ID
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query{
                bar: Bar 
                barsById(id: [ID]): [Bar]
            } 
            type Bar {
                barId: ID
                name: String
                nestedBarId: ID
            }
        ''')

        def query = '''
                { foos { bar { name nestedBar {name nestedBar { name } } } } }
        '''
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Foo: new Tuple2(serviceExecution1, underlyingSchema1),
                Bar: new Tuple2(serviceExecution2, underlyingSchema2)]
        )
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        def topLevelData = [foos: [[barId: "bar1"], [barId: "bar2"], [barId: "bar3"]]]
        def hydrationDataBatch1 = [barsById: [[object_identifier__UUID: "bar1", name: "Bar 1", nestedBarId: "nestedBar1"], [object_identifier__UUID: "bar2", name: "Bar 2", nestedBarId: "nestedBar2"]]]
        def hydrationDataBatch2 = [barsById: [[object_identifier__UUID: "bar3", name: "Bar 3", nestedBarId: null]]]
        def hydrationData2 = [barsById: [[object_identifier__UUID: "nestedBar1", name: "NestedBarName1", nestedBarId: "nestedBarId456"]]]
        def hydrationData3 = [barsById: [[object_identifier__UUID: "nestedBarId456", name: "NestedBarName2"]]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult1_1 = new ServiceExecutionResult(hydrationDataBatch1)
        ServiceExecutionResult hydrationResult1_2 = new ServiceExecutionResult(hydrationDataBatch2)
        ServiceExecutionResult hydrationResult2 = new ServiceExecutionResult(hydrationData2)
        ServiceExecutionResult hydrationResult3 = new ServiceExecutionResult(hydrationData3)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >>

                completedFuture(topLevelResult)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult1_1)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult1_2)


        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult2)

        1 * serviceExecution2.execute(_) >>

                completedFuture(hydrationResult3)

        result.join().data == [foos: [[bar: [name: "Bar 1", nestedBar: [name: "NestedBarName1", nestedBar: [name: "NestedBarName2"]]]], [bar: [name: "Bar 2", nestedBar: null]], [bar: [name: "Bar 3", nestedBar: null]]]]
    }

    def 'mutation can be executed'() {

        def query = '''
        mutation M{ hello }
        '''

        given:
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: "world"]
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "mutation nadel_2_MyService_M {hello}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.join().data == data
    }

    def "can declare common types"() {

        def nsdl = '''
         common {
            interface Node {
                id: ID!
            }
         }
         service IssueService {
            type Query{
                node: Node
            } 
            type Issue implements Node {
                id: ID!
                name: String
            }
         }
        '''
        def query = '''
        { node { ...on Issue { name } } }
        '''
        def underlyingSchema = typeDefinitions('''
            type Query{
                node: Node  
                
            } 
            interface Node {
                id: ID!
            }
            type Issue implements Node {
                id: ID!
                name: String
            }
        ''')
        ServiceExecution serviceExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                IssueService: new Tuple2(serviceExecution, underlyingSchema)]
        )

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("uuid")
                .build()
        def data1 = [node: [typename__uuid: "Issue", name: "My Issue"]]
        ServiceExecutionResult serviceExecutionResult = new ServiceExecutionResult(data1)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution.execute(_) >> completedFuture(serviceExecutionResult)
        result.join().data == [node: [name: "My Issue"]]
    }

    def "deep rename works"() {

        def nsdl = '''
         service IssueService {
            type Query{
                issue: Issue
            } 
            type Issue {
                name: String => renamed from detail.detailName
            }
         }
        '''
        def underlyingSchema = typeDefinitions('''
            type Query{
                issue: Issue 
                
            } 
            type Issue {
                detail: IssueDetails
            }
            type IssueDetails {
                detailName: String
            }
        ''')
        ServiceExecution serviceExecution = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                IssueService: new Tuple2(serviceExecution, underlyingSchema)]
        )

        given:
        def query = '''
        { issue { name } } 
        '''
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("uuid")
                .build()
        def data1 = [issue: [detail: [detailName: "My Issue"]]]
        ServiceExecutionResult serviceExecutionResult = new ServiceExecutionResult(data1)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution.execute({ params ->
            println printAstCompact(params.query)
            printAstCompact(params.query) == "query nadel_2_IssueService {issue {detail {detailName}}}"
        }) >> completedFuture(serviceExecutionResult)
        result.join().data == [issue: [name: "My Issue"]]
    }

    ExecutionIdProvider idProvider = new ExecutionIdProvider() {
        @Override
        ExecutionId provide(String queryStr, String operationName, Object context) {
            return ExecutionId.from("fromProvider")
        }
    }

    def "executionId is transferred from input"() {
        given:
        def query = '''
        query { hello {name} hello {id} }
        '''
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .executionIdProvider(idProvider)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .executionId(ExecutionId.from("fromInput"))
                .build()
        when:
        nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert params.executionId == ExecutionId.from("fromInput")
            completedFuture(new ServiceExecutionResult([:]))
        }
    }


    def "executionId is transferred from provider if missing in input"() {
        given:
        def query = '''
        query { hello {name} hello {id} }
        '''
        Nadel nadel = newNadel()
                .dsl(simpleNDSL)
                .serviceExecutionFactory(serviceFactory)
                .executionIdProvider(idProvider)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        when:
        nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert params.executionId == ExecutionId.from("fromProvider")
            completedFuture(new ServiceExecutionResult([:]))
        }
    }

    def "extending types from another service is possible"() {
        def ndsl = '''
         service Service1 {
            type Query{
                root: Root  
            } 
            extend type Query {
                anotherRoot: String
            }
            type Root {
                id: ID
            }
            extend type Root {
                name: String
            }
         }
         service Service2 {
            # Nadel currently requires a Query object
            type Query { 
                dummy: String
            } 
            
            extend type Root {
                extension: Extension => hydrated from Service2.lookup(id: $source.id) object identified by id 
            }
            type Extension {
                id: ID
                name: String
            }
         }
        '''
        def underlyingSchema1 = typeDefinitions('''
            type Query{
                root: Root  
                dummy:String
            } 
            extend type Query {
                anotherRoot: String
            }
            type Root {
                id: ID
            }
            extend type Root {
                name: String
            }
        ''')
        def underlyingSchema2 = typeDefinitions('''
            type Query{
                lookup(id:ID): Extension
            } 
            type Extension {
                id: ID
                name: String
            }
        ''')

        def execution1 = Mock(ServiceExecution)
        def execution2 = Mock(ServiceExecution)
        def serviceFactory = new ServiceExecutionFactory() {
            @Override
            ServiceExecution getServiceExecution(String serviceName) {
                switch (serviceName) {
                    case "Service1":
                        return execution1
                    case "Service2":
                        return execution2
                    default:
                        throw new RuntimeException()
                }
            }

            @Override
            TypeDefinitionRegistry getUnderlyingTypeDefinitions(String serviceName) {
                switch (serviceName) {
                    case "Service1":
                        return underlyingSchema1
                    case "Service2":
                        return underlyingSchema2
                    default:
                        throw new RuntimeException()
                }
            }
        }
        Nadel nadel = newNadel()
                .dsl(ndsl)
                .serviceExecutionFactory(serviceFactory)
                .executionIdProvider(idProvider)
                .build()

        def query = """
        { root {
            id
            name
            extension {
                id
                name
            }
        }
        anotherRoot}
        """
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .executionId(ExecutionId.from("fromInput"))
                .build()


        def service1Data1 = [root: [id: "rootId", name: "rootName"]]
        def service1Data2 = [anotherRoot: "anotherRoot"];
        def service2Data = [lookup: [id: "rootId", name: "extensionName"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * execution1.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            println printAstCompact(params.getQuery())
            assert printAstCompact(params.getQuery()) == "query nadel_2_Service1 {root {id name id}}"
            completedFuture(new ServiceExecutionResult(service1Data1))
        }
        1 * execution1.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            println printAstCompact(params.getQuery())
            assert printAstCompact(params.getQuery()) == "query nadel_2_Service1 {anotherRoot}"
            completedFuture(new ServiceExecutionResult(service1Data2))
        }
        1 * execution2.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            println printAstCompact(params.getQuery())
            assert printAstCompact(params.getQuery()) == "query nadel_2_Service2 {lookup(id:\"rootId\") {id name}}"
            completedFuture(new ServiceExecutionResult(service2Data))
        }

        result.data == [anotherRoot: "anotherRoot", root: [id: "rootId", name: "rootName", extension: [id: "rootId", name: "extensionName"]]]

    }

}
