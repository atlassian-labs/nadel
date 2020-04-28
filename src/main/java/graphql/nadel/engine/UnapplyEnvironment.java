package graphql.nadel.engine;

import graphql.Internal;
import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.GraphQLSchema;

import java.util.Map;

@Internal
public class UnapplyEnvironment {


    public UnapplyEnvironment(ExecutionResultNode parentNode,
                              boolean isHydrationTransformation,
                              boolean batched,
                              Map<String, String> typeRenameMappings,
                              GraphQLSchema overallSchema
    ) {
        this.isHydrationTransformation = isHydrationTransformation;
        this.batched = batched;
        this.typeRenameMappings = typeRenameMappings;
        this.overallSchema = overallSchema;
        this.parentNode = parentNode;
    }

    public ExecutionResultNode parentNode;
    public boolean isHydrationTransformation;
    public boolean batched;
    public Map<String, String> typeRenameMappings;
    public GraphQLSchema overallSchema;

}
