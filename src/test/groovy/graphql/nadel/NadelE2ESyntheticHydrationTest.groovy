package graphql.nadel

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
class NadelE2ESyntheticHydrationTest extends Specification {

    def "simple hydration query with a synthetic field"() {

        def underlyingSchema1 = typeDefinitions("""
        type Query {
            issue(id: ID): Issue
        }
        type Issue {
            id: ID
            projectId: ID
        }
        """)

        def underlyingSchema2 = typeDefinitions("""
        type Query {
            projects: ProjectsQuery
        }
        type ProjectsQuery {
            project(id: ID) : Project
        }

        type Project {
            id: ID
            name: String
        }
        """)

        def nsdl = '''
        service service1 {
            type Query {
                issue(id: ID): Issue
            }
            type Issue {
                id: ID
                project: Project => hydrated from service2.projects.project(id: $source.projectId)
            }
        }
        service service2 {
            type Query {
                projects: ProjectsQuery
            }
            type ProjectsQuery {
                project(id: ID) : Project
            }
            type Project {
                id: ID
                name: String
            }
        }
        '''

        def query = '''
            { issue { project { name } } }
        '''
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                service1: new Tuple2(serviceExecution1, underlyingSchema1),
                service2: new Tuple2(serviceExecution2, underlyingSchema2)]
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
        def topLevelData = [issue: [id: "1", projectId:"project1"]]

        def hydrationData = [projects: [project: [id: "project1", name: "Project 1"]]]

        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult1_1 = new ServiceExecutionResult(hydrationData)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >>
                completedFuture(topLevelResult)

        1 * serviceExecution2.execute(_) >>
                completedFuture(hydrationResult1_1)

