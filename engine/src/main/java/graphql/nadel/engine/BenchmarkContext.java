package graphql.nadel.engine;

import graphql.ExecutionInput;
import graphql.Internal;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.language.Document;
import graphql.nadel.NadelExecutionParams;
import graphql.nadel.Service;
import graphql.nadel.ServiceExecutionResult;
import graphql.nadel.engine.execution.NadelExecutionStrategy;
import graphql.nadel.engine.execution.transformation.FieldTransformation;
import graphql.nadel.engine.execution.transformation.TransformationMetadata;
import graphql.nadel.engine.result.ElapsedTime;
import graphql.nadel.engine.result.ResultComplexityAggregator;
import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.schema.GraphQLSchema;

import java.util.List;
import java.util.Map;

@Internal
public class BenchmarkContext {

    public static class ServiceResultNodesToOverallResultArgs {

        public ExecutionId executionId;
        public RootExecutionResultNode resultNode;
        public GraphQLSchema overallSchema;
        public RootExecutionResultNode correctRootNode;
        public Map<String, FieldTransformation> transformationIdToTransformation;
        public Map<FieldTransformation, String> transformationToFieldId;
        public Map<String, String> typeRenameMappings;
        public NadelContext nadelContext;
        public TransformationMetadata transformationMetadata;
        public NadelExecutionStrategy.ExecutionPathSet hydrationInputPaths = new NadelExecutionStrategy.ExecutionPathSet();
    }

    public static class ExecutionArgs {

        public List<Service> services;
        public GraphQLSchema overallSchema;
        public NadelInstrumentation instrumentation;
        public IntrospectionRunner introspectionRunner;
        public ServiceExecutionHooks serviceExecutionHooks;
        public Object context;
        public ExecutionInput executionInput;
        public Document document;
        public ExecutionId executionId;
        public InstrumentationState instrumentationState;
        public NadelExecutionParams nadelExecutionParams;
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

    public static class ServiceResultToResultNodesArgs {
        public ExecutionContext executionContextForService;
        public ExecutionStepInfo underlyingRootStepInfo;
        public List<MergedField> transformedMergedFields;
        public ServiceExecutionResult serviceExecutionResult;
        public ElapsedTime elapsedTime;
        public NormalizedQueryFromAst normalizedQuery;
    }

    public ServiceResultNodesToOverallResultArgs serviceResultNodesToOverallResult = new ServiceResultNodesToOverallResultArgs();
    public NadelExecutionStrategyArgs nadelExecutionStrategyArgs = new NadelExecutionStrategyArgs();
    public ExecutionArgs executionArgs = new ExecutionArgs();
    public ServiceResultToResultNodesArgs serviceResultToResultNodesArgs = new ServiceResultToResultNodesArgs();


    public RootExecutionResultNode overallResult;
}
