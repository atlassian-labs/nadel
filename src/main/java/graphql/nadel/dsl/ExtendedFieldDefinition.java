package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.IgnoredChars;
import graphql.language.InputValueDefinition;
import graphql.language.NodeBuilder;
import graphql.language.NodeDirectivesBuilder;
import graphql.language.SourceLocation;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExtendedFieldDefinition extends FieldDefinition {

    private final FieldTransformation fieldTransformation;

    private Integer defaultBatchSize;

    protected ExtendedFieldDefinition(String name,
                                      Type type,
                                      List<InputValueDefinition> inputValueDefinitions,
                                      List<Directive> directives,
                                      Description description,
                                      FieldTransformation fieldTransformation, SourceLocation sourceLocation,
                                      List<Comment> comments,
                                      Integer defaultBatchSize) {
        super(name, type, inputValueDefinitions, directives, description, sourceLocation, comments, IgnoredChars.EMPTY, Collections.emptyMap());
        this.fieldTransformation = fieldTransformation;
        this.defaultBatchSize = defaultBatchSize;
    }

    public FieldTransformation getFieldTransformation() {
        return fieldTransformation;
    }

    public Integer getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public static Builder newExtendedFieldDefinition(FieldDefinition copyFrom) {
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
        private Integer defaultBatchSize;

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

        @Override
        public NodeBuilder ignoredChars(IgnoredChars ignoredChars) {
            return null;
        }

        @Override
        public NodeBuilder additionalData(Map<String, String> additionalData) {
            return null;
        }

        @Override
        public NodeBuilder additionalData(String key, String value) {
            return null;
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

        public Builder defaultBatchSize(Integer defaultBatchSize) {
            this.defaultBatchSize = defaultBatchSize;
            return this;
        }

        public ExtendedFieldDefinition build() {
            ExtendedFieldDefinition fieldDefinition = new ExtendedFieldDefinition(name,
                    type,
                    inputValueDefinitions,
                    directives,
                    description,
                    fieldTransformation,
                    sourceLocation,
                    comments,
                    defaultBatchSize);
            return fieldDefinition;
        }
    }

}
