package graphql.nadel


import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
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

    def simpleUnderlyingSchema = TestUtil.schema("""
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
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)


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
        result.errors.collect( {ge -> ge.message}) == ["Problem1", "Problem2"]

    }
    def "errors and no data from a service execution are reflected in the result"() {
        given:
        def query = '''
            { hello { name } }
        '''

        Nadel nadel = buildNadel()

        List<Map<String, Object>> rawErrors = [[message: "Problem1"], [message: "Problem2"]]

        1 * delegatedExecution.execute(_) >> { args ->
            completedFuture(new ServiceExecutionResult([hello : [name : "World"]], rawErrors))
        }


        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [hello: [name : "World"]]
        !result.errors.isEmpty()
        result.errors.collect( {ge -> ge.message}) == ["Problem1", "Problem2"]
    }
}
