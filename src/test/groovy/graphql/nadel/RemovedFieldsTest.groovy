package graphql.nadel

import graphql.GraphQLError
import graphql.execution.AbortExecutionException
import graphql.language.Field
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.testutils.TestUtil
import graphql.nadel.testutils.harnesses.IssuesCommentsUsersHarness
import graphql.schema.GraphQLFieldDefinition

import static graphql.nadel.Nadel.newNadel
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput

class RemovedFieldsTest extends StrategyTestHelper {

    def "hydrated field in query is removed"() {
        given:
        def query = """
        {
            commentById(id:"C1") {
                author {
                    displayName
                    userId
                }
            }
        }
        """
        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["userId"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [commentById: [author: [displayName: "Display name of fred", userId: null]]]
    }

    def "field in non-hydrated query is removed"() {
        given:
        def query = """
        {
            commentById(id:"C1") {
                id
                created
            }
        }
        """
        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["id"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [commentById: [created: "1969-08-08@C1", id: null]]
    }

    def "field is removed from non-hydrated field in query"() {
        given:
        def query = """
        { 
            issueById(id : "I1") { 
                comments {
                    author {
                        displayName 
                    }
                }
            } 
        }
        """
        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["author"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        // What should the expected outcome of this be?
        // [issueById:[comments:[null, null, null]]?
        // [issueById:[comments:null]?
        // [issueById:null]?
        // [issueById:[comments:[[author:null], [author:null], [author:null]]]] ?
        result.data != [issueById: [comments: [[author: null], [author: null], [author: null]]]]
    }

    //query validation error message appears from graphql-java due to empty query
    // if all fields are removed in top level field, then child fields are hidden and only the top field is returned with null
    def "all non-hydrated fields in query are removed"() {
        given:
        def query = """
        {
            commentById(id:"C1") {
                id
                created
                commentText
            }
        }
        """
        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["created", "commentText", "id"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [commentById: null]
    }

    def "top level field is removed"() {
        given:
        def query = """
        {
            commentById(id:"C1") {
                id
            }
        }
        """
        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["commentById"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [commentById: null]
    }

    def "nested hydrated field in query is removed"() {
        given:
        def query = '''
        {
            issues {
                key
                summary
                key
                summary
                reporter {
                    displayName
                }
                comments {
                    commentText
                    author {
                        displayName
                        userId
                    }
                }
            }
        }
        '''


        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["userId"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [issues: [
                [key     : "WORK-I1", summary: "Summary for WORK-I1", reporter: [displayName: "Display name of fred"],
                 comments: [[commentText: "Text of C1", author: [displayName: "Display name of fred", userId: null]],
                            [commentText: "Text of C3", author: [displayName: "Display name of zed", userId: null]],
                            [commentText: "Text of C5", author: [displayName: "Display name of fred", userId: null]]]],
                [key     : "WORK-I2", summary: "Summary for WORK-I2", reporter: [displayName: "Display name of zed"],
                 comments: [[commentText: "Text of C2", author: [displayName: "Display name of ned", userId: null]],
                            [commentText: "Text of C4", author: [displayName: "Display name of jed", userId: null]],
                            [commentText: "Text of C6", author: [displayName: "Display name of ted", userId: null]]]]],
        ]

    }

    def "top level field in batched query is removed"() {
        given:
        def query = '''
        {
            issues {
                key
                comments {
                    commentText
                    author {
                        displayName
                        userId
                    }
                }
            }
        }
        '''


        def serviceExecutionFactory = IssuesCommentsUsersHarness.serviceFactoryWithDelay(2)

        Nadel nadel = newNadel()
                .dsl(IssuesCommentsUsersHarness.ndsl)
                .serviceExecutionFactory(serviceExecutionFactory)
                .serviceExecutionHooks(createServiceExecutionHooksWithFieldRemoval(["comments"]))
                .build()

        when:
        def result = nadel.execute(newNadelExecutionInput().query(query)).join()

        then:
        result.data == [issues: [
                [key: "WORK-I1", comments: null],
                [key: "WORK-I2", comments: null]]]

    }


    ServiceExecutionHooks createServiceExecutionHooksWithFieldRemoval(List<String> fieldsToRemove) {

        return new ServiceExecutionHooks() {
            @Override
            Optional<GraphQLError> isFieldAllowed(Field field, GraphQLFieldDefinition fieldDefinitionOverall, Object userSuppliedContext) {
                if (fieldsToRemove.contains(field.getName())) {
                    //temporary GraphQLError ->  need to implement a field permissions denied error
                    return Optional.of(new AbortExecutionException("removed field"))
                }
                return Optional.empty();
            }
        }
    }

    def "restricted field inside hydration via fragments used twice"() {
        given:
        def overallSchema = TestUtil.schemaFromNdsl('''
        service Issues {
            type Query {
                issue: Issue
            }
            type Issue {
                id: ID
                relatedIssue: Issue                 
                author: User => hydrated from UserService.usersById(id: $source.authorId) object identified by id
            }
        }
        service UserService {
            type Query {
                userByIds(id: ID): User
            }
            type User {
                id: ID
                restricted: String 
            }
        }
        ''')
        def issueSchema = TestUtil.schema("""
        type Query {
            issue : Issue
            myIssue: Issue
        }
        type Issue {
            id: ID
            authorId: ID
            relatedIssue: Issue                 
        }
        """)
        def userServiceSchema = TestUtil.schema("""
        type Query {
            usersById(id: [ID]): [User]
        }
        type User {
            id: ID
            restricted: String
        }
        """)
        def query = "{issue { ...IssueFragment relatedIssue { ...IssueFragment }}} fragment IssueFragment on Issue {id author {id restricted}}"

        def expectedQuery1 = "query nadel_2_Issues {issue {...IssueFragment relatedIssue {...IssueFragment}}} fragment IssueFragment on Issue {id authorId}"
        def response1 = [issue: [id: "ID1", authorId: "USER-1", relatedIssue: [id: "ID2", authorId: "USER-2"]]]

        def expectedQuery2 = "query nadel_2_UserService {usersById(id:[\"USER-1\",\"USER-2\"]) {id object_identifier__UUID:id}}"
        def response2 = [usersById: [[id: "USER-1", object_identifier__UUID: "USER-1"], [id: "USER-2", object_identifier__UUID: "USER-2"]]]

        def overallResponse = [issue: [id: "ID1", author: [id: "USER-1", restricted: null], relatedIssue: [id: "ID2", author: [id: "USER-2", restricted: null]]]]

        def hooks = new ServiceExecutionHooks() {
            @Override
            Optional<GraphQLError> isFieldAllowed(Field field, GraphQLFieldDefinition fieldDefinitionOverall, Object userSuppliedContext) {
                if (fieldDefinitionOverall.getName() == "restricted") {
                    //temporary GraphQLError ->  need to implement a field permissions denied error
                    return Optional.of(new AbortExecutionException("removed field"))
                }
                return Optional.empty();
            }
        }

        expect:
        test2Services(
                overallSchema,
                "Issues",
                issueSchema,
                "UserService",
                userServiceSchema,
                query,
                ["issue", "myIssue"],
                expectedQuery1,
                response1,
                expectedQuery2,
                response2,
                overallResponse,
                hooks
        )


    }

}
