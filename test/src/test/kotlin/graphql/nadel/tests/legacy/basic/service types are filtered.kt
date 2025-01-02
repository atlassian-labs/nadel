package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class `service types are filtered` : NadelLegacyIntegrationTest(query = """
|query {
|  test {
|    ... on Test {
|      id
|    }
|    ... on QueryError {
|      extensions {
|        ...GenericQueryErrorExtensions
|      }
|    }
|  }
|}
|
|fragment GenericQueryErrorExtensions on QueryErrorExtension {
|  errorType
|  ... on LabQueryErrorExtension {
|    statusCode
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="test", overallSchema="""
    |type Query {
    |  test: TestResult
    |}
    |type Test {
    |  id: ID
    |}
    |union TestResult = Test | QueryError
    """.trimMargin(), underlyingSchema="""
    |type Query {
    |  test: TestResult
    |}
    |type Test {
    |  id: ID
    |}
    |union TestResult = Test | QueryError
    |interface QueryErrorExtension {
    |  ""${'"'}A numerical code (such as a HTTP status code) representing the error category""${'"'}
    |  statusCode: Int
    |  ""${'"'}Application specific error type""${'"'}
    |  errorType: String
    |}
    |type GenericQueryErrorExtension implements QueryErrorExtension {
    |  statusCode: Int
    |  errorType: String
    |}
    |type QueryError {
    |  ""${'"'}The ID of the object that would have otherwise been returned if not for the query error""${'"'}
    |  identifier: ID
    |  "A message describing the error"
    |  message: String
    |  "Use this to put extra data on the error if required"
    |  extensions: [QueryErrorExtension!]
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("test") { env ->
          Test_Test(id = "Test-1")}
      }
      wiring.type("QueryErrorExtension") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }

      wiring.type("TestResult") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
, Service(name="lab", overallSchema="""
    |type LabQueryErrorExtension implements QueryErrorExtension {
    |  statusCode: Int
    |  errorType: String
    |}
    """.trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    |interface QueryErrorExtension {
    |  ""${'"'}A numerical code (such as a HTTP status code) representing the error category""${'"'}
    |  statusCode: Int
    |  ""${'"'}Application specific error type""${'"'}
    |  errorType: String
    |}
    |type LabQueryErrorExtension implements QueryErrorExtension {
    |  statusCode: Int
    |  errorType: String
    |}
    |type QueryError {
    |  ""${'"'}The ID of the object that would have otherwise been returned if not for the query error""${'"'}
    |  identifier: ID
    |  "A message describing the error"
    |  message: String
    |  "Use this to put extra data on the error if required"
    |  extensions: [QueryErrorExtension!]
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("QueryErrorExtension") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
, Service(name="shared", overallSchema="""
    |interface QueryErrorExtension {
    |  ""${'"'}A numerical code (such as a HTTP status code) representing the error category""${'"'}
    |  statusCode: Int
    |  ""${'"'}Application specific error type""${'"'}
    |  errorType: String
    |}
    |type GenericQueryErrorExtension implements QueryErrorExtension {
    |  statusCode: Int
    |  errorType: String
    |}
    |type QueryError {
    |  ""${'"'}The ID of the object that would have otherwise been returned if not for the query error""${'"'}
    |  identifier: ID
    |  "A message describing the error"
    |  message: String
    |  "Use this to put extra data on the error if required"
    |  extensions: [QueryErrorExtension!]
    |}
    """.trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Test_GenericQueryErrorExtension(
    override val statusCode: Int? = null,
    override val errorType: String? = null,
  ) : Test_QueryErrorExtension

  private data class Test_QueryError(
    public val identifier: String? = null,
    public val message: String? = null,
    public val extensions: List<Test_QueryErrorExtension>? = null,
  ) : Test_TestResult

  private interface Test_QueryErrorExtension {
    public val statusCode: Int?

    public val errorType: String?
  }

  private data class Test_Test(
    public val id: String? = null,
  ) : Test_TestResult

  private sealed interface Test_TestResult

  private data class Lab_LabQueryErrorExtension(
    override val statusCode: Int? = null,
    override val errorType: String? = null,
  ) : Lab_QueryErrorExtension

  private data class Lab_QueryError(
    public val identifier: String? = null,
    public val message: String? = null,
    public val extensions: List<Lab_QueryErrorExtension>? = null,
  )

  private interface Lab_QueryErrorExtension {
    public val statusCode: Int?

    public val errorType: String?
  }
}
