package graphql.nadel.engine;

import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalyzer;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

public class DelegatedResultAnalyzser extends FetchedValueAnalyzer {

    @Override
    protected Object serializeScalarValue(Object toAnalyze, GraphQLScalarType scalarType, ExecutionStepInfo executionStepInfo) throws CoercingSerializeException {
        if (scalarType == Scalars.GraphQLString) {
            if (toAnalyze instanceof String) {
                return toAnalyze;
            } else {
                throw new CoercingSerializeException("Unexpected value '" + toAnalyze + "'. String expected");
            }
        }
        return super.serializeScalarValue(toAnalyze, scalarType, executionStepInfo);
    }
}
