package graphql.nadel.instrumentation.paramaters

import graphql.nadel.engine.transform.NadelRenameTransform
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import io.kotest.core.spec.style.DescribeSpec

class NadelInstrumentationTimingParametersTest : DescribeSpec({
    describe("Step") {
        describe("getFullName") {
            it("generates correct full name") {
                // given
                val step = ChildStep(parent = QueryTransforming, transform = NadelRenameTransform::class)

                // when
                val fullName = step.getFullName()

                // then
                assert(fullName == "QueryTransforming.NadelRenameTransform")
            }

            it("generates correct full name for nested step") {
                // given
                val step = ChildStep(
                    parent = ChildStep(parent = QueryTransforming, transform = NadelRenameTransform::class),
                    name = "Lookup"
                )

                // when
                val fullName = step.getFullName()

                // then
                assert(fullName == "QueryTransforming.NadelRenameTransform.Lookup")
            }
        }

        describe("isChildOf") {
            it("return true if parent exists") {
                val step = ChildStep(
                    parent = ChildStep(parent = QueryTransforming, transform = NadelRenameTransform::class),
                    name = "Lookup"
                )

                // then
                assert(step.isChildOf(QueryTransforming))
                assert(step.isChildOf(step.parent))
                assert(step.isChildOf(step))
            }

            it("return false if parent does not exist") {
                val step = ChildStep(
                    parent = ChildStep(parent = QueryTransforming, transform = NadelRenameTransform::class),
                    name = "Lookup"
                )

                assert(!step.isChildOf(ResultTransforming))
            }

            it("returns true for self") {
                assert(QueryTransforming.isChildOf(QueryTransforming))
                assert(ResultTransforming.isChildOf(ResultTransforming))
                assert(ExecutionPlanning.isChildOf(ExecutionPlanning))
            }
        }
    }
})
