package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.schema.GraphQLSchema;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;

import java.util.List;
import java.util.Map;

public class UnapplyEnvironment {


    public UnapplyEnvironment(TraverserContext<ExecutionResultNode> context,
                              ExecutionStepInfo parentExecutionStepInfo,
                              boolean isHydrationTransformation,
                              boolean batched,
                              Map<String, String> typeRenameMappings,
                              GraphQLSchema overallSchema,
                              List<Field> notTransformedFields) {
        this.context = context;
        this.parentExecutionStepInfo = parentExecutionStepInfo;
        this.isHydrationTransformation = isHydrationTransformation;
        this.batched = batched;
        this.typeRenameMappings = typeRenameMappings;
        this.overallSchema = overallSchema;
        this.notTransformedFields = notTransformedFields;
    }

    public TraverserContext<ExecutionResultNode> context;
    public ExecutionStepInfo parentExecutionStepInfo;
    public boolean isHydrationTransformation;
    public boolean batched;
    public Map<String, String> typeRenameMappings;
    public GraphQLSchema overallSchema;
    public List<Field> notTransformedFields;

    public boolean treeIsSplit;

    public void unapplyNode(ExecutionResultNode node) {
        if (treeIsSplit) {
            TreeTransformerUtil.insertAfter(context, node);
        } else {
            TreeTransformerUtil.changeNode(context, node);
        }
    }

}
