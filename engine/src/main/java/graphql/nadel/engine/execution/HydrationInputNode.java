package graphql.nadel.engine.execution;

import graphql.Internal;
import graphql.language.SelectionSet;
import graphql.nadel.engine.execution.transformation.HydrationTransformation;
import graphql.nadel.engine.result.ExecutionResultNode;
import graphql.nadel.engine.result.LeafExecutionResultNode;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;
    private final NormalizedQueryField normalizedField;
    private final SelectionSet selectionSet;


    private HydrationInputNode(Builder builder) {
        super(builder, null);
        this.hydrationTransformation = builder.hydrationTransformation;
        this.normalizedField = builder.normalizedField;
        this.selectionSet = builder.selectionSet;
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

    public SelectionSet getSelectionSet() {
        return selectionSet;
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
        private SelectionSet selectionSet;

        public Builder() {

        }

        public Builder(HydrationInputNode existing) {
            super(existing);
            this.hydrationTransformation = existing.getHydrationTransformation();
            this.normalizedField = existing.getNormalizedField();
            this.selectionSet = existing.selectionSet;
        }

        public Builder hydrationTransformation(HydrationTransformation hydrationTransformation) {
            this.hydrationTransformation = hydrationTransformation;
            return this;
        }

        public Builder normalizedField(NormalizedQueryField normalizedQueryField) {
            this.normalizedField = normalizedQueryField;
            return this;
        }

        public Builder selectionSet(SelectionSet selectionSet) {
            this.selectionSet = selectionSet;
            return this;
        }

        @Override
        public HydrationInputNode build() {
            return new HydrationInputNode(this);
        }

    }


}
