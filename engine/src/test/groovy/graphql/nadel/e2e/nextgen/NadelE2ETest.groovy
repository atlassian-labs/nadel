package graphql.nadel.e2e.nextgen

import graphql.GraphQLError
import graphql.language.AstSorter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.testutils.TestUtil
import graphql.parser.Parser
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.NadelExecutionInput.newNadelExecutionInput
import static graphql.nadel.engine.testutils.TestUtil.typeDefinitions
import static java.util.concurrent.CompletableFuture.completedFuture

class NadelE2ETest extends Specification {
    def "simple deep rename"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue {
                name: String => renamed from detail.detailName
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                issue: Issue 
            } 
            type Issue {
                detail: IssueDetails
            }
            type IssueDetails {
                detailName: String
            }
        """
        def query = """
        { issue { name } } 
        """
        def expectedQuery = """query {
  ... on Query {
    issue {
      ... on Issue {
        my_uuid__detail: detail {
          ... on IssueDetails {
            detailName
          }
        }
      }
      ... on Issue {
        __typename__my_uuid: __typename
      }
    }
  }
}"""
        def overallResponse = [issue: [name: "My Issue"]]
        def serviceResponse = [issue: [__typename__my_uuid: "Issue", my_uuid__detail: [detailName: "My Issue"]]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "deep rename with interfaces"() {
        def serviceName = "PetService"
        def nsdl = [(serviceName): """
         service PetService {
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                name: String 
            }
            type Dog implements Pet {
                name: String => renamed from detail.petName
            }
            type Cat implements Pet {
                name: String => renamed from detail.petName
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                detail: PetDetails
            }
            type Dog implements Pet {
                detail: PetDetails
            }
            type Cat implements Pet {
                detail: PetDetails
            }
            type PetDetails {
                petName: String 
            }
        """
        def query = """
        { pets { name } } 
        """
        def expectedQuery = """query {
  ... on Query {
    pets {
      ... on Dog {
        my_uuid__detail: detail {
          ... on PetDetails {
            petName
          }
        }
      }
      ... on Cat {
        my_uuid__detail: detail {
          ... on PetDetails {
            petName
          }
        }
      }
      ... on Dog {
        __typename__my_uuid: __typename
      }
      ... on Cat {
        __typename__my_uuid: __typename
      }
    }
  }
}"""
        def serviceResponse = [pets: [
                [__typename__my_uuid: "Cat", my_uuid__detail: [petName: "Tiger"]],
                [__typename__my_uuid: "Dog", my_uuid__detail: [petName: "Luna"]],
        ]]

        def overallResponse = [pets: [[name: "Tiger"], [name: "Luna"]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                serviceName,
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "simple hydration"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue {
                author: User => hydrated from UserService.userById(userId: \$source.authorId)
            }
         }
        """, UserService: """
        service UserService {
            type Query {
                userById(userId: ID!): User
            } 
            type User {
                id: ID!
            }
        }
        """]
        def issueUnderlyingSchema = """
            type Query {
                issue: Issue 
            } 
            type Issue {
                authorId: ID!
            }
        """
        def userUnderlyingSchema = """
            type Query {
                userById(userId: ID!): User
            } 
            type User {
                id: ID!
            }
        """
        def query = """
        {
            issue {
                author {
                    id
                }
            }
        } 
        """

        def issueCalls = [
                (Parser.parse("""{
    ... on Query {
        issue {
            ... on Issue {
                hydration_uuid__authorId: authorId
            }
            ... on Issue {
                __typename__hydration_uuid: __typename
            }
        }
    }
}""")): [
                        issue: [
                                __typename__hydration_uuid: "Issue",
                                hydration_uuid__authorId  : "user-1",
                        ],
                ],
        ]

        def userCalls = [
                (Parser.parse("""{
    ... on Query {
        userById(userId: "user-1") {
            ... on User {
                id
            }
        }
    }
}""")): [
                        userById: [
                                id: "user-1"
                        ]
                ],
        ]

        def overallResponse = [issue: [author: [id: "user-1"]]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = testServices(
                nsdl,
                [
                        IssueService: issueUnderlyingSchema,
                        UserService : userUnderlyingSchema,
                ],
                [
                        IssueService: issueCalls,
                        UserService : userCalls,
                ],
                query,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "simple batch hydration"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issues: [Issue]
            } 
            type Issue {
                author: User => hydrated from UserService.usersByIds(userIds: \$source.authorId) object identified by id, batch size 2
            }
         }
        """, UserService: """
        service UserService {
            type User {
                id: ID!
                name: String
            }
        }
        """]
        def issueUnderlyingSchema = """
            type Query {
                issues: [Issue]
            } 
            type Issue {
                authorId: ID!
            }
        """
        def userUnderlyingSchema = """
            type Query {
                usersByIds(userIds: [ID!]!): [User]
            } 
            type User {
                id: ID!
                name: String
            }
        """
        def query = """
        {
            issues {
                author {
                    id
                    name
                }
            }
        } 
        """

        def issueCalls = [
                (Parser.parse("""{
    ... on Query {
        issues {
            ... on Issue {
                kt_batch_hydration__authorId: authorId
            }
            ... on Issue {
                __typename__kt_batch_hydration: __typename
            }
        }
    }
}""")): [
                        issues: [
                                [
                                        __typename__kt_batch_hydration: "Issue",
                                        kt_batch_hydration__authorId  : "user-1",
                                ],
                                [
                                        __typename__kt_batch_hydration: "Issue",
                                        kt_batch_hydration__authorId  : "user-2",
                                ],
                                [
                                        __typename__kt_batch_hydration: "Issue",
                                        kt_batch_hydration__authorId  : "user-5",
                                ],
                        ],
                ],
        ]

        def userCalls = [
                (Parser.parse("""{
    ... on Query {
        usersByIds(userIds: ["user-1", "user-2"]) {
            ... on User {
                id
            }
            ... on User {
                name
            }
        }
    }
}"""))                                 : [
                        usersByIds: [
                                [id: "user-1", name: "Scott"],
                                [id: "user-2", name: "Mike"],
                        ],
                ],
                (Parser.parse("""{
    ... on Query {
        usersByIds(userIds: ["user-5"]) {
            ... on User {
                id
            }
            ... on User {
                name
            }
        }
    }
}""")): [
                        usersByIds: [
                                [id: "user-5", name: "John"],
                        ],
                ],
        ]

        def expectedResponse = [issues: [
                [author: [id: "user-1", name: "Scott"]],
                [author: [id: "user-2", name: "Mike"]],
                [author: [id: "user-5", name: "John"]],
        ]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = testServices(
                nsdl,
                [
                        IssueService: issueUnderlyingSchema,
                        UserService : userUnderlyingSchema,
                ],
                [
                        IssueService: issueCalls,
                        UserService : userCalls,
                ],
                query,
        )
        then:
        errors.size() == 0
        response == expectedResponse
    }

    def "hydration with deep rename"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue {
                author: User => hydrated from UserService.userById(userId: \$source.authorId)
            }
         }
        """, UserService: """
        service UserService {
            type Query {
                userById(userId: ID!): User
            } 
            type User {
                id: ID!
                name: String => renamed from details.name
            }
        }
        """]
        def issueUnderlyingSchema = """
            type Query {
                issue: Issue 
            } 
            type Issue {
                authorId: ID!
            }
        """
        def userUnderlyingSchema = """
            type Query {
                userById(userId: ID!): User
            } 
            type User {
                id: ID!
                details: UserDetails
            }
            type UserDetails {
                name: String
            }
        """
        def query = """
        {
            issue {
                author {
                    id
                    name
                }
            }
        } 
        """

        def issueCalls = [
                (Parser.parse("""{
    ... on Query {
        issue {
            ... on Issue {
                hydration_uuid__authorId: authorId
            }
            ... on Issue {
                __typename__hydration_uuid: __typename
            }
        }
    }
}""")): [
                        issue: [
                                __typename__hydration_uuid: "Issue",
                                hydration_uuid__authorId  : "user-1",
                        ],
                ],
        ]

        def userCalls = [
                (Parser.parse("""{
    ... on Query {
        userById(userId: "user-1") {
            ... on User {
                id
            }
            ... on User {
                my_uuid__details: details {
                    ... on UserDetails {
                        name
                    }
                }
            }
            ... on User {
                __typename__my_uuid: __typename
            }
        }
    }
}""")): [
                        userById: [
                                __typename__my_uuid: "User",
                                id                 : "user-1",
                                my_uuid__details   : [name: "Atlassian"],
                        ]
                ],
        ]

        def overallResponse = [issue: [author: [id: "user-1", name: "Atlassian"]]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = testServices(
                nsdl,
                [
                        IssueService: issueUnderlyingSchema,
                        UserService : userUnderlyingSchema,
                ],
                [
                        IssueService: issueCalls,
                        UserService : userCalls,
                ],
                query,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "different deep renames on same normalized field"() {
        def serviceName = "PetService"
        def nsdl = [(serviceName): """
         service PetService {
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                name: String 
            }
            type Dog implements Pet {
                name: String => renamed from collar.petName
            }
            type Cat implements Pet {
                name: String => renamed from microchip.petName
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                pets: [Pet]
            } 
            interface Pet {
                id: ID
            }
            type Dog implements Pet {
                id: ID
                collar: Collar
            }
            type Cat implements Pet {
                id: ID
                microchip: Microchip
            }
            type Microchip {
                petName: String
            }
            type Collar {
                petName: String
            }
        """
        def query = """
        { pets { name } } 
        """
        def expectedQuery = """query {
  ... on Query {
    pets {
      ... on Dog {
        my_uuid__collar: collar {
          ... on Collar {
            petName
          }
        }
      }
      ... on Cat {
        my_uuid__microchip: microchip {
          ... on Microchip {
            petName
          }
        }
      }
      ... on Dog {
        __typename__my_uuid: __typename
      }
      ... on Cat {
        __typename__my_uuid: __typename
      }
    }
  }
}"""
        def serviceResponse = [pets: [
                [__typename__my_uuid: "Cat", my_uuid__microchip: [petName: "Tiger"]],
                [__typename__my_uuid: "Dog", my_uuid__collar: [petName: "Luna"]],
        ]]

        def overallResponse = [pets: [[name: "Tiger"], [name: "Luna"]]]
        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                serviceName,
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "simple type rename"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue => renamed from UnderlyingIssue {
                name: String 
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                issue: UnderlyingIssue 
            } 
            type UnderlyingIssue {
                name:String
            }
        """
        def query = """
        { issue { __typename name } } 
        """
        def expectedQuery = '''query {... on Query {issue {... on UnderlyingIssue {__typename} ... on UnderlyingIssue {name}}}}'''
        def serviceResponse = [issue: [__typename: "UnderlyingIssue", name: "My Issue"]]

        def overallResponse = [issue: [__typename: "Issue", name: "My Issue"]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "type rename with deep rename"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue => renamed from UnderlyingIssue{
                name: String => renamed from detail.detailName
                detail: IssueDetails
            }
            type IssueDetails => renamed from UnderlyingIssueDetails {
                otherDetail: String
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                issue: UnderlyingIssue 
            } 
            type UnderlyingIssue {
                detail: UnderlyingIssueDetails
            }
            type UnderlyingIssueDetails {
                detailName: String
                otherDetail: String
            }
        """
        def query = """
        { issue { name  detail {otherDetail} } }
        """
        def expectedQuery = """query {
  ... on Query {
    issue {
      ... on UnderlyingIssue {
        my_uuid__detail: detail {
          ... on UnderlyingIssueDetails {
            detailName
          }
        }
      }
      ... on UnderlyingIssue {
        __typename__my_uuid: __typename
      }
      ... on UnderlyingIssue {detail {... on UnderlyingIssueDetails {otherDetail}}}
    }
  }
}"""
        def serviceResponse = [issue: [detail: [otherDetail: "other detail"], __typename__my_uuid: "UnderlyingIssue", my_uuid__detail: [detailName: "My Issue"]]]
        def overallResponse = [issue: [name: "My Issue", detail: [otherDetail: "other detail"]]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "input object type rename with query variables"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue(arg: Input): String
            } 
            input Input => renamed from UnderlyingInput{
                foo: String
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                issue(arg: Input): String
            } 
            input Input{
                foo: String
            }
        """
        def query = ''' 
        query($var: Input)
        { issue(arg: $var)}
        '''
        def rawVariables = [var: [foo: "bar"]]
        def expectedQuery = '''query {... on Query {issue(arg: {foo: "bar"})}}'''
        def serviceResponse = [issue: "hello"]

        def overallResponse = [issue: "hello"]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
                rawVariables
        )
        then:
        errors.size() == 0
        response == overallResponse
    }

    def "simple field rename"() {
        def nsdl = [IssueService: """
         service IssueService {
            type Query {
                issue: Issue
            } 
            type Issue {
                name: String => renamed from underlyingName
            }
         }
        """]
        def underlyingSchema = """
            type Query {
                issue: Issue 
            } 
            type Issue {
                underlyingName: String
            }
        """
        def query = """
        { issue { name } } 
        """
        def expectedQuery = """query {
  ... on Query {
    issue {
      ... on Issue {
            my_uuid__underlyingName: underlyingName
      }
      ... on Issue {
        __typename__my_uuid: __typename
      }
    }
  }
}"""
        def overallResponse = [issue: [name: "My Issue"]]
        def serviceResponse = [issue: [__typename__my_uuid: "Issue", my_uuid__underlyingName: "My Issue"]]

        Map response
        List<GraphQLError> errors
        when:
        (response, errors) = test1Service(
                nsdl,
                'IssueService',
                underlyingSchema,
                query,
                expectedQuery,
                serviceResponse,
        )
        then:
        errors.size() == 0
        response == overallResponse
    }


    Object[] test1Service(Map<String, String> overallSchema,
                          String serviceOneName,
                          String underlyingSchema,
                          String query,
                          String expectedQuery,
                          Map serviceResponse,
                          Map rawVariables = [:]
    ) {
        def response1ServiceResult = new ServiceExecutionResult(serviceResponse)

        boolean calledService1 = false
        def astSorter = new AstSorter()
        ServiceExecution serviceExecution = { ServiceExecutionParameters sep ->
            calledService1 = true
            assert printAstCompact(
                    astSorter.sort(sep.query)
            ).tap {
                println "Actual query:"
                println it
            } == printAstCompact(
                    astSorter.sort(
                            new Parser().parseDocument(expectedQuery),
                    )
            ).tap {
                println "Expecting query:"
                println it
            }
            return completedFuture(response1ServiceResult)
        }

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([
                (serviceOneName): new Tuple2(serviceExecution, typeDefinitions(underlyingSchema))]
        )
        Nadel nadel = NextgenEngine.newNadel()
                .dsl(overallSchema)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(rawVariables)
                .artificialFieldsUUID("uuid")
                .build()

        def response = nadel.execute(nadelExecutionInput)

        def executionResult = response.get()

        return [executionResult.getData(), executionResult.getErrors()]
    }

    Object[] testServices(
            Map<String, String> overallSchema, // Map<ServiceName, OverallSchema>
            Map<String, String> services, // Map<ServiceName, UnderlyingSchema>
            Map<String, Map<Document, Map>> serviceCalls, // Map<ServiceName, Map<Query, Result>>
            String query,
            Map rawVariables = [:]
    ) {
        def astSorter = new AstSorter()
        def makeServiceExecution = { String serviceName ->
            println "On calling service '$serviceName'"
            def calls = serviceCalls[serviceName]
            def execution = Mock(ServiceExecution)
            calls.each {
                def expectedDocument = astSorter.sort(it.key)
                println "Expecting query"
                def result = new ServiceExecutionResult(it.value)
                println printAstCompact(expectedDocument)
                1 * execution.execute({ ServiceExecutionParameters sep ->
                    println "Service '$serviceName' got query"
                    println printAstCompact(sep.query)
                    printAstCompact(sep.query) == printAstCompact(expectedDocument)
                }) >> completedFuture(result)
            }
            execution
        }

        ServiceExecutionFactory serviceFactory = TestUtil.serviceFactory([:].tap {
            services.each {
                def serviceName = it.key
                def serviceSchema = it.value
                put(serviceName, new Tuple2(makeServiceExecution(serviceName), typeDefinitions(serviceSchema)))
            }
        })
        Nadel nadel = NextgenEngine.newNadel()
                .dsl(overallSchema)
                .serviceExecutionFactory(serviceFactory)
                .build()
        NadelExecutionInput nadelExecutionInput = newNadelExecutionInput()
                .query(query)
                .variables(rawVariables)
                .artificialFieldsUUID("uuid")
                .build()

        def response = nadel.execute(nadelExecutionInput)

        def executionResult = response.get()

        return [executionResult.getData(), executionResult.getErrors()]
    }


}
