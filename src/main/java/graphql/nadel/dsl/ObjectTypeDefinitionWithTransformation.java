package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.NodeBuilder;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SourceLocation;
import graphql.language.Type;

import java.util.ArrayList;
import java.util.List;

public class ObjectTypeDefinitionWithTransformation extends ObjectTypeDefinition {

    private final TypeTransformation typeTransformation;

    protected ObjectTypeDefinitionWithTransformation(String name,
                                                     List<Type> implementz,
                                                     List<Directive> directives,
                                                     List<FieldDefinition> fieldDefinitions,
                                                     Description description,
                                                     SourceLocation sourceLocation,
                                                     List<Comment> comments,
                                                     TypeTransformation typeTransformation) {
        super(name, implementz, directives, fieldDefinitions, description, sourceLocation, comments);
        this.typeTransformation = typeTransformation;
    }

    public TypeTransformation getTypeTransformation() {
        return typeTransformation;
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
        private TypeTransformation typeTransformation;

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

        public Builder typeTransformation(TypeTransformation typeTransformation) {
            this.typeTransformation = typeTransformation;
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

        public ObjectTypeDefinitionWithTransformation build() {
            ObjectTypeDefinitionWithTransformation objectTypeDefinition = new ObjectTypeDefinitionWithTransformation(name,
                    implementz,
                    directives,
                    fieldDefinitions,
                    description,
                    sourceLocation,
                    comments,
                    typeTransformation);
            return objectTypeDefinition;
        }
    }
}
