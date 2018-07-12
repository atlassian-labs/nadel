package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.Definition;
import graphql.language.Node;
import graphql.language.NodeBuilder;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

public class ServiceDefinition extends AbstractNode<ServiceDefinition> {

    private final String name;

    private List<Definition> typeDefinitions;

    private ServiceDefinition(String name, List<Definition> definitions, SourceLocation sourceLocation, List<Comment> comments) {
        super(sourceLocation, comments);
        this.name = name;
        this.typeDefinitions = new ArrayList<>();
        this.typeDefinitions = definitions;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(typeDefinitions);
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public ServiceDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }

    public String getName() {
        return name;
    }

    public List<Definition> getTypeDefinitions() {
        return new ArrayList<>(typeDefinitions);
    }

    public static Builder newServiceDefinition() {
        return new Builder();
    }

    public static class Builder implements NodeBuilder {

        private List<Comment> comments = new ArrayList<>();
        private SourceLocation sourceLocation;
        private String name;
        private List<Definition> definitions = new ArrayList<>();

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder definitions(List<Definition> definitions) {
            this.definitions = definitions;
            return this;
        }


        public ServiceDefinition build() {
            return new ServiceDefinition(name, definitions, sourceLocation, comments);

        }

    }


}
