package graphql.nadel.result;

import graphql.Internal;

import java.util.function.Consumer;

@Internal
public class ListExecutionResultNode extends ExecutionResultNode {

//    public ListExecutionResultNode(ExecutionStepInfo executionStepInfo,
//                                   ResolvedValue resolvedValue,
//                                   List<ExecutionResultNode> children,
//                                   List<GraphQLError> errors) {
//        super(executionStepInfo, resolvedValue, ResultNodesUtil.newNullableException(executionStepInfo, children), children, errors, null);
//    }
//
//    public ListExecutionResultNode(ExecutionStepInfo executionStepInfo,
//                                   ResolvedValue resolvedValue,
//                                   List<ExecutionResultNode> children,
//                                   List<GraphQLError> errors,
//                                   ElapsedTime elapsedTime) {
//        super(executionStepInfo, resolvedValue, ResultNodesUtil.newNullableException(executionStepInfo, children), children, errors, elapsedTime);
//    }

    private ListExecutionResultNode(Builder builder) {
        super(builder);
    }

    public static Builder newListExecutionResultNode() {
        return new Builder();
    }

    @Override
    public <B extends BuilderBase<B>> ListExecutionResultNode transform(Consumer<B> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((B) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        public Builder() {

        }

        public Builder(ListExecutionResultNode existing) {
            super(existing);
        }

        @Override
        public ListExecutionResultNode build() {
            return new ListExecutionResultNode(this);
        }
    }

}
