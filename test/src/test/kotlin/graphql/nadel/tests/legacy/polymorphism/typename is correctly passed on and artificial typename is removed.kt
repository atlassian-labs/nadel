package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `typename is correctly passed on and artificial typename is removed` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    __typename
|    id
|    ... on Issue {
|      authorIds
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issues: [AbstractIssue]
    |}
    |interface AbstractIssue {
    |  id: ID
    |}
    |type Issue implements AbstractIssue {
    |  id: ID
    |  authorIds: [ID]
    |}
    |""".trimMargin(), underlyingSchema="""
    |interface AbstractIssue {
    |  id: ID
    |}
    |
    |type Issue implements AbstractIssue {
    |  authorIds: [ID]
    |  id: ID
    |}
    |
    |type Query {
    |  issues: [AbstractIssue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(id = "ISSUE-1", authorIds = listOf("USER-1", "USER-2")),
              Issues_Issue(id = "ISSUE-2", authorIds = listOf("USER-3")))}
      }
      wiring.type("AbstractIssue") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
)) {
  private interface Issues_AbstractIssue {
    public val id: String?
  }

  private data class Issues_Issue(
    public val authorIds: List<String?>? = null,
    override val id: String? = null,
  ) : Issues_AbstractIssue
}
