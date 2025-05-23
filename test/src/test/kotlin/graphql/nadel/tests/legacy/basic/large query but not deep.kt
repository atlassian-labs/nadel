package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `large query but not deep` : NadelLegacyIntegrationTest(
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
                            name # 10
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          bar: foo { # 1
            child { # 2
              child { # 3
                child { # 4
                  child { # 5
                    child { # 6
                      child { # 7
                        child { # 8
                          child { # 9
                            name # 10
                          }
                        }
                      }
                    }
                  }
                }
              }
              two: child { # 3
                child { # 4
                  child { # 5
                    child { # 6
                      child { # 7
                        child { # 8
                          child { # 9
                            name # 10
                          }
                        }
                      }
                    }
                  }
                }
              }
              three: child { # 3
                child { # 4
                  child { # 5
                    child { # 6
                      child { # 7
                        child { # 8
                          child { # 9
                            name # 10
                          }
                        }
                      }
                      four: child { # 7
                        child { # 8
                          child { # 9
                            name # 10
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
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.field.resultKey == "foo") {
                            null
                        } else if (env.field.resultKey == "bar") {
                            null
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Service_Foo(
        val name: String? = null,
        val child: Service_Foo? = null,
    )
}
