package graphql.nadel;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.Metadata;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.ResultComplexityAggregator;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.schema.GraphQLSchema;

import java.util.List;
import java.util.Map;

public class DebugContext {

    public static class ServiceResultNodesToOverallResultArgs {

        public ExecutionId executionId;
        public RootExecutionResultNode resultNode;
        public GraphQLSchema overallSchema;
        public RootExecutionResultNode correctRootNode;
        public Map<String, FieldTransformation> fieldIdToTransformation;
        public Map<String, String> typeRenameMappings;
        public NadelContext nadelContext;
        public Metadata metadata;
    }

    public static class NadelExecutionStrategyArgs {

        public List<Service> services;
        public FieldInfos fieldInfos;
        public GraphQLSchema overallSchema;
        public NadelInstrumentation instrumentation;
        public ServiceExecutionHooks serviceExecutionHooks;
        public ExecutionContext executionContext;
        public FieldSubSelection fieldSubSelection;
        public ResultComplexityAggregator resultComplexityAggregator;
    }

    public ServiceResultNodesToOverallResultArgs serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResultArgs();
    public NadelExecutionStrategyArgs nadelExecutionStrategyArgs = new NadelExecutionStrategyArgs();

    public ExecutionContext executionContextForService;
    public ExecutionStepInfo underlyingRootStepInfo;
    public List<MergedField> transformedMergedFields;
    public ServiceExecutionResult serviceExecutionResult;
    public ElapsedTime elapsedTime;
    public NormalizedQueryFromAst normalizedQuery;

    public RootExecutionResultNode overallResult;
}