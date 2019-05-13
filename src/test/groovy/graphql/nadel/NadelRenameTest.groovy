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
                renameObject : ObjectOverall => renamed from renameObjectUnderlying   # the field is renamed
                renameInterface : InterfaceOverall => renamed from renameInterfaceUnderlying
                renameUnion : UnionOverall => renamed from renameUnionUnderlying
            } 
            
            type World {
                id: ID
                name: String
            }
            
            type ObjectOverall implements InterfaceOverall => renamed from ObjectUnderlying {  # and the type is renamed
                name : String
            }
            
            interface InterfaceOverall => renamed from InterfaceUnderlying {
                name : String
            }
            
            union UnionOverall => renamed from UnionUnderlying = X | Y
            
            type X => renamed from XUnderlying {
                x : Int
            }

            type Y => renamed from YUnderlying {
                y : Int
            }
         }
        '''

    def simpleUnderlyingSchema = typeDefinitions('''
            type Query{
                hello: World  
                renameObjectUnderlying : ObjectUnderlying
                renameInterfaceUnderlying : InterfaceUnderlying
                renameUnionUnderlying : UnionUnderlying
            } 
            type World {
                id: ID
                name: String
            }

            type ObjectUnderlying implements InterfaceUnderlying {
                name : String
            }

            interface InterfaceUnderlying {
                name : String
            }
            
            union UnionUnderlying = XUnderlying | YUnderlying
            
            type XUnderlying {
                x : Int
            }

            type YUnderlying {
                y : Int
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
        { renameObject { name } }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [renameObjectUnderlying: [name: "val"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) == "query nadel_2_MyService {renameObjectUnderlying {name}}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.data == [renameObject: [name: "val"]]
    }

    def "fragment type rename and field rename works as expected"() {
        def query = '''
        { 
            renameObject { 
                ... on ObjectOverall {
                    name
                } 
                ... FragDef
            } 
        }
        
        fragment FragDef on ObjectOverall {
            name
        }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [renameObjectUnderlying: [name: "val"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            assert printAstCompact(params.query) ==
                    "query nadel_2_MyService {renameObjectUnderlying {... on ObjectUnderlying {name} ...FragDef}} fragment FragDef on ObjectUnderlying {name}"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.errors.isEmpty()
        result.data == [renameObject: [name: "val"]]
    }


    def "interface rename with fragments works as expected"() {
        def query = '''
        { 
            renameObject { 
                ... on InterfaceOverall { name } 
            }
        }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .build()
        def data = [renameObjectUnderlying: [name: "val"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            def q = printAstCompact(params.query)
            assert q == "query nadel_2_MyService {renameObjectUnderlying {... on InterfaceUnderlying {name}}}", "Unexpected query: $q"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.errors.isEmpty()
        result.data == [renameObject: [name: "val"]]
    }

    def "interface rename direct works as expected"() {
        def query = '''
        { 
            renameInterface { 
                name 
            }
        }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("xxx")
                .build()
        //
        // note how we have to give back an implementation object type not and interface
        //
        def data = [renameInterfaceUnderlying: [name: "val", typename__xxx: "ObjectUnderlying"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            def q = printAstCompact(params.query)
            assert q == "query nadel_2_MyService {renameInterfaceUnderlying {name typename__xxx:__typename}}", "Unexpected query: $q"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.errors.isEmpty()
        result.data == [renameInterface: [name: "val"]]
    }


    def "union rename direct works as expected"() {
        def query = '''
        { 
            renameUnion { 
                ... on X {
                    x
                } 
            }
        }
        '''

        given:
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("xxx")
                .build()
        //
        // note how we have to give back an implementation object type not and interface
        //
        def data = [renameUnionUnderlying: [x: 1, typename__xxx: "XUnderlying"]]
        when:
        def result = nadel.execute(nadelExecutionInput).join()

        then:
        1 * delegatedExecution.execute(_) >> { args ->
            ServiceExecutionParameters params = args[0]
            def q = printAstCompact(params.query)
            assert q == "query nadel_2_MyService {renameUnionUnderlying {... on XUnderlying {x} typename__xxx:__typename}}", "Unexpected query: $q"
            completedFuture(new ServiceExecutionResult(data))
        }
        result.errors.isEmpty()
        result.data == [renameUnion: [x: 1]]
    }
}
