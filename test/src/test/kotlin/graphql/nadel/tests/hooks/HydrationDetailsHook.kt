@file:Suppress("ClassName")

package graphql.nadel.tests.hooks

import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

abstract class HydrationDetailsHook : EngineTestHook {
    override fun wrapServiceExecution(baseTestServiceExecution: ServiceExecution): ServiceExecution {
        return ServiceExecution { params ->
            if (params.hydrationDetails != null) {
                assertHydrationDetails(params.hydrationDetails!!)
            }
            baseTestServiceExecution.execute(params)
        }
    }

    abstract fun assertHydrationDetails(actualHydrationDetails: ServiceExecutionHydrationDetails)
}

@UseHook
class `basic-hydration` : HydrationDetailsHook() {
    override fun assertHydrationDetails(actualHydrationDetails: ServiceExecutionHydrationDetails) {
        assert(actualHydrationDetails.hydrationEffectField.toString() == "Query.barById")
        assert(actualHydrationDetails.hydrationCauseField.toString() == "Foo.bar")
        assert(actualHydrationDetails.hydrationCauseService.name == "service1")
    }
}

@UseHook
class `batch-hydration-with-renamed-actor-field` : HydrationDetailsHook() {
    override fun assertHydrationDetails(actualHydrationDetails: ServiceExecutionHydrationDetails) {
        assert(actualHydrationDetails.hydrationEffectField.toString() == "Query.barsByIdOverall")
        assert(actualHydrationDetails.hydrationCauseField.toString() == "Foo.bar")
        assert(actualHydrationDetails.hydrationCauseService.name == "service1")
    }
}
