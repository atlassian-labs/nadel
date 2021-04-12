package graphql.nadel.normalized;

import graphql.Internal;
import graphql.language.Argument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;
import static graphql.schema.GraphQLTypeUtil.simplePrint;
import static java.util.Collections.singletonList;

@Internal
public class NormalizedQueryField {

    private final String alias;
    private final List<Argument> arguments;
    private final GraphQLObjectType objectType;
    private final GraphQLFieldDefinition fieldDefinition;
    private final List<NormalizedQueryField> children;
    private final boolean isConditional;
    private final int level;
    private final List<String> path;
    private NormalizedQueryField parent;


    private NormalizedQueryField(Builder builder) {
        this.alias = builder.alias;
        this.arguments = builder.arguments;
        this.objectType = builder.objectType;
        this.fieldDefinition = assertNotNull(builder.fieldDefinition);
        this.children = builder.children;
        this.level = builder.level;
        this.parent = builder.parent;
        // can be null for the top level fields
        if (parent == null) {
            this.isConditional = false;
            this.path = singletonList(getResultKey());
        } else {
            GraphQLUnmodifiedType parentType = GraphQLTypeUtil.unwrapAll(parent.getFieldDefinition().getType());
            this.isConditional = parentType != this.objectType;
            this.path = new ArrayList<>(parent.getPath());
            this.path.add(getResultKey());
        }
    }

    /**
     * All merged fields have the same name.
     *
     * WARNING: This is not always the key in the execution result, because of possible aliases. See {@link #getResultKey()}
     *
     * @return the name of of the merged fields.
     */
    public String getName() {
        return getFieldDefinition().getName();
    }

    /**
     * Returns the key of this MergedFieldWithType for the overall result.
     * This is either an alias or the FieldWTC name.
     *
     * @return the key for this MergedFieldWithType.
     */
    public String getResultKey() {
        if (alias != null) {
            return alias;
        }
        return getName();
    }

    public String getAlias() {
        return alias;
    }

    /**
     * All merged fields share the same arguments.
     *
     * @return the list of arguments
     */
    public List<Argument> getArguments() {
        return arguments;
    }


    public static Builder newQueryExecutionField() {
        return new Builder();
    }


    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }


    public NormalizedQueryField transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public GraphQLObjectType getObjectType() {
        return objectType;
    }


    public String print() {
        StringBuilder result = new StringBuilder();
        if (getAlias() != null) {
            result.append(getAlias()).append(": ");
        }
        return result + objectType.getName() + "." + fieldDefinition.getName() + ": " + simplePrint(fieldDefinition.getType()) +
                " (conditional: " + this.isConditional + ")";
    }

    public List<NormalizedQueryField> getChildren() {
        return children;
    }

    public int getLevel() {
        return level;
    }

    public NormalizedQueryField getParent() {
        return parent;
    }

    public void replaceParent(NormalizedQueryField newParent) {
        this.parent = newParent;
    }

    public List<String> getPath() {
        return path;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("QueryExecutionField{");
        if (alias != null) {
            result.append(alias).append(": ");
        }
        result.append(path).append("/");
        result.append(objectType.getName())
                .append("}");

        return result.toString();
    }

    public static class Builder {
        private GraphQLObjectType objectType;
        private GraphQLFieldDefinition fieldDefinition;
        private List<NormalizedQueryField> children = new ArrayList<>();
        private int level;
        private NormalizedQueryField parent;
        private String alias;
        private List<Argument> arguments = new ArrayList<>();

        private Builder() {

        }

        private Builder(NormalizedQueryField existing) {
            this.alias = existing.alias;
            this.arguments = existing.arguments;
            this.objectType = existing.getObjectType();
            this.fieldDefinition = existing.getFieldDefinition();
            this.children = existing.getChildren();
            this.level = existing.getLevel();
            this.parent = existing.getParent();
        }

        public Builder objectType(GraphQLObjectType objectType) {
            this.objectType = objectType;
            return this;
        }


        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder arguments(List<Argument> arguments) {
            this.arguments = arguments;
            return this;
        }


        public Builder fieldDefinition(GraphQLFieldDefinition fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
            return this;
        }


        public Builder children(List<NormalizedQueryField> children) {
            this.children.clear();
            this.children.addAll(children);
            return this;
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder parent(NormalizedQueryField parent) {
            this.parent = parent;
            return this;
        }

        public NormalizedQueryField build() {
            return new NormalizedQueryField(this);
        }


    }

}
