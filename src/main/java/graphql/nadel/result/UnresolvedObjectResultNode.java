package graphql.nadel.result;

import java.util.function.Consumer;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    private UnresolvedObjectResultNode(Builder builder) {
        super(builder, null);
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

        public Builder() {

        }

        public Builder(UnresolvedObjectResultNode existing) {
            super(existing);
        }

        @Override
        public UnresolvedObjectResultNode build() {
            return new UnresolvedObjectResultNode(this);
        }
    }

}