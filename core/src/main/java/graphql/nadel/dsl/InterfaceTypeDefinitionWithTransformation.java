package graphql.nadel.dsl;

import graphql.Internal;
import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.IgnoredChars;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.NodeBuilder;
import graphql.language.SourceLocation;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

@Internal
public class InterfaceTypeDefinitionWithTransformation extends InterfaceTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    public InterfaceTypeDefinitionWithTransformation(TypeMappingDefinition typeMappingDefinition,
                                                     String name,
                                                     List<Type> implementz,
                                                     List<FieldDefinition> definitions,
                                                     List<Directive> directives,
                                                     Description description,
                                                     SourceLocation sourceLocation,
                                                     List<Comment> comments,
                                                     IgnoredChars ignoredChars,
                                                     Map<String, String> additionalData) {
        super(name, implementz, definitions, directives, description, sourceLocation, comments, ignoredChars, additionalData);
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
        private String name;
        private Description description;
        private List<Type> implementz = new ArrayList<>();
        private List<FieldDefinition> definitions = new ArrayList<>();
        private TypeMappingDefinition typeMappingDefinition;
        private List<Directive> directives = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;
        private Map<String, String> additionalData = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(InterfaceTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.definitions = existing.getFieldDefinitions();
            this.ignoredChars = existing.getIgnoredChars();
            this.additionalData = new LinkedHashMap<>(existing.getAdditionalData());
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

        public Builder implementz(List<Type> implementz) {
            this.implementz = implementz;
            return this;
        }

        public Builder implementz(Type implement) {
            this.implementz.add(implement);
            return this;
        }


        public Builder definitions(List<FieldDefinition> definitions) {
            this.definitions = definitions;
            return this;
        }

        public Builder definition(FieldDefinition definition) {
            this.definitions.add(definition);
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

        public Builder additionalData(Map<String, String> additionalData) {
            this.additionalData = assertNotNull(additionalData);
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.put(key, value);
            return this;
        }


        public InterfaceTypeDefinitionWithTransformation build() {
            return new InterfaceTypeDefinitionWithTransformation(
                    typeMappingDefinition,
                    name,
                    implementz,
                    definitions,
                    directives,
                    description,
                    sourceLocation,
                    comments,
                    ignoredChars,
                    additionalData);
        }
    }
}
