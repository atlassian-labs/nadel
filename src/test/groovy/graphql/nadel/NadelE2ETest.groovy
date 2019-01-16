package graphql.nadel

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
                name: String
            }
         }
        """
        def query = """
        { hello {name}}
        """
        def privateSchema = TestUtil.schema("""
            type Query{
                hello: World  
            } 
            type World {
                name: String
            }
        """)
        DelegatedExecution delegatedExecution = Mock(DelegatedExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            DelegatedExecution getDelegatedExecution(String serviceName) {
                return delegatedExecution
            }

            @Override
            GraphQLSchema getPrivateSchema(String serviceName) {
                privateSchema
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: [name: "earth"]]
        DelegatedExecutionResult delegatedExecutionResult = new DelegatedExecutionResult(data)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.delegate(_) >> completedFuture(delegatedExecutionResult)
        result.get().data == data

    }

    def "query to two services"() {

        def nsdl = """
         service Foo {
            type Query{
                foo: Foo  
                
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
                name: String
            }
         }
        """
        def query = """
        { foo {name} bar{name}}
        """
        def privateSchema1 = TestUtil.schema("""
            type Query{
                foo: Foo  
                
            } 
            type Foo {
                name: String
            }
        """)
        def privateSchema2 = TestUtil.schema("""
            type Query{
                bar: Bar
            } 
            type Bar {
                name: String
            }
        """)
        DelegatedExecution delegatedExecution1 = Mock(DelegatedExecution)
        DelegatedExecution delegatedExecution2 = Mock(DelegatedExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            DelegatedExecution getDelegatedExecution(String serviceName) {
                return serviceName == "Foo" ? delegatedExecution1 : delegatedExecution2
            }

            @Override
            GraphQLSchema getPrivateSchema(String serviceName) {
                return serviceName == "Foo" ? privateSchema1 : privateSchema2
            }
        }
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data1 = [foo: [name: "Foo"]]
        def data2 = [bar: [name: "Bar"]]
        DelegatedExecutionResult delegatedExecutionResult1 = new DelegatedExecutionResult(data1)
        DelegatedExecutionResult delegatedExecutionResult2 = new DelegatedExecutionResult(data2)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution1.delegate(_) >> completedFuture(delegatedExecutionResult1)
        1 * delegatedExecution2.delegate(_) >> completedFuture(delegatedExecutionResult2)
        result.get().data == data1 + data2

    }
}
