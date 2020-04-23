package graphql.nadel.result;

import graphql.Internal;

import java.util.function.Consumer;

@Internal
public class LeafExecutionResultNode extends ExecutionResultNode {

    private LeafExecutionResultNode(Builder builder) {
        super(builder);
    }

    // hack for subclasses to pass in BuilderBase instances
    protected LeafExecutionResultNode(BuilderBase<?> builder, Object hack) {
        super(builder);
    }

    public static Builder newLeafExecutionResultNode() {
        return new Builder();
    }

    @Override
    public <T extends BuilderBase<T>> LeafExecutionResultNode transform(Consumer<T> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((T) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        public Builder() {

        }

        public Builder(LeafExecutionResultNode existing) {
            super(existing);
        }

        @Override
        public LeafExecutionResultNode build() {
            return new LeafExecutionResultNode(this);
        }
    }
}