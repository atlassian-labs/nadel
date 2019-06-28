package graphql.nadel.dsl;

import graphql.language.Comment;
import graphql.language.Description;
import graphql.language.Directive;
import graphql.language.IgnoredChars;
import graphql.language.NodeBuilder;
import graphql.language.SourceLocation;
import graphql.language.Type;
import graphql.language.UnionTypeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnionTypeDefinitionWithTransformation extends UnionTypeDefinition {

    private final TypeMappingDefinition typeMappingDefinition;

    protected UnionTypeDefinitionWithTransformation(TypeMappingDefinition typeMappingDefinition,
                                                    String name,
                                                    List<Directive> directives,
                                                    List<Type> memberTypes,
                                                    Description description,
                                                    SourceLocation sourceLocation,
                                                    List<Comment> comments,
                                                    IgnoredChars ignoredChars) {
        super(name, directives, memberTypes, description, sourceLocation, comments, ignoredChars, Collections.emptyMap());
        this.typeMappingDefinition = typeMappingDefinition;
    }

    public TypeMappingDefinition getTypeMappingDefinition() {
        return typeMappingDefinition;
    }

    public static UnionTypeDefinitionWithTransformation.Builder newUnionTypeDefinitionWithTransformation(UnionTypeDefinition copyFrom) {
        return new UnionTypeDefinitionWithTransformation.Builder(copyFrom);
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private String name;
        private Description description;
        private List<Directive> directives;
        private List<Type> memberTypes;
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;

        private TypeMappingDefinition typeMappingDefinition;

        private Builder() {
        }

        private Builder(UnionTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.description = existing.getDescription();
            this.directives = existing.getDirectives();
            this.memberTypes = existing.getMemberTypes();
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

        @Override
        public NodeBuilder additionalData(Map<String, String> additionalData) {
            return null;
        }

        @Override
        public NodeBuilder additionalData(String key, String value) {
            return null;
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

        public Builder memberTypes(List<Type> memberTypes) {
            this.memberTypes = memberTypes;
            return this;
        }

        public Builder memberTypes(Type memberType) {
            this.memberTypes.add(memberType);
            return this;
        }

        public UnionTypeDefinitionWithTransformation build() {
            return new UnionTypeDefinitionWithTransformation(
                    typeMappingDefinition,
                    name,
                    directives,
                    memberTypes,
                    description,
                    sourceLocation,
                    comments,
                    ignoredChars
            );
        }
    }
}
