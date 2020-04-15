package graphql.nadel.result;

import graphql.execution.ExecutionStepInfo;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    private final ExecutionStepInfo executionStepInfo;
    private final NormalizedQueryField normalizedField;

    private UnresolvedObjectResultNode(Builder builder) {
        super(builder, null);
        this.executionStepInfo = builder.executionStepInfo;
        this.normalizedField = assertNotNull(builder.normalizedField);
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo;
    }

    public NormalizedQueryField getNormalizedField() {
        return normalizedField;
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
        private NormalizedQueryField normalizedField;

        public Builder() {

        }

        public Builder(UnresolvedObjectResultNode existing) {
            super(existing);
            this.executionStepInfo = existing.getExecutionStepInfo();
            this.normalizedField = existing.getNormalizedField();
        }

        public Builder executionStepInfo(ExecutionStepInfo executionStepInfo) {
            this.executionStepInfo = executionStepInfo;
            return this;
        }

        public Builder normalizedField(NormalizedQueryField normalizedField) {
            this.normalizedField = normalizedField;
            return this;
        }

        @Override
        public UnresolvedObjectResultNode build() {
            return new UnresolvedObjectResultNode(this);
        }
    }

}