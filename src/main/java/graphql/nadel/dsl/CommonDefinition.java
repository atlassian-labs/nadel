package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.IgnoredChars;
import graphql.language.Node;
import graphql.language.NodeBuilder;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.SDLDefinition;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommonDefinition extends AbstractNode<CommonDefinition> {

    private List<SDLDefinition> typeDefinitions;

    private CommonDefinition(List<SDLDefinition> typeDefinitions, SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        super(sourceLocation, comments, ignoredChars);
        this.typeDefinitions = typeDefinitions;
    }

    @Override
    public List<Node> getChildren() {
        return null;
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return null;
    }

    @Override
    public CommonDefinition withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public CommonDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }

    public List<SDLDefinition> getTypeDefinitions() {
        return new ArrayList<>(typeDefinitions);
    }

    public static Builder newCommonDefinition() {
        return new Builder();
    }

    public static class Builder implements NodeBuilder {

        private List<Comment> comments = new ArrayList<>();
        private SourceLocation sourceLocation;
        private List<SDLDefinition> typeDefinitions = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;

        private Builder() {

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

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder typeDefinitions(List<SDLDefinition> typeDefinitions) {
            this.typeDefinitions.clear();
            this.typeDefinitions.addAll(typeDefinitions);
            return this;
        }

        public Builder addTypeDefinitions(List<SDLDefinition> typeDefinitions) {
            this.typeDefinitions.addAll(typeDefinitions);
            return this;
        }


        public CommonDefinition build() {
            return new CommonDefinition(typeDefinitions, sourceLocation, comments, ignoredChars);
        }
    }
}
