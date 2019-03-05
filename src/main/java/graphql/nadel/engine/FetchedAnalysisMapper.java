package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.Map;

public class FetchedAnalysisMapper {

    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();


    public FetchedValueAnalysis mapFetchedValueAnalysis(FetchedValueAnalysis fetchedValueAnalysis,
                                                        GraphQLSchema overallSchema,
                                                        Map<Field, FieldTransformation> transformationMap) {
        ExecutionStepInfo mappedExecutionStepInfo = executionStepInfoMapper.mapExecutionStepInfo(fetchedValueAnalysis.getExecutionStepInfo(), overallSchema, transformationMap);
        GraphQLObjectType mappedResolvedType = null;
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT && !fetchedValueAnalysis.isNullValue()) {
            mappedResolvedType = (GraphQLObjectType) overallSchema.getType(fetchedValueAnalysis.getResolvedType().getName());
        }
        //TODO: match underlying errors
        GraphQLObjectType finalMappedResolvedType = mappedResolvedType;
        return fetchedValueAnalysis.transfrom(builder -> {
            builder
                    .resolvedType(finalMappedResolvedType)
                    .executionStepInfo(mappedExecutionStepInfo);
        });
    }
}
