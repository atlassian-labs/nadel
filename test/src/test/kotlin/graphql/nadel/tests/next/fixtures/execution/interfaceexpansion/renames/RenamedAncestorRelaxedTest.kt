package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

/**
 * Confirms the field is emitted bare even though a renamed ancestor makes its overall and underlying
 * query paths differ, and the underlying-only `Secret` is stripped to `{}`.
 */

class RenamedAncestorRelaxedBareInterfaceFieldTest : RenamedAncestorTestBase(
    query = """
        query {
          container {
            things {
              id
            }
          }
        }
    """.trimIndent(),
)

class RenamedAncestorRelaxedBareTypenameTest : RenamedAncestorTestBase(
    query = """
        query {
          container {
            things {
              __typename
            }
          }
        }
    """.trimIndent(),
)
