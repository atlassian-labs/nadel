package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `operation depth limit` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo { # 1
            child { # 2
              child { # 3
                child { # 4
                  child { # 5
                    child { # 6
                      child { # 7
                        child { # 8
                          child { # 9
                            child { # 10
                              name # 11
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  name: String
                  child: Foo
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                }
                type Foo {
                  name: String
                  child: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Service_Foo(
        val name: String? = null,
        val child: Service_Foo? = null,
    )
}
