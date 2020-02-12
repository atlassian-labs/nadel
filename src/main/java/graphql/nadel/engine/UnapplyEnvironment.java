package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.GraphQLSchema;

import java.util.Map;

public class UnapplyEnvironment {


    public UnapplyEnvironment(ExecutionStepInfo parentExecutionStepInfo,
                              boolean isHydrationTransformation,
                              boolean batched,
                              Map<String, String> typeRenameMappings,
                              GraphQLSchema overallSchema
    ) {
        this.parentExecutionStepInfo = parentExecutionStepInfo;
        this.isHydrationTransformation = isHydrationTransformation;
        this.batched = batched;
        this.typeRenameMappings = typeRenameMappings;
        this.overallSchema = overallSchema;
    }

    public ExecutionStepInfo parentExecutionStepInfo;
    public boolean isHydrationTransformation;
    public boolean batched;
    public Map<String, String> typeRenameMappings;
    public GraphQLSchema overallSchema;

}
