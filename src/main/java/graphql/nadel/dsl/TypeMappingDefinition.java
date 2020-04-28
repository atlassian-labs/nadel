package graphql.nadel.dsl;

import graphql.Internal;
import graphql.language.AbstractNode;
import graphql.language.Comment;
import graphql.language.IgnoredChars;
import graphql.language.Node;
import graphql.language.NodeChildrenContainer;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.List;
import java.util.Map;

@Internal
public class TypeMappingDefinition extends AbstractNode<TypeMappingDefinition> {

    private String underlyingName;
    private String overallName;


    public TypeMappingDefinition(SourceLocation sourceLocation, List<Comment> comments, Map<String, String> additionalData) {
        super(sourceLocation, comments, IgnoredChars.EMPTY, additionalData);
    }

    public String getUnderlyingName() {
        return underlyingName;
    }

    public void setUnderlyingName(String underlyingName) {
        this.underlyingName = underlyingName;
    }

    public String getOverallName() {
        return overallName;
    }

    public void setOverallName(String overallName) {
        this.overallName = overallName;
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
    public TypeMappingDefinition withNewChildren(NodeChildrenContainer newChildren) {
        return null;
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public TypeMappingDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
