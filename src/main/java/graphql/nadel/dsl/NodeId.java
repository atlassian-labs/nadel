package graphql.nadel.dsl;

import graphql.execution.MergedField;
import graphql.language.Node;
import graphql.util.FpKit;

import java.util.Collections;
import java.util.List;

import static graphql.Assert.assertNotNull;

public class NodeId {
    /**
     * Every AST node is given an id as additional data
     */
    public static final String ID = "id";

    public static String getId(Node<?> node) {
        return assertNotNull(node.getAdditionalData().get(ID), "expected node %s to have an id", node);
    }

    public static List<String> getIds(Node<?> node) {
        return Collections.singletonList(getId(node));
    }

    public static List<String> getIds(List<? extends Node<?>> nodes) {
        return FpKit.map(nodes, NodeId::getId);
    }

    public static List<String> getIds(MergedField mergedField) {
        return FpKit.map(mergedField.getFields(), NodeId::getId);
    }
}
