package graphql.nadel;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ElapsedTime;

import java.util.List;

public class DebugContext {

    public ExecutionContext executionContextForService;
    public ExecutionStepInfo underlyingRootStepInfo;
    public List<MergedField> transformedMergedFields;
    public ServiceExecutionResult serviceExecutionResult;
    public ElapsedTime elapsedTime;
    public NormalizedQueryFromAst normalizedQuery;
}