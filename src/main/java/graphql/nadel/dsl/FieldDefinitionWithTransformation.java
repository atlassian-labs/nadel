package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.NodeDirectivesBuilder;
import graphql.language.SourceLocation;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.List;

public class FieldDefinitionWithTransformation extends FieldDefinition {

    private final FieldTransformation fieldTransformation;


    protected FieldDefinitionWithTransformation(String name,
                                                Type type,
                                                List<InputValueDefinition> inputValueDefinitions,
                                                List<Directive> directives,
                                                Description description,
                                                FieldTransformation fieldTransformation, SourceLocation sourceLocation,
                                                List<Comment> comments) {
        super(name, type, inputValueDefinitions, directives, description, sourceLocation, comments);
        this.fieldTransformation = fieldTransformation;
    }

    public FieldTransformation getFieldTransformation() {
        return fieldTransformation;
    }

    public static Builder newFieldDefinitionWithTransformation(FieldDefinition copyFrom) {
        return new Builder(copyFrom);
    }

    public static final class Builder implements NodeDirectivesBuilder {
        private SourceLocation sourceLocation;
        private String name;
        private List<Comment> comments = new ArrayList<>();
        private Type type;
        private Description description;
        private List<InputValueDefinition> inputValueDefinitions = new ArrayList<>();
        private List<Directive> directives = new ArrayList<>();
        private FieldTransformation fieldTransformation;

        private Builder() {
        }

        private Builder(FieldDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.name = existing.getName();
            this.comments = existing.getComments();
            this.type = existing.getType();
            this.description = existing.getDescription();
            this.inputValueDefinitions = existing.getInputValueDefinitions();
            this.directives = existing.getDirectives();
        }


        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder description(Description description) {
            this.description = description;
            return this;
        }

        public Builder inputValueDefinitions(List<InputValueDefinition> inputValueDefinitions) {
            this.inputValueDefinitions = inputValueDefinitions;
            return this;
        }

        public Builder inputValueDefinition(InputValueDefinition inputValueDefinitions) {
            this.inputValueDefinitions.add(inputValueDefinitions);
            return this;
        }

        public Builder directives(List<Directive> directives) {
            this.directives = directives;
            return this;
        }

        public Builder directive(Directive directive) {
            this.directives.add(directive);
            return this;
        }

        public Builder fieldTransformation(FieldTransformation fieldTransformation) {
            this.fieldTransformation = fieldTransformation;
            return this;
        }

        public FieldDefinitionWithTransformation build() {
            FieldDefinitionWithTransformation fieldDefinition = new FieldDefinitionWithTransformation(name,
                    type,
                    inputValueDefinitions,
                    directives,
                    description,
                    fieldTransformation,
                    sourceLocation,
                    comments);
            return fieldDefinition;
        }
    }

}
