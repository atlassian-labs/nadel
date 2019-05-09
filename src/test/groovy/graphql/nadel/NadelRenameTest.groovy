package graphql.nadel

import graphql.nadel.testutils.TestUtil
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelRenameTest extends Specification {

    def simpleNDSL = '''
         service MyService {
            type Query{
                hello: World  
                renameOverall : RenameOverall => renamed from renameUnderlying   # the field is renamed 
            } 
            type World {
                id: ID
                name: String
            }
            type RenameOverall => renamed from RenameUnderlying {  # and the type is renamed
                name : String
            }
         }
        '''

    def simpleUnderlyingSchema = typeDefinitions('''
            type Query{
                hello: World  
                renameUnderlying : RenameUnderlying
            } 
            type World {
                id: ID
                name: String
            }

            type RenameUnderlying {
                name : String
            }

        ''')

    def delegatedExecution = Mock(ServiceExecution)
    def serviceFactory = TestUtil.serviceFactory(delegatedExecution, simpleUnderlyingSchema)

    def nadel = newNadel()
            .dsl(simpleNDSL)
            .serviceExecutionFactory(serviceFactory)
            .build()

    def "simple type rename and field rename works as expected"() {
        def query = '''
        { renameOverall { name } }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [renameUnderlying: [name: "val"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "query nadel_2_MyService {renameUnderlying {name}}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.data == [renameOverall: [name: "val"]]
    }

    def "fragment type rename and field rename works as expected"() {
        def query = '''
        { 
            renameOverall { 
                ... on RenameOverall {
                    name
                } 
                ... FragDef
            } 
        }
        
        fragment FragDef on RenameOverall {
            name
        }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [renameUnderlying: [name: "val"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) ==
                    "query nadel_2_MyService {renameUnderlying {... on RenameUnderlying {name} ...FragDef}} fragment FragDef on RenameUnderlying {name}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.errors.isEmpty()
        result.data == [renameOverall: [name: "val"]]
    }
}
