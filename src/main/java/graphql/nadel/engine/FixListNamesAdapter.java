package graphql.nadel.engine;

import graphql.Assert;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
import graphql.util.NodeAdapter;
import graphql.util.NodeLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FixListNamesAdapter implements NodeAdapter<ExecutionResultNode> {

    public static final FixListNamesAdapter FIX_NAMES_ADAPTER = new FixListNamesAdapter();

    private FixListNamesAdapter() {

    }

    @Override
    public Map<String, List<ExecutionResultNode>> getNamedChildren(ExecutionResultNode node) {
        Map<String, List<ExecutionResultNode>> result = new LinkedHashMap<>();
        result.put(null, node.getChildren());
        return result;
    }

    @Override
    public ExecutionResultNode withNewChildren(ExecutionResultNode node, Map<String, List<ExecutionResultNode>> newChildren) {
        Assert.assertTrue(newChildren.size() == 1);
        List<ExecutionResultNode> childrenList = newChildren.get(null);
        Assert.assertNotNull(childrenList);

        if (node instanceof ListExecutionResultNode) {
            node = fixFieldName((ListExecutionResultNode) node, childrenList.get(0));
        }
        return node.withNewChildren(childrenList);
    }

    private ExecutionResultNode fixFieldName(ListExecutionResultNode node, ExecutionResultNode childNode) {
        return StrategyUtil.changeFieldInResultNode(node, childNode.getMergedField());
    }

    @Override
    public ExecutionResultNode removeChild(ExecutionResultNode node, NodeLocation location) {
        throw new UnsupportedOperationException();
    }
}

