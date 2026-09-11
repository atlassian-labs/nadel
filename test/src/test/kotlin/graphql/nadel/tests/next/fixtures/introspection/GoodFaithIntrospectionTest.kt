package graphql.nadel.tests.next.fixtures.introspection

import graphql.introspection.GoodFaithIntrospection
import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.next.fixtures.validation.QueryValidationTestBase
import graphql.validation.QueryComplexityLimits

class GoodFaithIntrospectionRejectedTest : GoodFaithIntrospectionTestBase()

class GoodFaithIntrospectionDisabledTest : GoodFaithIntrospectionTestBase(
    disabled = true,
)

class GoodFaithIntrospectionDisabledWithQueryLimitsTest : GoodFaithIntrospectionTestBase(
    disabled = true,
    limits = QueryComplexityLimits.newLimits().maxFieldsCount(1).build(),
)

abstract class GoodFaithIntrospectionTestBase(
    private val disabled: Boolean = false,
    limits: QueryComplexityLimits? = null,
) : QueryValidationTestBase(
    query = """
        {
          first: __type(name: "Echo") {
            name
          }
          second: __type(name: "Echo") {
            name
          }
        }
    """.trimIndent(),
    limits = limits,
) {
    override fun makeExecutionInput(): NadelExecutionInput.Builder {
        val input = super.makeExecutionInput()
        if (disabled) {
            input.graphqlContext(GoodFaithIntrospection.GOOD_FAITH_INTROSPECTION_DISABLED, true)
        }
        return input
    }
}
