package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.schema.GraphQLObjectType;

import java.util.Map;
import java.util.function.BiFunction;

public class FetchedValueAnalysisMapper {



    public FetchedValueAnalysis mapFetchedValueAnalysis(FetchedValueAnalysis fetchedValueAnalysis,
                                                        UnapplyEnvironment environment,
                                                        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper) {
        ExecutionStepInfo executionStepInfo = fetchedValueAnalysis.getExecutionStepInfo();
        ExecutionStepInfo mappedExecutionStepInfo = esiMapper.apply(executionStepInfo, environment);

        Map<String, String> typeRenameMappings = environment.typeRenameMappings;

        GraphQLObjectType mappedResolvedType = mapResolvedType(fetchedValueAnalysis, environment, typeRenameMappings);
        //TODO: match underlying errors
        GraphQLObjectType finalMappedResolvedType = mappedResolvedType;
        return fetchedValueAnalysis.transfrom(builder -> {
            builder
                    .resolvedType(finalMappedResolvedType)
                    .executionStepInfo(mappedExecutionStepInfo);
        });
    }

    private GraphQLObjectType mapResolvedType(FetchedValueAnalysis fetchedValueAnalysis, UnapplyEnvironment environment, Map<String, String> typeRenameMappings) {
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT && !fetchedValueAnalysis.isNullValue()) {
            String resolvedTypeName = fetchedValueAnalysis.getResolvedType().getName();
            resolvedTypeName = typeRenameMappings.getOrDefault(resolvedTypeName, resolvedTypeName);
            return (GraphQLObjectType) environment.overallSchema.getType(resolvedTypeName);
        }
        return fetchedValueAnalysis.getResolvedType();
    }
}
