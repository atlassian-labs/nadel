package graphql.nadel.engine;

import graphql.Assert;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.FragmentDefinition;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecution;
import graphql.nadel.ServiceExecutionParameters;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationServiceExecutionParameters;
import graphql.nadel.util.LogKit;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static graphql.nadel.ServiceExecutionParameters.newServiceExecutionParameters;
import static graphql.nadel.engine.StrategyUtil.createRootExecutionStepInfo;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class ServiceExecutor {

    private final Logger log = LoggerFactory.getLogger(ServiceExecutor.class);
    private final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(ServiceExecutor.class);

    private final ServiceResultToResultNodes resultToResultNode = new ServiceResultToResultNodes();

    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;

    public ServiceExecutor(GraphQLSchema overallSchema, NadelInstrumentation instrumentation) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
    }

    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext executionContext,
                                                              QueryTransformationResult queryTransformerResult,
                                                              Service service,
                                                              Operation operation,
                                                              Object serviceContext) {

        List<MergedField> transformedMergedFields = queryTransformerResult.getTransformedMergedFields();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformerResult, serviceContext);
        ExecutionContext executionContextForService = buildServiceExecutionContext(executionContext, underlyingSchema, serviceExecutionParameters);

        ExecutionStepInfo underlyingRootStepInfo = createRootExecutionStepInfo(service.getUnderlyingSchema(), operation);

        CompletableFuture<ServiceExecutionResult> result = executeImpl(service, serviceExecution, serviceExecutionParameters, underlyingRootStepInfo, executionContext);
        return result
                .thenApply(executionResult -> serviceExecutionResultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult));
    }


    private CompletableFuture<ServiceExecutionResult> executeImpl(Service service, ServiceExecution serviceExecution, ServiceExecutionParameters serviceExecutionParameters, ExecutionStepInfo executionStepInfo, ExecutionContext executionContext) {

        NadelInstrumentationServiceExecutionParameters instrumentationParams = new NadelInstrumentationServiceExecutionParameters(service, executionContext, executionContext.getInstrumentationState());
        serviceExecution = instrumentation.instrumentServiceExecution(serviceExecution, instrumentationParams);

        try {
            log.debug("service {} invocation started", service.getName());
            CompletableFuture<ServiceExecutionResult> result = serviceExecution.execute(serviceExecutionParameters);
            Assert.assertNotNull(result, "service execution returned null");
            log.debug("service {} invocation finished ", service.getName());
            //
            // if they return an exceptional CF then we turn that into graphql errors as well
            return result.handle(handleServiceException(service, executionStepInfo));
        } catch (Exception e) {
            return completedFuture(mkExceptionResult(service, executionStepInfo, e));
        }
    }

    private BiFunction<ServiceExecutionResult, Throwable, ServiceExecutionResult> handleServiceException(Service service, ExecutionStepInfo executionStepInfo) {
        return (serviceCallResult, throwable) -> {
            if (throwable != null) {
                return mkExceptionResult(service, executionStepInfo, throwable);
            } else {
                return serviceCallResult;
            }
        };
    }

    private ServiceExecutionResult mkExceptionResult(Service service, ExecutionStepInfo executionStepInfo, Throwable e) {
        String errorText = format("An exception occurred invoking the service '%s' : '%s'", service.getName(), e.getMessage());
        logNotSafe.error(errorText, e);

        GraphqlErrorBuilder errorBuilder = GraphqlErrorBuilder.newError();
        MergedField field = executionStepInfo.getField();
        if (field != null) {
            errorBuilder.location(field.getSingleField().getSourceLocation());
        }
        GraphQLError error = errorBuilder
                .message(errorText)
                .path(executionStepInfo.getPath())
                .errorType(ErrorType.DataFetchingException)
                .build();

        Map<String, Object> errorMap = error.toSpecification();
        return new ServiceExecutionResult(new LinkedHashMap<>(), singletonList(errorMap));
    }


    private ServiceExecutionParameters buildServiceExecutionParameters(ExecutionContext executionContext, QueryTransformationResult queryTransformerResult, Object serviceContext) {

        // only pass down variables that are referenced in the transformed query
        Map<String, Object> contextVariables = executionContext.getVariables();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String referencedVariable : queryTransformerResult.getReferencedVariables()) {
            Object value = contextVariables.get(referencedVariable);
            variables.put(referencedVariable, value);
        }

        // only pass down fragments that have been used by the query after it is transformed
        Map<String, FragmentDefinition> fragments = queryTransformerResult.getTransformedFragments();

        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        Object callerSuppliedContext = nadelContext.getUserSuppliedContext();

        return newServiceExecutionParameters()
                .query(queryTransformerResult.getDocument())
                .context(callerSuppliedContext)
                .variables(variables)
                .fragments(fragments)
                .operationDefinition(queryTransformerResult.getOperationDefinition())
                .executionId(executionContext.getExecutionId())
                .cacheControl(executionContext.getCacheControl())
                .serviceContext(serviceContext)
                .build();
    }

    private ExecutionContext buildServiceExecutionContext(ExecutionContext executionContext, GraphQLSchema underlyingSchema, ServiceExecutionParameters serviceExecutionParameters) {
        return executionContext.transform(builder -> builder
                .graphQLSchema(underlyingSchema)
                .fragmentsByName(serviceExecutionParameters.getFragments())
                .variables(serviceExecutionParameters.getVariables())
                .operationDefinition(serviceExecutionParameters.getOperationDefinition())
        );
    }

    private RootExecutionResultNode serviceExecutionResultToResultNode(ExecutionContext executionContextForService, ExecutionStepInfo underlyingRootStepInfo, List<MergedField> transformedMergedFields, ServiceExecutionResult executionResult) {
        return resultToResultNode.resultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult);
    }

}
