package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.multiinterface

/**
 * Confirms the relaxation check accepts a field whose parent resolves to multiple interfaces - the
 * `containers { item { id } }` selection goes bare downstream instead of expanding per exposed impl.
 */
class MultiInterfaceHiddenImplRelaxedBareInterfaceFieldTest : MultiInterfaceHiddenImplTestBase(
    query = """
        query {
          containers {
            item {
              id
            }
          }
        }
    """.trimIndent(),
)
