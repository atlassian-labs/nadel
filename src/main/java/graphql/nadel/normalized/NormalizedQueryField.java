package graphql.nadel.normalized;

import graphql.Internal;
import graphql.execution.MergedField;
import graphql.language.Argument;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;
import static graphql.schema.GraphQLTypeUtil.simplePrint;

@Internal
public class NormalizedQueryField {

    private final MergedField mergedField;
    private final GraphQLObjectType objectType;
    private final GraphQLFieldDefinition fieldDefinition;
    private final GraphQLFieldsContainer fieldsContainer;
    private final List<NormalizedQueryField> children;
    private final boolean isConditional;
    private final int level;
    private NormalizedQueryField parent;


    private NormalizedQueryField(Builder builder) {
        this.mergedField = builder.mergedField;
        this.objectType = builder.objectType;
        this.fieldDefinition = assertNotNull(builder.fieldDefinition);
        this.fieldsContainer = assertNotNull(builder.fieldsContainer);
        this.children = builder.children;
        this.level = builder.level;
        this.parent = builder.parent;
        // can be null for the top level fields
        if (parent == null) {
            this.isConditional = false;
        } else {
            GraphQLUnmodifiedType parentType = GraphQLTypeUtil.unwrapAll(parent.getFieldDefinition().getType());
            this.isConditional = parentType != this.objectType;
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
        return mergedField.getName();
    }

    /**
     * Returns the key of this MergedFieldWithType for the overall result.
     * This is either an alias or the FieldWTC name.
     *
     * @return the key for this MergedFieldWithType.
     */
    public String getResultKey() {
        Field singleField = getSingleField();
        if (singleField.getAlias() != null) {
            return singleField.getAlias();
        }
        return singleField.getName();
    }

    /**
     * The first of the merged fields.
     *
     * Because all fields are almost identically
     * often only one of the merged fields are used.
     *
     * @return the fist of the merged Fields
     */
    public Field getSingleField() {
        return mergedField.getSingleField();
    }

    /**
     * All merged fields share the same arguments.
     *
     * @return the list of arguments
     */
    public List<Argument> getArguments() {
        return getSingleField().getArguments();
    }


    public MergedField getMergedField() {
        return mergedField;
    }

    public static Builder newQueryExecutionField() {
        return new Builder();
    }

    public static Builder newQueryExecutionField(Field field) {
        return new Builder().mergedField(MergedField.newMergedField().addField(field).build());
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

    public GraphQLFieldsContainer getFieldsContainer() {
        return fieldsContainer;
    }


    public String print() {
        StringBuilder result = new StringBuilder();
        Field singleField = getSingleField();
        if (singleField.getAlias() != null) {
            result.append(singleField.getAlias()).append(": ");
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

    @Override
    public String toString() {
        return "QueryExecutionField{" +
                "mergedField" + mergedField +
                ", objectType=" + objectType +
                ", fieldDefinition=" + fieldDefinition +
                ", fieldsContainer=" + fieldsContainer +
                ", children=" + children +
                ", isConditional=" + isConditional +
                '}';
    }

    public static class Builder {
        private MergedField mergedField;
        private GraphQLObjectType objectType;
        private GraphQLFieldDefinition fieldDefinition;
        private GraphQLFieldsContainer fieldsContainer;
        private List<NormalizedQueryField> children = new ArrayList<>();
        private int level;
        private NormalizedQueryField parent;

        private Builder() {

        }

        private Builder(NormalizedQueryField existing) {
            this.mergedField = existing.getMergedField();
            this.objectType = existing.getObjectType();
            this.fieldDefinition = existing.getFieldDefinition();
            this.fieldsContainer = existing.getFieldsContainer();
            this.children = existing.getChildren();
            this.level = existing.getLevel();
            this.parent = existing.getParent();
        }


        public Builder objectType(GraphQLObjectType objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder mergedField(MergedField mergedField) {
            this.mergedField = mergedField;
            return this;
        }

        public Builder fieldDefinition(GraphQLFieldDefinition fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
            return this;
        }

        public Builder fieldsContainer(GraphQLFieldsContainer fieldsContainer) {
            this.fieldsContainer = fieldsContainer;
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
