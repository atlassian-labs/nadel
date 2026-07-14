package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

import graphql.nadel.NadelExecutionHints

/**
 * The renamed-impl scenario with the hint ON: same schema + queries as [RenamedHiddenImplTestBase], hint flipped.
 * The selection goes bare downstream; the underlying-only `Secret` is stripped to `{}`, and the exposed
 * `Issue` node comes back as the overall type name `JiraIssue`.
 */

class RenamedHiddenImplRelaxedBareInterfaceFieldTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}

class RenamedHiddenImplRelaxedBareTypenameTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}
