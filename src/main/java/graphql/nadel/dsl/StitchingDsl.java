package graphql.nadel.dsl;


import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.Node;
import graphql.language.NodeBuilder;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

public class StitchingDsl extends AbstractNode<StitchingDsl> {


    private final List<ServiceDefinition> serviceDefinitions;

    private StitchingDsl(List<ServiceDefinition> serviceDefinitions, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.serviceDefinitions = serviceDefinitions;
    }


    public List<ServiceDefinition> getServiceDefinitions() {
        return new ArrayList<>(serviceDefinitions);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(serviceDefinitions);
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

        private Builder() {

        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
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


        public StitchingDsl build() {
            return new StitchingDsl(serviceDefinitions, sourceLocation, comments);
        }

    }


}
