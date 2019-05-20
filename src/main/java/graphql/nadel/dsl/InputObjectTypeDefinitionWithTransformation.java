package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.IgnoredChars;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.NodeBuilder;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.List;

public class InputObjectTypeDefinitionWithTransformation extends InputObjectTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    protected InputObjectTypeDefinitionWithTransformation(TypeMappingDefinition typeMappingDefinition, String name, List<Directive> directives, List<InputValueDefinition> inputValueDefinitions, Description description, SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        super(name, directives, inputValueDefinitions, description, sourceLocation, comments, ignoredChars);
        this.typeMappingDefinition = typeMappingDefinition;
    }


    public TypeMappingDefinition getTypeMappingDefinition() {
        return typeMappingDefinition;
    }

    public static Builder newInputObjectTypeDefinitionWithTransformation(InputObjectTypeDefinition existing) {
        return new Builder(existing);
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private String name;
        private Description description;
        private List<Directive> directives = new ArrayList<>();
        private List<InputValueDefinition> inputValueDefinitions = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;
        private TypeMappingDefinition typeMappingDefinition;

        private Builder() {
        }

        private Builder(InputObjectTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.inputValueDefinitions = existing.getInputValueDefinitions();
        }


        public Builder typeMappingDefinition(TypeMappingDefinition typeMappingDefinition) {
            this.typeMappingDefinition = typeMappingDefinition;
            return this;
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(Description description) {
            this.description = description;
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

        public Builder inputValueDefinitions(List<InputValueDefinition> inputValueDefinitions) {
            this.inputValueDefinitions = inputValueDefinitions;
            return this;
        }

        public Builder inputValueDefinition(InputValueDefinition inputValueDefinition) {
            this.inputValueDefinitions.add(inputValueDefinition);
            return this;
        }

        public Builder ignoredChars(IgnoredChars ignoredChars) {
            this.ignoredChars = ignoredChars;
            return this;
        }

        public InputObjectTypeDefinition build() {
            return new InputObjectTypeDefinitionWithTransformation(typeMappingDefinition,
                    name,
                    directives,
                    inputValueDefinitions,
                    description,
                    sourceLocation,
                    comments,
                    ignoredChars);
        }
    }
}