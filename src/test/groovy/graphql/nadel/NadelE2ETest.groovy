package graphql.nadel

import graphql.schema.GraphQLSchema
import spock.lang.Specification

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {


    def "query to one service"() {

        DelegatedExecution delegatedExecution = Mock(DelegatedExecution)
        ServiceDataFactory serviceDataFactory = new ServiceDataFactory() {
            @Override
            DelegatedExecution getDelegatedExecution(String serviceName) {
                return delegatedExecution;
            }

            @Override
            GraphQLSchema getPrivateSchema(String serviceName) {
            }
        }
        def nsdl = """
         service MyService {
            type Query{
                hello: String
            } 
         }
        """
        def query = """
        { hello }
        """
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceDataFactory(serviceDataFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [hello: "world"]
        DelegatedExecutionResult delegatedExecutionResult = new DelegatedExecutionResult(data)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * delegatedExecution.delegate(_) >> completedFuture(delegatedExecutionResult)
        result.get().data == data

    }
}
