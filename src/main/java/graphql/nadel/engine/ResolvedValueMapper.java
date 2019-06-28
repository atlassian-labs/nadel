package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.schema.GraphQLObjectType;

import java.util.Map;
import java.util.function.BiFunction;

public class ResolvedValueMapper {


    public ResolvedValue mapResolvedValue(ResolvedValue resolvedValue,
                                          UnapplyEnvironment environment) {

//        Map<String, String> typeRenameMappings = environment.typeRenameMappings;

//        GraphQLObjectType mappedResolvedType = mapResolvedType(fetchedValueAnalysis, environment, typeRenameMappings);
//        //TODO: match underlying errors
//        GraphQLObjectType finalMappedResolvedType = mappedResolvedType;
//        return fetchedValueAnalysis.transfrom(builder -> {
//            builder
//                    .resolvedType(finalMappedResolvedType)
//                    .executionStepInfo(mappedExecutionStepInfo);
//        });
        return resolvedValue;
    }

//    private GraphQLObjectType mapResolvedType(FetchedValueAnalysis fetchedValueAnalysis, UnapplyEnvironment environment, Map<String, String> typeRenameMappings) {
//        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT && !fetchedValueAnalysis.isNullValue()) {
//            String resolvedTypeName = fetchedValueAnalysis.getResolvedType().getName();
//            resolvedTypeName = typeRenameMappings.getOrDefault(resolvedTypeName, resolvedTypeName);
//            return (GraphQLObjectType) environment.overallSchema.getType(resolvedTypeName);
//        }
//        return fetchedValueAnalysis.getResolvedType();
//    }
}
