package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

/**
 * The selection goes bare downstream; the underlying-only `Secret` is stripped to `{}`, and the exposed `Issue`
 * node comes back as the overall type name `JiraIssue`.
 */

class RenamedHiddenImplRelaxedBareInterfaceFieldTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class RenamedHiddenImplRelaxedBareTypenameTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
