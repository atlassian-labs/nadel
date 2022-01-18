package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory
import graphql.schema.idl.TypeDefinitionRegistry

/**
 * Checks that the hydration counts are being set
 */
@Suppress("UnusedEquals")
@UseHook
class `batching-of-hydration-list` : EngineTestHook {
    var callCount = 0

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)
                    return if (serviceName == "UserService") {
                        ServiceExecution { serviceExecutionParameters ->
                            val hydrationDetails = serviceExecutionParameters.hydrationDetails
                            if (hydrationDetails?.hydrationSourceService != null) {
                                callCount++
                                // we get called twice - with a batch size of 3
                                assert(hydrationDetails.batchSize == 3)
                                assert(hydrationDetails.totalObjectsToBeHydrated == 6)
                                assert(hydrationDetails.countOfObjectsToBeHydrated == 3)
                            }
                            serviceExecution.execute(serviceExecutionParameters)
                        }
                    } else {
                        serviceExecution
                    }
                }

                override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                    return serviceExecutionFactory.getUnderlyingTypeDefinitions(serviceName)
                }
            })
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        if (engineType == NadelEngineType.nextgen) {
            assert(callCount == 2,{ "The $engineType callCount is $callCount"})
        }
    }
}
