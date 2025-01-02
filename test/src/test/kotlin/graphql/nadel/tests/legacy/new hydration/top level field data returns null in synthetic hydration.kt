package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit

public class `top level field data returns null in synthetic hydration` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issue {
|    project {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service2",
    overallSchema="""
    |type Query {
    |  projects: ProjectsQuery
    |}
    |type ProjectsQuery {
    |  project(id: ID): Project
    |}
    |type Project {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Project {
    |  id: ID
    |  name: String
    |}
    |
    |type ProjectsQuery {
    |  project(id: ID): Project
    |}
    |
    |type Query {
    |  projects: ProjectsQuery
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("projects") {
          Unit}
      }
      wiring.type("ProjectsQuery") { type ->
        type.dataFetcher("project") { env ->
          if (env.getArgument<Any?>("id") == "project1") {
            null}
          else {
            null}
        }
      }
    }
    )
, Service(name="service1", overallSchema="""
    |type Query {
    |  issue(id: ID): Issue
    |}
    |type Issue {
    |  id: ID
    |  project: Project
    |  @hydrated(
    |    service: "service2"
    |    field: "projects.project"
    |    arguments: [{name: "id" value: "${'$'}source.projectId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |  projectId: ID
    |}
    |
    |type Query {
    |  issue(id: ID): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          Service1_Issue(projectId = "project1")}
      }
    }
    )
)) {
  private data class Service2_Project(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service2_ProjectsQuery(
    public val project: Service2_Project? = null,
  )

  private data class Service1_Issue(
    public val id: String? = null,
    public val projectId: String? = null,
  )
}
