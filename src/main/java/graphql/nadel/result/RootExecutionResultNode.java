package graphql.nadel.result;

import java.util.function.Consumer;

public class RootExecutionResultNode extends ObjectExecutionResultNode {


    private RootExecutionResultNode(Builder builder) {
        super(builder, null);
    }

    public static Builder newRootExecutionResultNode() {
        return new Builder();
    }

    @Override
    public <T extends BuilderBase<T>> RootExecutionResultNode transform(Consumer<T> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((T) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        public Builder() {

        }

        public Builder(RootExecutionResultNode existing) {
            super(existing);
        }

        @Override
        public RootExecutionResultNode build() {
            return new RootExecutionResultNode(this);
        }
    }


//    @Override
//    public ExecutionStepInfo getExecutionStepInfo() {
//        return assertShouldNeverHappen("not supported at root node");
//    }
//
//    @Override
//    public ResolvedValue getResolvedValue() {
//        return assertShouldNeverHappen("not supported at root node");
//    }
//
}
