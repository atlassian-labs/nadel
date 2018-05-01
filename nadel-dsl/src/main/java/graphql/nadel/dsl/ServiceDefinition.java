package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Directive;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.TypeDefinition;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

public class ServiceDefinition extends AbstractNode<ServiceDefinition> {
    private final String name;
    private final String url;
    private final List<Directive> directives;

    private List<TypeDefinition<?>> typeDefinitions;

    public ServiceDefinition(String name, String url, List<Directive> directives) {
        this.name = name;
        this.url = url;
        this.directives = directives;
        this.typeDefinitions = new ArrayList<>();
    }

    public ServiceDefinition(String name, String url) {
        this(name, url, new ArrayList<>());
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

    public String getUrl() {
        return url;
    }


    public List<TypeDefinition<?>> getTypeDefinitions() {
        return typeDefinitions;
    }
}
