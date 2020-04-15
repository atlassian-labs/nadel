package graphql.nadel.result;

import graphql.nadel.normalized.NormalizedQueryField;
import graphql.schema.GraphQLObjectType;

import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    private final NormalizedQueryField normalizedField;
    private final GraphQLObjectType resolvedType;

    private UnresolvedObjectResultNode(Builder builder) {
        super(builder, null);
        this.normalizedField = assertNotNull(builder.normalizedField);
        this.resolvedType = assertNotNull(builder.resolvedType);
    }

    public NormalizedQueryField getNormalizedField() {
        return normalizedField;
    }

    public GraphQLObjectType getResolvedType() {
        return resolvedType;
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

        private NormalizedQueryField normalizedField;
        private GraphQLObjectType resolvedType;

        public Builder() {

        }

        public Builder(UnresolvedObjectResultNode existing) {
            super(existing);
            this.normalizedField = existing.getNormalizedField();
            this.resolvedType = existing.getResolvedType();
        }


        public Builder normalizedField(NormalizedQueryField normalizedField) {
            this.normalizedField = normalizedField;
            return this;
        }

        public Builder resolvedType(GraphQLObjectType resolvedType) {
            this.resolvedType = resolvedType;
            return this;
        }

        @Override
        public UnresolvedObjectResultNode build() {
            return new UnresolvedObjectResultNode(this);
        }
    }

}