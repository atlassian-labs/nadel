package graphql.nadel

import graphql.language.AstPrinter
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {

    def "query to one service"() {

        def nsdl = """
         service MyService {
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
         }
        """
        def query = """
        { hello {name} hello {id} }
        """
        def underlyingSchema = TestUtil.schema("""
            type Query{
                hello: World  
            } 
            type World {
                id: ID
                name: String
            }
        """)
        ServiceExecution delegatedExecution = Mock(ServiceExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            ServiceExecution getDelegatedExecution(String serviceName) {
                return delegatedExecution
            }

            @Override
            GraphQLSchema getUnderlyingSchema(String serviceName) {
                underlyingSchema
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(new StringReader(nsdl))
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: [id: "3", name: "earth"]]
        ServiceExecutionResult delegatedExecutionResult = new ServiceExecutionResult(data)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert AstPrinter.printAstCompact(params.query) == "query {hello {name} hello {id}}"
            completedFuture(delegatedExecutionResult)
        }
        result.get().data == data
    }

    def "query to two services with field rename"() {

        def nsdl = """
         service Foo {
            type Query{
                foo: Foo  <= \$source.fooOriginal 
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
                name: String <= \$source.title
            }
         }
        """
        def query = """
        { otherFoo: foo {name} bar{name}}
        """
        def underlyingSchema1 = TestUtil.schema("""
            type Query{
                fooOriginal: Foo  
                
            } 
            type Foo {
                name: String
            }
        """)
        def underlyingSchema2 = TestUtil.schema("""
            type Query{
                bar: Bar 
            } 
            type Bar {
                title: String
            }
        """)
        ServiceExecution delegatedExecution1 = Mock(ServiceExecution)
        ServiceExecution delegatedExecution2 = Mock(ServiceExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            ServiceExecution getDelegatedExecution(String serviceName) {
                return serviceName == "Foo" ? delegatedExecution1 : delegatedExecution2
            }

            @Override
            GraphQLSchema getUnderlyingSchema(String serviceName) {
                return serviceName == "Foo" ? underlyingSchema1 : underlyingSchema2
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(new StringReader(nsdl))
                .serviceDataFactory(serviceDataFactory)
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
        result.get().data == [otherFoo: [name: "Foo"], bar: [name: "Bar"]]
    }

    def "query with three nested hydrations"() {

        def nsdl = """
         service Foo {
            type Query{
                foo: Foo  
            } 
            type Foo {
                name: String
                bar: Bar <= \$innerQueries.Bar.barById(id: \$source.barId)
            }
         }
         service Bar {
            type Query{
                bar: Bar 
            } 
            type Bar {
                name: String 
                nestedBar: Bar <= \$innerQueries.Bar.barById(id: \$source.nestedBarId)
            }
         }
        """
        def underlyingSchema1 = TestUtil.schema("""
            type Query{
                foo: Foo  
            } 
            type Foo {
                name: String
                barId: ID
            }
        """)
        def underlyingSchema2 = TestUtil.schema("""
            type Query{
                bar: Bar 
                barById(id: ID): Bar
            } 
            type Bar {
                id: ID
                name: String
                nestedBarId: ID
            }
        """)

        def query = """
            { foo { bar { name nestedBar {name nestedBar { name } } } } }
        """
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            ServiceExecution getDelegatedExecution(String serviceName) {
                return serviceName == "Foo" ? serviceExecution1 : serviceExecution2
            }

            @Override
            GraphQLSchema getUnderlyingSchema(String serviceName) {
                return serviceName == "Foo" ? underlyingSchema1 : underlyingSchema2
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(new StringReader(nsdl))
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def topLevelData = [foo: [barId: "barId123"]]
        def hydrationData1 = [barById: [name: "BarName", nestedBarId: "nestedBarId123"]]
        def hydrationData2 = [barById: [name: "NestedBarName1", nestedBarId: "nestedBarId456"]]
        def hydrationData3 = [barById: [name: "NestedBarName2"]]
        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult1 = new ServiceExecutionResult(hydrationData1)
        ServiceExecutionResult hydrationResult2 = new ServiceExecutionResult(hydrationData2)
        ServiceExecutionResult hydrationResult3 = new ServiceExecutionResult(hydrationData3)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >> completedFuture(topLevelResult)
        1 * serviceExecution2.execute(_) >> completedFuture(hydrationResult1)
        1 * serviceExecution2.execute(_) >> completedFuture(hydrationResult2)
        1 * serviceExecution2.execute(_) >> completedFuture(hydrationResult3)
        result.get().data == [foo: [bar: [name: "BarName", nestedBar: [name: "NestedBarName1", nestedBar: [name: "NestedBarName2"]]]]]
    }

    def "simple mutation"() {

        def nsdl = """
         service MyService {
            type Query {
                foo: String
            }
            type Mutation{
                hello: String  
            } 
         }
        """
        def query = """
        mutation M{ hello }
        """
        def underlyingSchema = TestUtil.schema("""
            type Query {
                foo: String
            }
            type Mutation{
                hello: String
            } 
        """)
        ServiceExecution delegatedExecution = Mock(ServiceExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            ServiceExecution getDelegatedExecution(String serviceName) {
                return delegatedExecution
            }

            @Override
            GraphQLSchema getUnderlyingSchema(String serviceName) {
                underlyingSchema
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(new StringReader(nsdl))
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: "world"]
        ServiceExecutionResult delegatedExecutionResult = new ServiceExecutionResult(data)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert AstPrinter.printAstCompact(params.query) == "mutation M {hello}"
            completedFuture(delegatedExecutionResult)
        }
        result.get().data == data


    }
}
