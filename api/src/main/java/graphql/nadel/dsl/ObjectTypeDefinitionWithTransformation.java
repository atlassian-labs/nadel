package graphql.nadel.dsl;

import graphql.Internal;
import graphql.com.google.common.collect.ImmutableList;
import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.IgnoredChars;
import graphql.language.NodeBuilder;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SourceLocation;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

@Internal
public class ObjectTypeDefinitionWithTransformation extends ObjectTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    protected ObjectTypeDefinitionWithTransformation(TypeMappingDefinition typeMappingDefinition,
                                                     String name,
                                                     List<Type> implementz,
                                                     List<Directive> directives,
                                                     List<FieldDefinition> fieldDefinitions,
                                                     Description description,
                                                     SourceLocation sourceLocation,
                                                     List<Comment> comments,
                                                     IgnoredChars ignoredChars,
                                                     Map<String, String> additionalData) {
        super(name, implementz, directives, fieldDefinitions, description, sourceLocation, comments, ignoredChars, additionalData);
        this.typeMappingDefinition = typeMappingDefinition;
    }

    public TypeMappingDefinition getTypeMappingDefinition() {
        return typeMappingDefinition;
    }

    public static ObjectTypeDefinitionWithTransformation.Builder newObjectTypeDefinitionWithTransformation(ObjectTypeDefinition copyFrom) {
        return new ObjectTypeDefinitionWithTransformation.Builder(copyFrom);
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private String name;
        private Description description;
        private List<Type> implementz = new ArrayList<>();
        private List<Directive> directives = new ArrayList<>();
        private List<FieldDefinition> fieldDefinitions = new ArrayList<>();
        private TypeMappingDefinition typeMappingDefinition;
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;
        private Map<String, String> additionalData = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(ObjectTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.implementz = existing.getImplements();
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

        public Builder additionalData(Map<String, String> additionalData) {
            this.additionalData = assertNotNull(additionalData);
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.put(key, value);
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

        public Builder directives(List<Directive> directives) {
            this.directives = directives;
            return this;
        }

        public Builder directive(Directive directive) {
            this.directives = ImmutableList.<Directive>builder()
                    .addAll(directives)
                    .add(directive)
                    .build();
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

        public ObjectTypeDefinitionWithTransformation build() {
            return new ObjectTypeDefinitionWithTransformation(typeMappingDefinition,
                    name,
                    implementz,
                    directives,
                    fieldDefinitions,
                    description,
                    sourceLocation,
                    comments,
                    ignoredChars,
                    additionalData);
        }
    }
}
