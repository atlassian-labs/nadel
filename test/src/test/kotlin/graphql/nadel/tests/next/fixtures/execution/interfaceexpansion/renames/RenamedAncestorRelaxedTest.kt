package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

import graphql.nadel.NadelExecutionHints

/**
 * The renamed-ancestor scenario with the hint ON: same schema + queries as [RenamedAncestorTestBase], hint
 * flipped. Confirms the field is emitted bare even though a renamed ancestor makes its overall and underlying
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
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}

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
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}
