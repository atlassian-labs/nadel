package graphql.nadel.engine.tracking;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.engine.HydrationInputNode;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.nadel.engine.UnderscoreTypeNameUtils.isAliasedUnderscoreTypeNameField;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class FieldTracking {
    private final NadelInstrumentation instrumentation;
    private final ExecutionContext executionContext;
    private final InstrumentationState instrumentationState;

    private final Map<ExecutionPath, ExecutionStepInfo> dispatchedFields = new HashMap<>();
    private final Map<ExecutionPath, ExecutionStepInfo> completedFields = new HashMap<>();
    private final Map<ExecutionPath, InstrumentationContext<ExecutionResultNode>> pendingFieldFetchContexts = new HashMap<>();
    private final NadelContext nadelContext;

    public FieldTracking(NadelInstrumentation instrumentation, ExecutionContext executionContext) {
        this.instrumentation = instrumentation;
        this.executionContext = executionContext;
        this.instrumentationState = executionContext.getInstrumentationState();
        this.nadelContext = (NadelContext) executionContext.getContext();
    }


    public void fieldsDispatched(List<ExecutionStepInfo> stepInfos) {
        for (ExecutionStepInfo stepInfo : stepInfos) {
            dispatchIfNeeded(stepInfo);
        }
    }

    private void dispatchIfNeeded(ExecutionStepInfo stepInfo) {
        if (skipField(stepInfo)) {
            // don't do things twice
            return;
        }
        ExecutionPath path = collapsedPath(stepInfo.getPath());
        if (!dispatchedFields.containsKey(path)) {
            ExecutionStepInfo newStepInfo = stepInfo.transform(builder -> builder.path(path));
            NadelInstrumentationFetchFieldParameters parameters = new NadelInstrumentationFetchFieldParameters(executionContext, newStepInfo, instrumentationState);
            InstrumentationContext<ExecutionResultNode> ctx = instrumentation.beginFieldFetch(parameters);
            pendingFieldFetchContexts.put(path, ctx);
            dispatchedFields.put(path, stepInfo);
            //noinspection unchecked
            ctx.onDispatched(null);
        }
    }


    @SuppressWarnings("RedundantIfStatement")
    private boolean skipField(ExecutionStepInfo stepInfo) {
        ExecutionPath path = collapsedPath(stepInfo.getPath());
        if (isAliasedUnderscoreTypeNameField(nadelContext, stepInfo.getField())) {
            return true;
        }
        if (completedFields.containsKey(path)) {
            return true;
        }
        return false;
    }

    public void fieldsCompleted(ExecutionResultNode resultNode, Throwable throwable) {
        if (resultNode instanceof RootExecutionResultNode) {
            completeNodes(resultNode.getChildren(), throwable);
        } else {
            completeNodes(singletonList(resultNode), throwable);
        }
    }

    private void completeNodes(List<ExecutionResultNode> resultNodes, Throwable throwable) {
        for (ExecutionResultNode resultNode : resultNodes) {
            FetchedValueAnalysis fva = resultNode.getFetchedValueAnalysis();
            ExecutionStepInfo stepInfo = fva.getExecutionStepInfo();
            ExecutionPath path = collapsedPath(stepInfo.getPath());

            // we are re-entrant and quite stateful and hence we don't want to do things twice
            boolean skipField = skipField(stepInfo);

            dispatchIfNeeded(stepInfo);

            //
            // hydrated fields are the exception - they have started to execute but they still need to be completed
            // we have another call back path for them
            if (resultNode instanceof HydrationInputNode) {
                continue;
            }

            if (!skipField) {
                if (pendingFieldFetchContexts.containsKey(path)) {
                    InstrumentationContext<ExecutionResultNode> ctx = pendingFieldFetchContexts.get(path);
                    ctx.onCompleted(resultNode, throwable);

                    pendingFieldFetchContexts.remove(path);
                    completedFields.put(path, stepInfo);
                }
            }
            // and go down and complete the children
            List<ExecutionResultNode> children = resultNode.getChildren();
            completeNodes(children, throwable);
        }
    }

    private ExecutionPath collapsedPath(ExecutionPath path) {
        List<Object> segments = path.toList();
        if (segments.isEmpty()) {
            return path;
        }
        List<Object> namesOnly = segments.stream().filter(seg -> seg instanceof String).collect(toList());
        return ExecutionPath.fromList(namesOnly);
    }
}
