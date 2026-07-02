package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.allvisible

import graphql.nadel.NadelExecutionHints

/**
 * The allvisible control with the hint ON: nothing is hidden, so relaxation never fires and behaviour matches
 * hint-OFF.
 */
class AllVisibleRelaxedBareInterfaceFieldTest : AllImplementationsVisibleTestBase(
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

class AllVisibleRelaxedBareTypenameTest : AllImplementationsVisibleTestBase(
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
