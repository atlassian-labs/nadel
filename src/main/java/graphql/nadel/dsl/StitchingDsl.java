package graphql.nadel.dsl;


import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.IgnoredChars;
import graphql.language.Node;
import graphql.language.NodeBuilder;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StitchingDsl extends AbstractNode<StitchingDsl> {


    private final List<ServiceDefinition> serviceDefinitions;
    private final CommonDefinition commonDefinition;

    private StitchingDsl(List<ServiceDefinition> serviceDefinitions,
                         CommonDefinition commonDefinition, SourceLocation sourceLocation,
                         List<Comment> comments,
                         IgnoredChars ignoredChars) {
        super(sourceLocation, comments, ignoredChars);
        this.serviceDefinitions = serviceDefinitions;
        this.commonDefinition = commonDefinition;
    }


    public List<ServiceDefinition> getServiceDefinitions() {
        return new ArrayList<>(serviceDefinitions);
    }

    public CommonDefinition getCommonDefinition() {
        return commonDefinition;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(serviceDefinitions);
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return null;
    }

    @Override
    public StitchingDsl withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }


    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public StitchingDsl deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }


    public static Builder newStitchingDSL() {
        return new Builder();
    }

    public static class Builder implements NodeBuilder {

        private List<Comment> comments = new ArrayList<>();
        private SourceLocation sourceLocation;
        private List<ServiceDefinition> serviceDefinitions = new ArrayList<>();
        private CommonDefinition commonDefinition;
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

        public Builder serviceDefinitions(List<ServiceDefinition> serviceDefinitions) {
            this.serviceDefinitions = serviceDefinitions;
            return this;
        }

        public Builder commonDefinition(CommonDefinition commonDefinition) {
            this.commonDefinition = commonDefinition;
            return this;
        }


        public StitchingDsl build() {
            return new StitchingDsl(serviceDefinitions, commonDefinition, sourceLocation, comments, ignoredChars);
        }

    }


}
