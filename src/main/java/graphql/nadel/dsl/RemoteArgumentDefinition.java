package graphql.nadel.dsl;

import graphql.language.AbstractNode;
import graphql.language.Node;
import graphql.language.NodeVisitor;
import graphql.language.SourceLocation;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class RemoteArgumentDefinition extends AbstractNode<RemoteArgumentDefinition> {

    private final String name;
    private final FieldMappingDefinition fieldMappingDefinition;

    public RemoteArgumentDefinition(String name, FieldMappingDefinition fieldMappingDefinition,
                                    SourceLocation sourceLocation) {
        super(sourceLocation, emptyList());
        this.name = name;
        this.fieldMappingDefinition = fieldMappingDefinition;
    }

    public String getName() {
        return name;
    }

    public FieldMappingDefinition getFieldMappingDefinition() {
        return fieldMappingDefinition;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public boolean isEqualTo(Node node) {
        return false;
    }

    @Override
    public RemoteArgumentDefinition deepCopy() {
        return null;
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return null;
    }
}
