package graphql.nadel.engine.tracking;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.nadel.engine.HydrationInputNode;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
import graphql.nadel.util.ExecutionPathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.nadel.engine.ArtificialFieldUtils.isArtificialField;

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
//        for (ExecutionStepInfo stepInfo : stepInfos) {
//            dispatchIfNeeded(stepInfo);
//        }
    }

    private synchronized void dispatchIfNeeded(ExecutionStepInfo stepInfo) {
        if (skipField(stepInfo)) {
            // don't do things twice
            return;
        }
        ExecutionPath path = stepInfo.getPath();
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
        ExecutionPath path = stepInfo.getPath();
        if (isArtificialField(nadelContext, stepInfo.getField())) {
            return true;
        }
        if (ExecutionPathUtils.isListEndingPath(path)) {
            return true;
        }
        if (completedFields.containsKey(path)) {
            return true;
        }
        return false;
    }

    public void fieldCompleted(ExecutionStepInfo stepInfo) {
//        dispatchIfNeeded(stepInfo);
//        completeNode(stepInfo, null, null);
    }

    public void fieldsCompleted(List<ExecutionResultNode> resultNodes, Throwable throwable) {
//        for (ExecutionResultNode resultNode : resultNodes) {
//            fieldsCompleted(resultNode, throwable);
//        }
    }

    public void fieldsCompleted(ExecutionResultNode resultNode, Throwable throwable) {
//        if (resultNode instanceof RootExecutionResultNode) {
//            completeNodes(resultNode.getChildren(), throwable);
//        } else {
//            completeNodes(singletonList(resultNode), throwable);
//        }
    }

    private synchronized void completeNodes(List<ExecutionResultNode> resultNodes, Throwable throwable) {
//        for (ExecutionResultNode resultNode : resultNodes) {
//
//
//            // the reason we dispatch during completion is because sub fields are not visited
//            // during the initial service call and hence they are never seen until we complete
//            // the parent top level field
//            dispatchIfNeeded(stepInfo);
//
//            //
//            // hydrated fields are the exception - they have started to execute but they still need to be completed
//            // we have another call back path for them
//            if (isHydration(resultNode)) {
//                continue;
//            }
//            completeNode(stepInfo, resultNode, throwable);
//
//            // and go down and complete the children
//            List<ExecutionResultNode> children = resultNode.getChildren();
//            completeNodes(children, throwable);
//        }
    }

    private synchronized void completeNode(ExecutionStepInfo stepInfo, ExecutionResultNode resultNode, Throwable throwable) {
        // we are re-entrant and quite stateful and hence we don't want to do things twice
        boolean skipField = skipField(stepInfo);
        ExecutionPath path = stepInfo.getPath();
        if (!skipField) {
            if (pendingFieldFetchContexts.containsKey(path)) {
                InstrumentationContext<ExecutionResultNode> ctx = pendingFieldFetchContexts.get(path);
                ctx.onCompleted(resultNode, throwable);

                pendingFieldFetchContexts.remove(path);
                completedFields.put(path, stepInfo);
            }
        }
    }

    private boolean isHydration(ExecutionResultNode resultNode) {
        boolean hydrationNode = resultNode instanceof HydrationInputNode;
        boolean directChildrenAreHydrated = resultNode instanceof ListExecutionResultNode &&
                resultNode.getChildren().stream().allMatch(n -> n instanceof HydrationInputNode);
        return hydrationNode || directChildrenAreHydrated;
    }

}
