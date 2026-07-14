package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.multiinterface

import graphql.nadel.NadelExecutionHints

/**
 * The multi-interface scenario with the hint ON: same schema + query as [MultiInterfaceHiddenImplTestBase], hint
 * flipped. Confirms the relaxation check accepts a field whose parent resolves to multiple interfaces - the
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
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints().noInterfaceToObjectFragmentExpansion { _ -> true }
    }
}
