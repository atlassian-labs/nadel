package graphql.nadel.result;

import graphql.execution.ExecutionStepInfo;

import java.util.function.Consumer;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    private final ExecutionStepInfo executionStepInfo;

    private UnresolvedObjectResultNode(Builder builder) {
        super(builder, null);
        this.executionStepInfo = builder.executionStepInfo;
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo;
    }

    public static Builder newUnresolvedExecutionResultNode() {
        return new Builder();
    }

    @Override
    public <T extends BuilderBase<T>> UnresolvedObjectResultNode transform(Consumer<T> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((T) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        private ExecutionStepInfo executionStepInfo;

        public Builder() {

        }

        public Builder(UnresolvedObjectResultNode existing) {
            super(existing);
            this.executionStepInfo = existing.getExecutionStepInfo();
        }

        public Builder executionStepInfo(ExecutionStepInfo executionStepInfo) {
            this.executionStepInfo = executionStepInfo;
            return this;
        }

        @Override
        public UnresolvedObjectResultNode build() {
            return new UnresolvedObjectResultNode(this);
        }
    }

}