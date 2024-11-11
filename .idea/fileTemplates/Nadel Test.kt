#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}
#end

import graphql.nadel.tests.next.NadelIntegrationTest

#parse("File Header.java")
class ${NAME} : NadelIntegrationTest(
    query = """
        {
          echo
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                    echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("echo") { env ->
                                "echo"
                            }
                    }
            },
        ),
    ),
)