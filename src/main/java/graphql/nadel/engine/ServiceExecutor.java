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
    private final ServiceResultToResultNodesMutable resultToResultNodesMutable = new ServiceResultToResultNodesMutable();

    private final NadelInstrumentation instrumentation;

    public ServiceExecutor(NadelInstrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public CompletableFuture<RootExecutionResultNode> execute(ExecutionContext executionContext,
                                                              QueryTransformationResult queryTransformerResult,
                                                              Service service,
                                                              Operation operation,
                                                              Object serviceContext,
                                                              boolean isHydrationCall) {

        List<MergedField> transformedMergedFields = queryTransformerResult.getTransformedMergedFields();

        ServiceExecution serviceExecution = service.getServiceExecution();
        GraphQLSchema underlyingSchema = service.getUnderlyingSchema();

        ServiceExecutionParameters serviceExecutionParameters = buildServiceExecutionParameters(executionContext, queryTransformerResult, serviceContext, isHydrationCall);
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
            log.debug("service {} invocation started - executionId '{}'", service.getName(), executionContext.getExecutionId());
            CompletableFuture<ServiceExecutionResult> result = serviceExecution.execute(serviceExecutionParameters);
            Assert.assertNotNull(result, "service execution returned null");
            log.debug("service {} invocation finished  - executionId '{}' ", service.getName(), executionContext.getExecutionId());
            //
            // if they return an exceptional CF then we turn that into graphql errors as well
            return result.handle(handleServiceException(service, executionContext, executionStepInfo));
        } catch (Exception e) {
            return completedFuture(mkExceptionResult(service, executionContext, executionStepInfo, e));
        }
    }

    private BiFunction<ServiceExecutionResult, Throwable, ServiceExecutionResult> handleServiceException(Service service, ExecutionContext executionContext, ExecutionStepInfo executionStepInfo) {
        return (serviceCallResult, throwable) -> {
            if (throwable != null) {
                return mkExceptionResult(service, executionContext, executionStepInfo, throwable);
            } else {
                return serviceCallResult;
            }
        };
    }

    private ServiceExecutionResult mkExceptionResult(Service service, ExecutionContext executionContext, ExecutionStepInfo executionStepInfo, Throwable throwable) {
        String errorText = format("An exception occurred invoking the service '%s' : '%s' - executionId '%s'", service.getName(), throwable.getMessage(), executionContext.getExecutionId());
        logNotSafe.error(errorText, throwable);

        GraphqlErrorBuilder errorBuilder = GraphqlErrorBuilder.newError();
        MergedField field = executionStepInfo.getField();
        if (field != null) {
            errorBuilder.location(field.getSingleField().getSourceLocation());
        }

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put(java.lang.Throwable.class.getName(), throwable);

        GraphQLError error = errorBuilder
                .message(errorText)
                .path(executionStepInfo.getPath())
                .errorType(ErrorType.DataFetchingException)
                .extensions(extensions)
                .build();

        Map<String, Object> errorMap = error.toSpecification();
        return new ServiceExecutionResult(new LinkedHashMap<>(), singletonList(errorMap));
    }


    private ServiceExecutionParameters buildServiceExecutionParameters(ExecutionContext executionContext, QueryTransformationResult queryTransformerResult, Object serviceContext, boolean isHydrationCall) {

        // only pass down variables that are referenced in the transformed query
        Map<String, Object> variables = buildReferencedVariables(executionContext, queryTransformerResult);

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
                .hydrationCall(isHydrationCall)
                .build();
    }

    Map<String, Object> buildReferencedVariables(ExecutionContext executionContext, QueryTransformationResult queryTransformerResult) {
        Map<String, Object> contextVariables = executionContext.getVariables();
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String referencedVariable : queryTransformerResult.getReferencedVariables()) {
            Object value = contextVariables.get(referencedVariable);
            variables.put(referencedVariable, value);
        }
        return variables;
    }

    private ExecutionContext buildServiceExecutionContext(ExecutionContext executionContext, GraphQLSchema underlyingSchema, ServiceExecutionParameters serviceExecutionParameters) {
        return executionContext.transform(builder -> builder
                .graphQLSchema(underlyingSchema)
                .fragmentsByName(serviceExecutionParameters.getFragments())
                .variables(serviceExecutionParameters.getVariables())
                .operationDefinition(serviceExecutionParameters.getOperationDefinition())
        );
    }

    public static class DebugContext {

        public ExecutionContext executionContextForService;
        public ExecutionStepInfo underlyingRootStepInfo;
        public List<MergedField> transformedMergedFields;
        public ServiceExecutionResult serviceExecutionResult;

    }

    private RootExecutionResultNode serviceExecutionResultToResultNode(ExecutionContext executionContextForService, ExecutionStepInfo underlyingRootStepInfo, List<MergedField> transformedMergedFields, ServiceExecutionResult executionResult) {
        NadelContext nadelContext = executionContextForService.getContext();
        DebugContext debugContext = (DebugContext) nadelContext.getUserSuppliedContext();
        debugContext.executionContextForService = executionContextForService;
        debugContext.underlyingRootStepInfo = underlyingRootStepInfo;
        debugContext.transformedMergedFields = transformedMergedFields;
        debugContext.serviceExecutionResult = executionResult;

        long t = System.currentTimeMillis();
        RootExecutionResultNode rootExecutionResultNode = resultToResultNode.resultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult);
//        System.out.println("time: " + (System.currentTimeMillis() - t));
        t = System.currentTimeMillis();
//        graphql.nadel.execution.RootExecutionResultNode rootExecutionResultNode2 = resultToResultNodesMutable.resultToResultNode(executionContextForService, underlyingRootStepInfo, transformedMergedFields, executionResult);
//        if (rootExecutionResultNode2 != null) {
//            System.out.println("time2: " + (System.currentTimeMillis() - t));
//        }
        return rootExecutionResultNode;
    }

}
