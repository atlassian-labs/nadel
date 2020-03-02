package graphql.nadel.engine;

import graphql.Internal;
import graphql.language.Field;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.schema.GraphQLOutputType;

import java.util.Map;
import java.util.function.Consumer;

@Internal
public class UnderlyingTypeContext {

    // always available, at the beginning it is the Query type
    private final GraphQLOutputType outputTypeUnderlying;
    // the current field, is null for the root field
    private final Field field;
    private final GraphQLFieldDefinition fieldDefinitionUnderlying;
    private final GraphQLFieldsContainer fieldsContainerUnderlying;
    private final Map<String, Object> fieldArgumentValues;

    private final Object argumentValue;
    private final GraphQLArgument argumentDefinitionUnderlying;
    private final GraphQLInputValueDefinition inputValueDefinitionUnderlying;


    public UnderlyingTypeContext(Builder builder) {
        this.outputTypeUnderlying = builder.outputTypeUnderlying;
        this.field = builder.field;
        this.fieldDefinitionUnderlying = builder.fieldDefinitionUnderlying;
        this.fieldsContainerUnderlying = builder.fieldsContainerUnderlying;
        this.fieldArgumentValues = builder.fieldArgumentValues;
        this.argumentValue = builder.argumentValue;
        this.argumentDefinitionUnderlying = builder.argumentDefinitionUnderlying;
        this.inputValueDefinitionUnderlying = builder.inputValueDefinitionUnderlying;
    }

    public Map<String, Object> getFieldArgumentValues() {
        return fieldArgumentValues;
    }

    public GraphQLOutputType getOutputTypeUnderlying() {
        return outputTypeUnderlying;
    }

    public Field getField() {
        return field;
    }

    public GraphQLFieldDefinition getFieldDefinitionUnderlying() {
        return fieldDefinitionUnderlying;
    }

    public GraphQLFieldsContainer getFieldsContainerUnderlying() {
        return fieldsContainerUnderlying;
    }

    public static Builder newUnderlyingTypeContext() {
        return new Builder();
    }

    public Object getArgumentValue() {
        return argumentValue;
    }

    public GraphQLArgument getArgumentDefinitionUnderlying() {
        return argumentDefinitionUnderlying;
    }

    public GraphQLInputValueDefinition getInputValueDefinitionUnderlying() {
        return inputValueDefinitionUnderlying;
    }

    public UnderlyingTypeContext transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static class Builder {

        GraphQLOutputType outputTypeUnderlying;
        Field field;
        GraphQLFieldDefinition fieldDefinitionUnderlying;
        GraphQLFieldsContainer fieldsContainerUnderlying;
        Map<String, Object> fieldArgumentValues;
        Object argumentValue;
        GraphQLArgument argumentDefinitionUnderlying;
        GraphQLInputValueDefinition inputValueDefinitionUnderlying;

        public Builder() {

        }

        public Builder(UnderlyingTypeContext other) {
            this.outputTypeUnderlying = other.outputTypeUnderlying;
            this.field = other.field;
            this.fieldDefinitionUnderlying = other.fieldDefinitionUnderlying;
            this.fieldsContainerUnderlying = other.fieldsContainerUnderlying;
            this.argumentValue = other.argumentValue;
            this.argumentDefinitionUnderlying = other.argumentDefinitionUnderlying;
            this.inputValueDefinitionUnderlying = other.inputValueDefinitionUnderlying;
        }

        public Builder fieldArgumentValues(Map<String, Object> fieldArgumentValues) {
            this.fieldArgumentValues = fieldArgumentValues;
            return this;
        }

        public Builder fieldsContainerUnderlying(GraphQLFieldsContainer fieldsContainer) {
            this.fieldsContainerUnderlying = fieldsContainer;
            return this;
        }


        public Builder outputTypeUnderlying(GraphQLOutputType graphQLOutputType) {
            this.outputTypeUnderlying = graphQLOutputType;
            return this;
        }

        public Builder field(Field field) {
            this.field = field;
            return this;
        }

        public Builder fieldDefinitionUnderlying(GraphQLFieldDefinition graphQLFieldDefinition) {
            this.fieldDefinitionUnderlying = graphQLFieldDefinition;
            return this;
        }

        public Builder argumentValue(Object argumentValue) {
            this.argumentValue = argumentValue;
            return this;
        }

        public Builder argumentDefinitionUnderlying(GraphQLArgument argumentDefinition) {
            this.argumentDefinitionUnderlying = argumentDefinition;
            return this;
        }

        public Builder inputValueDefinitionUnderlying(GraphQLInputValueDefinition inputValueDefinition) {
            this.inputValueDefinitionUnderlying = inputValueDefinition;
            return this;
        }

        public UnderlyingTypeContext build() {
            return new UnderlyingTypeContext(this);
        }

    }
}
