package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `operation depth limit` : NadelLegacyIntegrationTest(query = """
|query {
|  foo { # 1
|    child { # 2
|      child { # 3
|        child { # 4
|          child { # 5
|            child { # 6
|              child { # 7
|                child { # 8
|                  child { # 9
|                    child { # 10
|                      name # 11
|                    }
|                  }
|                }
|              }
|            }
|          }
|        }
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  name: String
    |  child: Foo
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  name: String
    |  child: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Service_Foo(
    public val name: String? = null,
    public val child: Service_Foo? = null,
  )
}
