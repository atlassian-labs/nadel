package graphql.nadel.result;

import graphql.Internal;

import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public class ObjectExecutionResultNode extends ExecutionResultNode {


    private ObjectExecutionResultNode(Builder builder) {
        super(builder);
        assertNotNull(getField());
    }

    // hack for subclasses to pass in BuilderBase instances
    protected ObjectExecutionResultNode(BuilderBase<?> builder, Object hack) {
        super(builder);
    }

    public static Builder newObjectExecutionResultNode() {
        return new Builder();
    }

    @Override
    public <T extends BuilderBase<T>> ObjectExecutionResultNode transform(Consumer<T> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((T) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        public Builder() {

        }

        public Builder(ObjectExecutionResultNode existing) {
            super(existing);
        }

        @Override
        public ObjectExecutionResultNode build() {
            return new ObjectExecutionResultNode(this);
        }
    }


}
