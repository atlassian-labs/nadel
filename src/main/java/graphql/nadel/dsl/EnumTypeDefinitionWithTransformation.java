package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.IgnoredChars;
import graphql.language.NodeBuilder;
import graphql.language.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnumTypeDefinitionWithTransformation extends EnumTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    protected EnumTypeDefinitionWithTransformation(TypeMappingDefinition typeMappingDefinition, String name, List<EnumValueDefinition> enumValueDefinitions, List<Directive> directives, Description description, SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        super(name, enumValueDefinitions, directives, description, sourceLocation, comments, ignoredChars, Collections.emptyMap());
        this.typeMappingDefinition = typeMappingDefinition;
    }

    public TypeMappingDefinition getTypeMappingDefinition() {
        return typeMappingDefinition;
    }

    public static Builder newEnumTypeDefinitionWithTransformation(EnumTypeDefinition existing) {
        return new Builder(existing);
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private String name;
        private Description description;
        private List<EnumValueDefinition> enumValueDefinitions = new ArrayList<>();
        private List<Directive> directives = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;
        private TypeMappingDefinition typeMappingDefinition;

        private Builder() {
        }

        private Builder(EnumTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.enumValueDefinitions = existing.getEnumValueDefinitions();
            this.ignoredChars = existing.getIgnoredChars();
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

        public Builder enumValueDefinitions(List<EnumValueDefinition> enumValueDefinitions) {
            this.enumValueDefinitions = enumValueDefinitions;
            return this;
        }

        public Builder enumValueDefinition(EnumValueDefinition enumValueDefinition) {
            this.enumValueDefinitions.add(enumValueDefinition);
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

        public Builder ignoredChars(IgnoredChars ignoredChars) {
            this.ignoredChars = ignoredChars;
            return this;
        }

        @Override
        public NodeBuilder additionalData(Map<String, String> additionalData) {
            return null;
        }

        @Override
        public NodeBuilder additionalData(String key, String value) {
            return null;
        }

        public EnumTypeDefinitionWithTransformation build() {
            return new EnumTypeDefinitionWithTransformation(typeMappingDefinition, name, enumValueDefinitions, directives, description, sourceLocation, comments, ignoredChars);
        }
    }
}
