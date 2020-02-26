package graphql.nadel.instrumentation;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.tracing.TracingSupport;
import graphql.language.Document;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters;
import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.validation.ValidationError;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.whenCompleted;

public class TracingInstrumentation implements NadelInstrumentation {

    @Override
    public InstrumentationState createState(NadelInstrumentationCreateStateParameters parameters) {
        return new TracingSupport(false);
    }

    @Override
    public InstrumentationContext<Document> beginParse(NadelInstrumentationQueryExecutionParameters parameters) {
        TracingSupport tracingSupport = parameters.getInstrumentationState();
        TracingSupport.TracingContext ctx = tracingSupport.beginParse();
        return whenCompleted((result, t) -> ctx.onEnd());
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(NadelNadelInstrumentationQueryValidationParameters parameters) {
        TracingSupport tracingSupport = parameters.getInstrumentationState();
        TracingSupport.TracingContext ctx = tracingSupport.beginValidation();
        return whenCompleted((result, t) -> ctx.onEnd());
    }

    @Override
    public InstrumentationContext<ExecutionResultNode> beginFieldFetch(NadelInstrumentationFetchFieldParameters parameters) {
        TracingSupport tracingSupport = parameters.getInstrumentationState();
        ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();
        DataFetchingEnvironment environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(parameters.getExecutionContext()).executionStepInfo(executionStepInfo).build();
        //
        // in re-using the graphql Tracing support, we rely on that code only looking at the execution step inside the DFE
        // in this case we now wish it just took the execution step info but here we are
        //
        TracingSupport.TracingContext ctx = tracingSupport.beginField(environment, false);
        return whenCompleted((result, t) -> ctx.onEnd());
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, NadelInstrumentationQueryExecutionParameters parameters) {
        TracingSupport tracingSupport = parameters.getInstrumentationState();
        ExecutionResult newResult = ExecutionResultImpl.newExecutionResult().from(executionResult)
                .addExtension("tracing", tracingSupport.snapshotTracingData())
                .build();
        return CompletableFuture.completedFuture(newResult);
    }

}
