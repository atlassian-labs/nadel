package graphql.nadel.tests.legacy.`chained transforms`.`ari use case`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `ari argument in renamed input` : NadelLegacyIntegrationTest(
    query = """
        mutation mainJiraSoftwareStartSprintModalSubmitMutation(
          ${'$'}boardId: ID!
          ${'$'}sprintId: ID!
          ${'$'}name: String!
          ${'$'}goal: String
          ${'$'}startDate: String!
          ${'$'}endDate: String!
        ) {
          startSprint(
            input: {
              boardId: ${'$'}boardId
              sprintId: ${'$'}sprintId
              name: ${'$'}name
              goal: ${'$'}goal
              startDate: ${'$'}startDate
              endDate: ${'$'}endDate
            }
          ) {
            __typename
          }
        }
    """.trimIndent(),
    variables = mapOf(
        "boardId" to "ari:cloud:jira-software::board/123",
        "sprintId" to "ari:cloud:jira-software::sprint/456",
        "name" to "Test Input",
        "goal" to null,
        "startDate" to "2022-03-22",
        "endDate" to "2022-04-02",
    ),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  echo: String
                }
                type Mutation {
                  startSprint(input: StartSprintInput): Sprint
                }
                input StartSprintInput @renamed(from: "SprintInput") {
                  boardId: ID! @ARI(type: "board", owner: "jira-software", interpreted: true)
                  sprintId: ID! @ARI(type: "sprint", owner: "jira-software", interpreted: true)
                  name: String!
                  goal: String
                  startDate: String!
                  endDate: String!
                }
                type Sprint {
                  id: ID!
                }
                directive @ARI(
                  type: String!
                  owner: String!
                  interpreted: Boolean! = false
                ) on ARGUMENT_DEFINITION | FIELD_DEFINITION | INPUT_FIELD_DEFINITION
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                type Mutation {
                  startSprint(input: SprintInput): Sprint
                }
                type Sprint {
                  id: ID!
                }
                input SprintInput {
                  boardId: ID!
                  sprintId: ID!
                  name: String!
                  goal: String
                  startDate: String!
                  endDate: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Mutation") { type ->
                    type.dataFetcher("startSprint") { env ->
                        if (env.getArgument<Any?>("input") ==
                            mapOf(
                                "boardId" to "123",
                                "sprintId" to "456",
                                "name" to "Test Input",
                                "goal" to null,
                                "startDate" to "2022-03-22",
                                "endDate" to "2022-04-02",
                            )
                        ) {
                            MyService_Sprint()
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class MyService_Sprint(
        val id: String? = null,
    )

    private data class MyService_SprintInput(
        val boardId: String? = null,
        val sprintId: String? = null,
        val name: String? = null,
        val goal: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
    )
}
