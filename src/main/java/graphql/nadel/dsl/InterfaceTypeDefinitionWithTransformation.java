package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.IgnoredChars;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.NodeBuilder;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.List;

public class InterfaceTypeDefinitionWithTransformation extends InterfaceTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    protected InterfaceTypeDefinitionWithTransformation(String name,
                                                        List<FieldDefinition> fieldDefinitions,
                                                        List<Directive> directives,
                                                        Description description,
                                                        SourceLocation sourceLocation,
                                                        List<Comment> comments,
                                                        IgnoredChars ignoredChars,
                                                        TypeMappingDefinition typeMappingDefinition) {
        super(name, fieldDefinitions, directives, description, sourceLocation, comments, ignoredChars);
        this.typeMappingDefinition = typeMappingDefinition;
    }

    public TypeMappingDefinition getTypeMappingDefinition() {
        return typeMappingDefinition;
    }

    public static InterfaceTypeDefinitionWithTransformation.Builder newInterfaceTypeDefinitionWithTransformation(InterfaceTypeDefinition copyFrom) {
        return new InterfaceTypeDefinitionWithTransformation.Builder(copyFrom);
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;
        private String name;
        private Description description;
        private List<Directive> directives = new ArrayList<>();
        private List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        private TypeMappingDefinition typeMappingDefinition;

        private Builder() {
        }

        private Builder(InterfaceTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.fieldDefinitions = existing.getFieldDefinitions();
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

        @Override
        public NodeBuilder ignoredChars(IgnoredChars ignoredChars) {
            this.ignoredChars = ignoredChars;
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

        public Builder fieldDefinitions(List<FieldDefinition> fieldDefinitions) {
            this.fieldDefinitions = fieldDefinitions;
            return this;
        }

        public Builder fieldDefinition(FieldDefinition fieldDefinition) {
            this.fieldDefinitions.add(fieldDefinition);
            return this;
        }

        public InterfaceTypeDefinitionWithTransformation build() {
            return new InterfaceTypeDefinitionWithTransformation(
                    name,
                    fieldDefinitions,
                    directives,
                    description,
                    sourceLocation,
                    comments,
                    ignoredChars,
                    typeMappingDefinition
            );
        }
    }
}
