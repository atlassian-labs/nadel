package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `complex identified by doesnt use all input fields in the inputIdentifiedBy argument` :
    NadelLegacyIntegrationTest(query = """
|query {
|  activities {
|    id
|    issue {
|      issueId
|      description
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Activity",
    overallSchema="""
    |type Query {
    |  activities: [Activity]
    |}
    |
    |type Activity {
    |  id: ID
    |  issue: Issue @hydrated(
    |    service: "Issue"
    |    field: "issues"
    |    arguments: [{name: "issuesInput" value: "${'$'}source.context.issueHydrationInput"}]
    |    inputIdentifiedBy: [
    |      {sourceId: "context.issueHydrationInput.id" resultId: "issueId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activities: [Activity]
    |}
    |
    |type Activity {
    |  id: ID
    |  context: HydrationContext
    |}
    |
    |type HydrationContext {
    |  issueHydrationInput: IssueHydrationInput
    |}
    |
    |type IssueHydrationInput {
    |  id: ID!
    |  site: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activities") { env ->
          listOf(Activity_Activity(context = Activity_HydrationContext(issueHydrationInput =
              Activity_IssueHydrationInput(id = "ISSUE-0", site = "CLOUD-0")), id = "ACTIVITY-0"),
              Activity_Activity(context = Activity_HydrationContext(issueHydrationInput =
              Activity_IssueHydrationInput(id = "ISSUE-1", site = "CLOUD-0")), id = "ACTIVITY-1"),
              Activity_Activity(context = Activity_HydrationContext(issueHydrationInput =
              Activity_IssueHydrationInput(id = "ISSUE-2", site = "CLOUD-0")), id = "ACTIVITY-2"),
              Activity_Activity(context = Activity_HydrationContext(issueHydrationInput =
              Activity_IssueHydrationInput(id = "ISSUE-3", site = "CLOUD-0")), id = "ACTIVITY-3"))}
      }
    }
    )
, Service(name="Issue", overallSchema="""
    |type Query {
    |  issues(issuesInput: [IssueInput!]): [Issue]
    |}
    |
    |type Issue {
    |  issueId: ID
    |  description: String
    |}
    |
    |input IssueInput {
    |  id: ID!
    |  site: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issues(issuesInput: [IssueInput!]): [Issue]
    |}
    |
    |type Issue {
    |  issueId: ID
    |  description: String
    |}
    |
    |input IssueInput {
    |  id: ID!
    |  site: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          if (env.getArgument<Any?>("issuesInput") == listOf(mapOf("id" to "ISSUE-0", "site" to
              "CLOUD-0"), mapOf("id" to "ISSUE-1", "site" to "CLOUD-0"), mapOf("id" to "ISSUE-2",
              "site" to "CLOUD-0"), mapOf("id" to "ISSUE-3", "site" to "CLOUD-0"))) {
            listOf(Issue_Issue(description = "fix A", issueId = "ISSUE-0"), Issue_Issue(description
                = "fix B", issueId = "ISSUE-1"), Issue_Issue(description = "fix C", issueId =
                "ISSUE-2"), Issue_Issue(description = "fix D", issueId = "ISSUE-3"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Activity_Activity(
    public val id: String? = null,
    public val context: Activity_HydrationContext? = null,
  )

  private data class Activity_HydrationContext(
    public val issueHydrationInput: Activity_IssueHydrationInput? = null,
  )

  private data class Activity_IssueHydrationInput(
    public val id: String? = null,
    public val site: String? = null,
  )

  private data class Issue_Issue(
    public val issueId: String? = null,
    public val description: String? = null,
  )

  private data class Issue_IssueInput(
    public val id: String? = null,
    public val site: String? = null,
  )
}
