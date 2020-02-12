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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;

public class StitchingDsl extends AbstractNode<StitchingDsl> {


    private final List<ServiceDefinition> serviceDefinitions;
    private final CommonDefinition commonDefinition;

    private StitchingDsl(List<ServiceDefinition> serviceDefinitions,
                         CommonDefinition commonDefinition, SourceLocation sourceLocation,
                         List<Comment> comments,
                         IgnoredChars ignoredChars,
                         Map<String, String> additionalData) {
        super(sourceLocation, comments, ignoredChars, additionalData);
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
        private Map<String, String> additionalData = new LinkedHashMap<>();

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

        public Builder additionalData(Map<String, String> additionalData) {
            this.additionalData = assertNotNull(additionalData);
            return this;
        }

        public Builder additionalData(String key, String value) {
            this.additionalData.put(key, value);
            return this;
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
            return new StitchingDsl(serviceDefinitions, commonDefinition, sourceLocation, comments, ignoredChars, additionalData);
        }

    }


}