        result.join().data == [issue: [project: [name: "Project 1"]]]
    }

    def "batched hydration query with a synthetic field"() {

        def underlyingSchema1 = typeDefinitions("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorIds: [ID]
        }
        """)
        def underlyingSchema2 = typeDefinitions("""
        type Query {
            users: UsersQuery
        }
        
        type UsersQuery {
           usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
            name: String
        }
        """)

        def nsdl = '''
        service service1 {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authors: [User] => hydrated from service2.users.usersByIds(id: $source.authorIds) object identified by id
            }
        }
        service service2 {
            type Query {
                users: UsersQuery
            }
        
            type UsersQuery {
                usersByIds(id: [ID]): [User] default batch size 3
            }
            type User {
                id: ID
                lastName: String
            }
        }
        '''

        def query = "{issues {id authors {id}}}"
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                service1: new Tuple2(serviceExecution1, underlyingSchema1),
                service2: new Tuple2(serviceExecution2, underlyingSchema2)]
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

        def issue1 = [id: "ISSUE-1", authorIds: ["USER-1", "USER-2"]]
        def issue2 = [id: "ISSUE-2", authorIds: ["USER-3"]]
        def issue3 = [id: "ISSUE-3", authorIds: ["USER-2", "USER-4", "USER-5",]]
        def topLevelData = [issues: [issue1, issue2, issue3]]

        def batchResponse1 = [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-3", object_identifier__UUID: "USER-3"]]
        def hydrationData1 = [users: [usersByIds: batchResponse1]]

        def batchResponse2 = [[id: "USER-2", object_identifier__UUID: "USER-2"], [id: "USER-4", object_identifier__UUID: "USER-4"], [id: "USER-5", object_identifier__UUID: "USER-5"]]
        def hydrationData2 = [users: [usersByIds: batchResponse2]]

        ServiceExecutionResult topLevelResult = new ServiceExecutionResult(topLevelData)
        ServiceExecutionResult hydrationResult1 = new ServiceExecutionResult(hydrationData1)
        ServiceExecutionResult hydrationResult2 = new ServiceExecutionResult(hydrationData2)
        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >>
                completedFuture(topLevelResult)

        1 * serviceExecution2.execute(_) >>
                completedFuture(hydrationResult1)

        1 * serviceExecution2.execute(_) >>
                completedFuture(hydrationResult2)

        def issue1Result = [id: "ISSUE-1", authors: [[id: "USER-1"], [id: "USER-2"]]]
        def issue2Result = [id: "ISSUE-2", authors: [[id: "USER-3"]]]
        def issue3Result = [id: "ISSUE-3", authors: [[id: "USER-2"], [id: "USER-4"], [id: "USER-5"]]]

        result.join().data == [issues: [issue1Result, issue2Result, issue3Result]]
    }

    def "query with three nested hydrations and synthetic fields"() {

        def nsdl = '''
         service Foo {
            type Query{
               foos: [Foo]  
            }
            type Foo {
                name: String
                bar: Bar => hydrated from Bar.barsQuery.barsById(id: $source.barId) object identified by barId, batch size 2
            }
         }
         service Bar {
            type Query{
                barsQuery: BarQuery
            } 
            type BarQuery {
                bar: Bar 
                barsById(id: [ID]): [Bar]
            }
            type Bar {
                barId: ID
                name: String 
                nestedBar: Bar => hydrated from Bar.barsQuery.barsById(id: $source.nestedBarId) object identified by barId
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
                barsQuery: BarQuery
            } 
            type BarQuery {
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
        def hydrationDataBatch1 = [barsQuery: [barsById: [[object_identifier__UUID: "bar1", name: "Bar 1", nestedBarId: "nestedBar1"], [object_identifier__UUID: "bar2", name: "Bar 2", nestedBarId: "nestedBar2"]]]]
        def hydrationDataBatch2 = [barsQuery: [barsById: [[object_identifier__UUID: "bar3", name: "Bar 3", nestedBarId: null]]]]
        def hydrationData2 = [barsQuery: [barsById: [[object_identifier__UUID: "nestedBar1", name: "NestedBarName1", nestedBarId: "nestedBarId456"]]]]
        def hydrationData3 = [barsQuery: [barsById: [[object_identifier__UUID: "nestedBarId456", name: "NestedBarName2"]]]]
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

    def "extending types from another service is possible with synthetic fields"() {
        def ndsl = '''
         service Service1 {
            extend type Query{
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
            extend type Root {
                extension: Extension => hydrated from Service2.lookUpQuery.lookup(id: $source.id) object identified by id 
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
                lookUpQuery: LookUpQuery
            }
            
            type LookUpQuery {
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
        { 
        root {
            id
            name
            extension {
                id
                name
            }
        }
        anotherRoot
        }
        """
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .executionId(ExecutionId.from("fromInput"))
                .build()


        def service1Data1 = [root: [id: "rootId", name: "rootName"]]
        def service1Data2 = [anotherRoot: "anotherRoot"];
        def service2Data = [lookUpQuery: [lookup: [id: "rootId", name: "extensionName"]]]
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
            assert printAstCompact(params.getQuery()) == "query nadel_2_Service2 {lookUpQuery {lookup(id:\"rootId\") {id name}}}"
            completedFuture(new ServiceExecutionResult(service2Data))
        }

        result.data == [anotherRoot: "anotherRoot", root: [id: "rootId", name: "rootName", extension: [id: "rootId", name: "extensionName"]]]

    }

    def "hydration call with fragments in the hydrated part and synthetic field"() {
        def issueSchema = typeDefinitions("""
        type Query {
            issues : [Issue]
        }
        type Issue {
            id: ID
            authorDetails: [AuthorDetail]
        }
        type AuthorDetail {
            authorId: ID
            name: String
        }
        """)
        def userServiceSchema = typeDefinitions("""
        type Query {
            userQuery: UserQuery
        }
  
        type UserQuery {
            usersByIds(id: [ID]): [User]
        }
        type User {
            id: ID
            name: String
        }
        """)

        def nsdl = '''
        service Issues {
            type Query {
                issues: [Issue]
            }
            type Issue {
                id: ID
                authorDetails: [AuthorDetail]
                authors: [User] => hydrated from UserService.userQuery.usersByIds(id: $source.authorDetails.authorId) object identified by id
            }
            type AuthorDetail {
                name: String
            }
        }
        service UserService {
            type Query {
                userQuery: UserQuery
            }
            type UserQuery {
                usersByIds(id: [ID]): [User] default batch size 2
            }
            type User {
                id: ID
                name: String
            }
        }
        '''

        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Issues: new Tuple2(serviceExecution1, issueSchema),
                UserService: new Tuple2(serviceExecution2, userServiceSchema)]
        )

        def query = """
            fragment IssueFragment on Issue {
                id
            } 
            {
                issues {...IssueFragment 
                    id 
                    authors {
                        id ...UserFragment1
                    } 
                } 
                userQuery {
                    usersByIds(id: ["USER-1"]){ ...UserFragment1 }
                }
            }
            fragment UserFragment1 on User {
               id 
               name
               ...UserFragment2
            }
            fragment UserFragment2 on User {
                name
            } 
        """
        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        def issue1 = [id: "ISSUE-1", authorDetails: [[authorId: "USER-1"], [authorId: "USER-2"]]]
        def response1 = new ServiceExecutionResult([issues: [issue1]])

        def user1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"]]
        def response2 = new ServiceExecutionResult([userQuery:[usersByIds: user1]])

        def batchResponse1 = [[id: "USER-1", name: "User 1", object_identifier__UUID: "USER-1"], [id: "USER-2", name: "User 2", object_identifier__UUID: "USER-2"]]
        def response3 = new ServiceExecutionResult([userQuery:[usersByIds: batchResponse1]])

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response1)

        then:
        1 * serviceExecution2.execute(_) >> completedFuture(response2)

        then:
        1 * serviceExecution2.execute(_) >> completedFuture(response3)

        def issue1Result = [id: "ISSUE-1", authors: [[id: "USER-1", name: "User 1"], [id: "USER-2", name: "User 2"]]]
        result.join().data == [issues: [issue1Result], userQuery: [usersByIds: [[id: "USER-1", name: "User 1"]]]]
    }

    def "hydration with renamed type and synthetic field"() {
        def underlyingFooSchema = typeDefinitions("""
            type Query {
                fooOriginal: Foo
            }
            type Foo {
                id: ID!
                fooBarId: ID
            }
        """)
        def underlyingBarSchema = typeDefinitions("""
            type Query {
                bars: BarQuery
            }
            type BarQuery {
              barById(id: ID!): Bar
            }
            type Bar {
                id: ID
            }
        """)
        def nsdl = '''
            service Foo {
                type Query {
                    foo: Foo => renamed from fooOriginal
                }
                type Foo {
                    id: ID!
                    fooBar: Bar => hydrated from Bar.bars.barById(id: \$source.fooBarId)
                }
            }
            service Bar {
                type Query {
                    bars: BarQuery
                }
                type BarQuery {
                   barById(id: ID!): Bar
                }
                type Bar {
                    id: ID!
                }
            }
        '''
        def query = """
            {
                foo {
                    id
                    fooBar {
                        id
                    }
                }
            }
        """

        ServiceExecution serviceExecution1 = Mock(ServiceExecution)
        ServiceExecution serviceExecution2 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                Foo: new Tuple2(serviceExecution1, underlyingFooSchema),
                Bar: new Tuple2(serviceExecution2, underlyingBarSchema)]
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

        def data1 = [fooOriginal: [id: "Foo", fooBarId: "hydrated-bar"]]
        def data2 = [bars:[barById: [id: "hydrated-bar"]]]
        def response1 = new ServiceExecutionResult(data1)
        def response2 = new ServiceExecutionResult(data2)

        when:
        def result = nadel.execute(nadelExecutionInput)

        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response1)

        1 * serviceExecution2.execute(_) >> completedFuture(response2)


        result.join().data == [
                foo: [
                        id    : "Foo",
                        fooBar: [
                                id: "hydrated-bar",
                        ],
                ],
        ]
    }

    def "hydration call over itself within renamed types and synthetic field"() {
        def testingSchema = typeDefinitions("""
        type Query {
            tests: TestQuery
        }
        type TestQuery {
            testing: Testing
            characters(ids: [ID!]!): [Character]
        }

        type Testing {
            movies: [Movie]
        }

        type Character {
            id: ID!
            name: String
        }

        type Movie {
            id: ID!
            name: String
            characterIds: [ID]
        }
        """)
        def nsdl = '''
        service testing {
            type Query {
                tests: TestQuery
            }
            type TestQuery {
               testing: Testing
            }

            type Testing {
                movies: [TestingMovie]
            }
            type TestingCharacter => renamed from Character   {
                id: ID!
                name: String
            }

            type TestingMovie => renamed from Movie {
                id: ID!
                name: String
                characters: [TestingCharacter] => hydrated from testing.tests.characters(ids: $source.characterIds) object identified by id, batch size  3
            }
        }
        '''
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                testing: new Tuple2(serviceExecution1, testingSchema)]
        )

        def query = """
                    {
                        tests { 
                         testing {
                            movies {
                              id
                              name
                               characters {
                                 id
                                 name
                               }
                            }
                          }
                        }
                    }
        """

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()


        def movies = [[id: "M1", name: "Movie 1", characterIds: ["C1", "C2"]], [id: "M2", name: "Movie 2", characterIds: ["C1", "C2", "C3"]]]
        def response1 = new ServiceExecutionResult([tests: [testing: [movies: movies]]])

        def characters1 = [[id: "C1", name: "Luke", object_identifier__UUID: "C1"], [id: "C2", name: "Leia", object_identifier__UUID: "C2"], [id: "C1", name: "Luke", object_identifier__UUID: "C1"]]
        def response2 = new ServiceExecutionResult([tests:[characters: characters1]])

        def characters2 = [[id: "C2", name: "Leia", object_identifier__UUID: "C2"], [id: "C3", name: "Anakin", object_identifier__UUID: "C3"]]
        def response3 = new ServiceExecutionResult([tests:[characters: characters2]])

        when:
        def result = nadel.execute(nadelExecutionInput)


        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response1)

        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response2)

        1 * serviceExecution1.execute(_) >> completedFuture(response3)

        def data = [movies: [[id: "M1", name: "Movie 1", characters: [[id: "C1", name: "Luke"], [id: "C2", name: "Leia"]]], [id: "M2", name: "Movie 2", characters: [[id: "C1", name: "Luke"], [id: "C2", name: "Leia"], [id: "C3", name: "Anakin"]]]]]
        result.join().data == [tests: [testing: data]]
    }

    def "simple hydration with one service and synthetic field and type renaming"() {
        def testingSchema = typeDefinitions("""
        type Query {
            tests: TestQuery
        }
        type TestQuery {
            testing: Testing
            character(id: ID): Character
        }

        type Testing {
            movie: Movie
        }

        type Character {
            id: ID!
            name: String
        }

        type Movie {
            id: ID!
            name: String
            characterId: ID
        }
        """)

        def nsdl = '''
        service testing {
            type Query {
                tests: TestQuery
            }
            type TestQuery {
               testing: Testing
            }

            type Testing {
                movie: Movie
            }
            type TestingCharacter => renamed from Character   {
                id: ID!
                name: String
            }

            type Movie {
                id: ID!
                name: String
                character: TestingCharacter => hydrated from testing.tests.character(id: $source.characterId) object identified by id, batch size  3
            }
        }
        '''
        ServiceExecution serviceExecution1 = Mock(ServiceExecution)

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                testing: new Tuple2(serviceExecution1, testingSchema)]
        )

        def query = """
                    {
                        tests { 
                         testing {
                            movie {
                              id
                              name
                               character {
                                 id
                                 name
                               }
                            }
                          }
                        }
                    }
        """

        given:
        Nadel nadel = newNadel()
                .dsl(nsdl)
                .serviceExecutionFactory(serviceFactory)
                .build()

        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .artificialFieldsUUID("UUID")
                .build()

        def movies = [id: "M1", name: "Movie 1", characterId:"C1"]
        def response1 = new ServiceExecutionResult([tests: [testing: [movie: movies]]])

        def characters1 = [id: "C1", name: "Luke", object_identifier__UUID: "C1"]
        def response2 = new ServiceExecutionResult([tests:[character: characters1]])

        when:
        def result = nadel.execute(nadelExecutionInput)


        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response1)

        then:
        1 * serviceExecution1.execute(_) >> completedFuture(response2)

        def data = [movie: [id: "M1", name: "Movie 1", character: [id: "C1", name: "Luke"]]]
        result.join().data == [tests:[testing: data]]
    }

    ExecutionIdProvider idProvider = new ExecutionIdProvider() {
        @Override
        ExecutionId provide(String queryStr, String operationName, Object context) {
            return ExecutionId.from("fromProvider")
        }
    }


}
