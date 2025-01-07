package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `introspection can be performed even with fragments` : NadelLegacyIntegrationTest(
    query = """
        query {
          ...IntrospectionFrag
          __type(name: "World") {
            name
          }
          __typename
        }
        fragment IntrospectionFrag on Query {
          __schema {
            queryType {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello: World
                }
                type World {
                  id: ID
                  name: String
                }
                type Mutation {
                  hello: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Mutation {
                  hello: String
                }
                type Query {
                  hello: World
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class MyService_World(
        val id: String? = null,
        val name: String? = null,
    )
}
