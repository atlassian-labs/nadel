package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `repeated hydrated fields on the same level` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    issue {
|      name
|    }
|    issue {
|      desc
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Foo
    |  issue(issueId: ID): Issue
    |}
    |type Foo {
    |  issue: Issue
    |  @hydrated(
    |    service: "Foo"
    |    field: "issue"
    |    arguments: [{name: "issueId" value: "${'$'}source.issueId"}]
    |  )
    |}
    |type Issue {
    |  id: ID
    |  name: String
    |  desc: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  issueId: ID
    |}
    |
    |type Issue {
    |  desc: String
    |  id: ID
    |  name: String
    |}
    |
    |type Query {
    |  foo: Foo
    |  issue(issueId: ID): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Foo_Foo(issueId = "ISSUE-1")}

        .dataFetcher("issue") { env ->
          if (env.getArgument<Any?>("issueId") == "ISSUE-1") {
            Foo_Issue(desc = "I AM A DESC", name = "I AM A NAME")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Foo_Foo(
    public val issueId: String? = null,
  )

  private data class Foo_Issue(
    public val desc: String? = null,
    public val id: String? = null,
    public val name: String? = null,
  )
}
