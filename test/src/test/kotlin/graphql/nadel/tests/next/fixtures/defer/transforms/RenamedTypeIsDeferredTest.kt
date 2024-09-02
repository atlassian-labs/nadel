package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class RenamedTypeIsDeferredTest : NadelIntegrationTest(
    query = """
      query {
        zoo {
          ...@defer {
            monkey {
              name
              __typename
            }
          }
          cat {
            name
            ...@defer {
              __typename
            }
          }          
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "defer",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  zoo: ZooApi 
                }
                type ZooApi {
                  monkey: Monkey 
                  cat: Cat
                }
                type Monkey @renamed(from: "Donkey"){
                  name: String
                }
                type Cat @renamed(from: "Rat"){
                  name: String
                }
                

            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("zoo") { env ->
                                Any()
                            }
                    }
                    .type("ZooApi") { type ->
                        type
                            .dataFetcher("monkey") { env ->
                                Any()
                            }
                            .dataFetcher("cat") { env ->
                                Any()
                            }
                    }
                    .type("Donkey") { type ->
                        type
                            .dataFetcher("name") { env ->
                                "Harambe"
                            }
                    }
                    .type("Rat") { type ->
                        type
                            .dataFetcher("name") { env ->
                                "Garfield"
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}