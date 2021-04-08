package graphql.nadel.engine;

import graphql.Internal;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;

import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;
    private final NormalizedQueryField normalizedField;


    private HydrationInputNode(Builder builder) {
        super(builder, null);
        this.hydrationTransformation = builder.hydrationTransformation;
        this.normalizedField = builder.normalizedField;
        assertNotNull(getFieldDefinition());
    }

    public static Builder newHydrationInputNode() {
        return new Builder();
    }

    public HydrationTransformation getHydrationTransformation() {
        return hydrationTransformation;
    }

    public NormalizedQueryField getNormalizedField() {
        return normalizedField;
    }


    @Override
    public <T extends BuilderBase<T>> HydrationInputNode transform(Consumer<T> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept((T) builder);
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        private HydrationTransformation hydrationTransformation;
        private NormalizedQueryField normalizedField;
        private ExecutionResultNode parent;

        public Builder() {

        }

        public Builder(HydrationInputNode existing) {
            super(existing);
            this.hydrationTransformation = existing.getHydrationTransformation();
            this.normalizedField = existing.getNormalizedField();
        }

        public Builder hydrationTransformation(HydrationTransformation hydrationTransformation) {
            this.hydrationTransformation = hydrationTransformation;
            return this;
        }

        public Builder normalizedField(NormalizedQueryField normalizedQueryField) {
            this.normalizedField = normalizedQueryField;
            return this;
        }

        @Override
        public HydrationInputNode build() {
            return new HydrationInputNode(this);
        }
    }


}
