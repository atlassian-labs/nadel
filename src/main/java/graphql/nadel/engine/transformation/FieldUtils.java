package graphql.nadel.engine.transformation;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.util.FpKit.map;

public final class FieldUtils {

    /**
     * This returns the aliased result name if a field is alised other its the field name
     *
     * @param field the field in play
     *
     * @return the result name
     */
    public static String resultKeyForField(Field field) {
        return field.getAlias() != null ? field.getAlias() : field.getName();
    }

    public static Field pathToFields(List<String> path,
                                     Field copyFieldMetadataFrom,
                                     String transformationId,
                                     List<String> additionalIds,
                                     boolean firstRootOfTransformation,
                                     Map<String, List<FieldMetadata>> metadataByFieldId) {
        return pathToFields(path, copyFieldMetadataFrom, transformationId, additionalIds, firstRootOfTransformation, null, metadataByFieldId);
    }

    public static Field pathToFields(List<String> path,
                                     Field copyMetadataFrom,
                                     String transformationId,
                                     List<String> additionalIds,
                                     boolean firstRootOfTransformation,
                                     SelectionSet lastSelectionSet,
                                     Map<String, List<FieldMetadata>> metadataByFieldId) {
        Field curField = null;
        for (int ix = path.size() - 1; ix >= 0; ix--) {
            Field.Builder newField = Field.newField();
            String fieldId = "new-field_" + path.get(ix) + "_" + UUID.randomUUID().toString();
            newField.additionalData(NodeId.ID, fieldId);
            FieldMetadataUtil.setFieldMetadata(fieldId, transformationId, additionalIds, ix == 0 && firstRootOfTransformation, metadataByFieldId);
            if (ix == path.size() - 1 && lastSelectionSet != null) {
                newField.selectionSet(lastSelectionSet);
            }
            if (curField != null) {
                newField.selectionSet(newSelectionSet().selection(curField).build());
            }
            newField.name(path.get(ix));
            curField = newField.build();
            FieldMetadataUtil.copyFieldMetadata(copyMetadataFrom, curField, metadataByFieldId);
        }
        return curField;
    }

    public static LeafExecutionResultNode geFirstLeafNode(ExecutionResultNode executionResultNode) {
        ExecutionResultNode curNode = executionResultNode;
        while (curNode instanceof ObjectExecutionResultNode) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child but got %s", curNode.getChildren().size());
            curNode = curNode.getChildren().get(0);
        }
        assertTrue(curNode instanceof LeafExecutionResultNode, "expecting only object results and at the end one leaf");
        return (LeafExecutionResultNode) curNode;
    }


    public static ExecutionResultNode getSubTree(ExecutionResultNode executionResultNode, int levels) {
        ExecutionResultNode curNode = executionResultNode;
        int curLevel = 0;
        while (curNode.getChildren().size() > 0 && curLevel++ < levels) {
            assertTrue(curNode.getChildren().size() == 1, "expecting one child but got %s", curNode.getChildren().size());
            curNode = curNode.getChildren().get(0);
            if (curNode instanceof LeafExecutionResultNode) {
                return curNode;
            }
        }
        return curNode;
    }

    public static void addTransformationIdToChildren(Field field, Map<String, FragmentDefinition> fragmentDefinitionMap, String transformationId, Map<String, List<FieldMetadata>> metadataByFieldId) {
        if (field.getSelectionSet() == null) {
            return;
        }

        Function<? super Node, ? extends List<Node>> getChildren = node -> {
            if (node instanceof FragmentSpread) {
                FragmentDefinition fragmentDefinition = assertNotNull(fragmentDefinitionMap.get(((FragmentSpread) node).getName()));
                List<Node> result = new ArrayList<>();
                result.addAll(node.getChildren());
                result.add(fragmentDefinition);
                return result;
            }
            return node.getChildren();
        };
        NodeTraverser nodeTraverser = new NodeTraverser(Collections.emptyMap(), getChildren);
        nodeTraverser.depthFirst(new NodeVisitorStub() {
            @Override
            public TraversalControl visitField(Field field, TraverserContext<Node> context) {
                FieldMetadataUtil.addFieldMetadata(field, transformationId, false, metadataByFieldId);
                return TraversalControl.CONTINUE;
            }
        }, field.getSelectionSet());

    }


    public static ExecutionResultNode mapChildren(ExecutionResultNode executionResultNode, Function<ExecutionResultNode, ExecutionResultNode> mapper) {
        List<ExecutionResultNode> newChildren = map(executionResultNode.getChildren(), mapper);
        return executionResultNode.withNewChildren(newChildren);
    }

}
