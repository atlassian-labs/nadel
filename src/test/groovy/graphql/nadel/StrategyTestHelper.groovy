package graphql.nadel

import graphql.ExecutionInput
import graphql.GraphQLError
import graphql.execution.ExecutionId
import graphql.execution.nextgen.ExecutionHelper
import graphql.nadel.dsl.ServiceDefinition
import graphql.nadel.engine.NadelContext
import graphql.nadel.engine.NadelExecutionStrategy
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.result.ResultNodesUtil
import graphql.nadel.result.RootExecutionResultNode
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLSchema
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

import static graphql.language.AstPrinter.printAstCompact
import static graphql.nadel.testutils.TestUtil.createNormalizedQuery
import static graphql.nadel.testutils.TestUtil.parseQuery
import static java.util.concurrent.CompletableFuture.completedFuture

class StrategyTestHelper extends Specification {

    ExecutionHelper executionHelper = new ExecutionHelper()

    Object[] test2Services(GraphQLSchema overallSchema,
                           String serviceOneName,
                           GraphQLSchema underlyingOne,
                           String serviceTwoName,
                           GraphQLSchema underlyingTwo,
                           String query,
                           List<String> topLevelFields,
                           String expectedQuery1,
                           Map response1,
                           String expectedQuery2,
                           Map response2,
                           ServiceExecutionHooks serviceExecutionHooks = new ServiceExecutionHooks() {}
    ) {

        def response1ServiceResult = new ServiceExecutionResult(response1)
        def response2ServiceResult = new ServiceExecutionResult(response2)

        boolean calledService1 = false
        ServiceExecution service1Execution = { ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            assert printAstCompact(sep.query) == expectedQuery1
            calledService1 = true
            return completedFuture(response1ServiceResult)
        }
        boolean calledService2 = false
        ServiceExecution service2Execution = { ServiceExecutionParameters sep ->
            println printAstCompact(sep.query)
            assert printAstCompact(sep.query) == expectedQuery2
            calledService2 = true
            return completedFuture(response2ServiceResult)
        }
        def serviceDefinition = ServiceDefinition.newServiceDefinition().build()
        def definitionRegistry = Mock(DefinitionRegistry)
        def instrumentation = new NadelInstrumentation() {}

        def service1 = new Service(serviceOneName, underlyingOne, service1Execution, serviceDefinition, definitionRegistry)
        def service2 = new Service(serviceTwoName, underlyingTwo, service2Execution, serviceDefinition, definitionRegistry)

        Map fieldInfoByDefinition = [:]
        topLevelFields.forEach({ it ->
            def fd = overallSchema.getQueryType().getFieldDefinition(it)
            FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service1, fd)
            fieldInfoByDefinition.put(fd, fieldInfo)
        })
        FieldInfos fieldInfos = new FieldInfos(fieldInfoByDefinition)

        NadelExecutionStrategy nadelExecutionStrategy = new NadelExecutionStrategy([service1, service2], fieldInfos, overallSchema, instrumentation, serviceExecutionHooks)


        def executionData = createExecutionData(query, overallSchema)

        def response = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection)

        assert calledService1
        assert calledService2

        return [resultData(response), resultErrors(response)]
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, GraphQLSchema overallSchema) {
        createExecutionData(query, [:], overallSchema)
    }

    ExecutionHelper.ExecutionData createExecutionData(String query, Map<String, Object> variables, GraphQLSchema overallSchema) {
        def document = parseQuery(query)
        def normalizedQuery = createNormalizedQuery(overallSchema, document)

        def nadelContext = NadelContext.newContext()
                .forkJoinPool(ForkJoinPool.commonPool())
                .artificialFieldsUUID("UUID")
                .normalizedOverallQuery(normalizedQuery)
                .build()
        def executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .context(nadelContext)
                .build()
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, null)
        executionData
    }

    Object resultData(CompletableFuture<RootExecutionResultNode> response) {
        ResultNodesUtil.toExecutionResult(response.get()).data
    }

    List<GraphQLError> resultErrors(CompletableFuture<RootExecutionResultNode> response) {
        ResultNodesUtil.toExecutionResult(response.get()).errors
    }

    FieldInfos topLevelFieldInfo(GraphQLFieldDefinition fieldDefinition, Service service) {
        FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, fieldDefinition)
        return new FieldInfos([(fieldDefinition): fieldInfo])
    }

}
