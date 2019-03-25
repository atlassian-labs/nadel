package graphql.nadel

import graphql.nadel.testutils.MockServiceExecution
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.schema
import static graphql.nadel.testutils.TestUtil.serviceFactory
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelErrorHandlingTest extends Specification {

    def simpleNDSL = """
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
        """

    def simpleUnderlyingSchema = schema("""
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
        """)

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = serviceFactory(delegatedExecution, simpleUnderlyingSchema)


    def buildNadel() {
        newNadel()
                .dsl(simpleNDSL)
                .serviceDataFactory(serviceFactory)
                .build()
    }

    def "errors and some data from a service execution are reflected in the result"() {
        given:
        def query = '''
            { hello { name } }
        '''

        Nadel nadel = buildNadel()

        List<Map<String, Object>> rawErrors = [[message: "Problem1"], [message: "Problem2"]]

        1 * delegatedExecution.execute(_) >> { args ->
            completedFuture(new ServiceExecutionResult(null, rawErrors))
        }


        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [hello: null]
        !result.errors.isEmpty()
        result.errors.collect({ ge -> ge.message }) == ["Problem1", "Problem2"]

    }

    def "errors and no data from a service execution are reflected in the result"() {
        given:
        def query = '''
            { hello { name } }
        '''

        Nadel nadel = buildNadel()

        List<Map<String, Object>> rawErrors = [[message: "Problem1"], [message: "Problem2"]]

        1 * delegatedExecution.execute(_) >> { args ->
            completedFuture(new ServiceExecutionResult([hello: [name: "World"]], rawErrors))
        }


        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [hello: [name: "World"]]
        !result.errors.isEmpty()
        result.errors.collect({ ge -> ge.message }) == ["Problem1", "Problem2"]
    }

    def "query with hydration that fail with errors"() {

        def nsdl = '''
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
        '''
        def underlyingSchema1 = schema('''
            type Query{
                foo: Foo  
            } 
            type Foo {
                name: String
                barId: ID
            }
        ''')
        def underlyingSchema2 = schema('''
            type Query{
                bar: Bar 
                barById(id: ID): Bar
            } 
            type Bar {
                id: ID
                name: String
                nestedBarId: ID
            }
        ''')

        def query = '''
            { foo { bar { name nestedBar {name nestedBar { name } } } } }
        '''
        def topLevelData = [foo: [barId: "barId123"]]
        def hydrationData = [barById: null]
        ServiceExecution serviceExecution1 = new MockServiceExecution(topLevelData)
        ServiceExecution serviceExecution2 = new MockServiceExecution(hydrationData,
                [[message: "Error during hydration"]])

        ServiceDataFactory serviceFactory = serviceFactory([
                Foo: new Tuple2(serviceExecution1, underlyingSchema1),
                Bar: new Tuple2(serviceExecution2, underlyingSchema2)]
        )
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()

        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        !result.errors.isEmpty()
        result.errors[0] == [[message: "error during hydration"]]
        result.data == [foo: [bar: null]]
    }

}
