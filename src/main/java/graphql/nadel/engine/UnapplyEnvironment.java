package graphql.nadel.engine;

import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.GraphQLSchema;

import java.util.Map;

public class UnapplyEnvironment {


    public UnapplyEnvironment(ExecutionResultNode correctParentTypes,
                              boolean isHydrationTransformation,
                              boolean batched,
                              Map<String, String> typeRenameMappings,
                              GraphQLSchema overallSchema
    ) {
        this.isHydrationTransformation = isHydrationTransformation;
        this.batched = batched;
        this.typeRenameMappings = typeRenameMappings;
        this.overallSchema = overallSchema;
        this.correctParentTypes = correctParentTypes;
    }

    public ExecutionResultNode correctParentTypes;
    public boolean isHydrationTransformation;
    public boolean batched;
    public Map<String, String> typeRenameMappings;
    public GraphQLSchema overallSchema;

}
