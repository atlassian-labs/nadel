package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory

/**
 * Checks that the hydration source is being set during hydration calls on new engine only
 */
@Suppress("UnusedEquals")
@UseHook
class `hydration-matching-using-index-with-lists` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)
                    return if (serviceName == "UserService") {
                        ServiceExecution { serviceExecutionParameters ->
                            val hydrationDetails = serviceExecutionParameters.hydrationDetails
                            if (hydrationDetails?.hydrationSourceService != null) {
                                assert(hydrationDetails.hydrationVirtualField.fieldName == "authors")
                                assert(hydrationDetails.hydrationVirtualField.typeName == "Issue")
                                assert(hydrationDetails.hydrationSourceService.name == "Issues")
                            }
                            serviceExecution.execute(serviceExecutionParameters)
                        }
                    } else {
                        serviceExecution
                    }
                }
            })
    }
}
