package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.nadel.execution.ExecutionResultNode;
import graphql.nadel.execution.ListExecutionResultNode;
import graphql.util.TraversalControl;

import java.util.List;

import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.FieldMetadataUtil.addFieldMetadata;
import static graphql.nadel.engine.transformation.FieldUtils.addFieldIdToChildren;
import static graphql.nadel.engine.transformation.FieldUtils.getSubTreeMutable;
import static graphql.nadel.engine.transformation.FieldUtils.pathToFields;
import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformationMutable extends FieldTransformationMutable {

    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformationMutable(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }


    @Override
    public FieldMappingDefinition getDefinition() {
        return mappingDefinition;
    }

    @Override
    public ApplyResult apply(ApplyEnvironment environment) {
        setEnvironment(environment);
        List<String> path = mappingDefinition.getInputPath();
        List<String> existingIds = FieldMetadataUtil.getFieldIds(environment.getField());
        Field changedNode = environment.getField().transform(builder -> builder.name(mappingDefinition.getInputPath().get(0)));
        changedNode = addFieldMetadata(changedNode, getFieldId(), true);
        Field fieldWithIds = addFieldIdToChildren(environment.getField(), getFieldId());
        SelectionSet selectionSetWithIds = fieldWithIds.getSelectionSet();
        if (path.size() > 1) {
            Field firstChildField = pathToFields(path.subList(1, path.size()), getFieldId(), existingIds, false, selectionSetWithIds);
            changedNode = changedNode.transform(builder -> builder.selectionSet(newSelectionSet().selection(firstChildField).build()));
        } else {
            changedNode = changedNode.transform(builder -> builder.selectionSet(selectionSetWithIds));
        }
        changeNode(environment.getTraverserContext(), changedNode);
        return new ApplyResult(TraversalControl.CONTINUE);
    }


    @Override
    public UnapplyResultMutable unapplyResultNode(ExecutionResultNode executionResultNode,
                                                  List<FieldTransformationMutable> allTransformations,
                                                  UnapplyEnvironment environment) {

        ExecutionResultNode subTree = getSubTreeMutable(executionResultNode, mappingDefinition.getInputPath().size() - 1);
        ExecutionStepInfo esi = subTree.getExecutionStepInfo();

        esi = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi, environment.parentExecutionStepInfo);
        esi = executionStepInfoMapper.mapExecutionStepInfo(esi, environment);
        ExecutionResultNode resultNode = subTree.withNewExecutionStepInfo(esi);

        replaceFieldsAndTypesInsideList(resultNode, allTransformations, environment);

        return new UnapplyResultMutable(resultNode, TraversalControl.CONTINUE);
    }

    private void replaceFieldsAndTypesInsideList(ExecutionResultNode node,
                                                 List<FieldTransformationMutable> allTransformations,
                                                 UnapplyEnvironment environment) {

        if (node instanceof ListExecutionResultNode) {
            node.getChildren().forEach(child -> {
                ExecutionStepInfo newEsi = replaceFieldsAndTypesWithOriginalValues(allTransformations, child.getExecutionStepInfo(), environment.parentExecutionStepInfo);
                node.setExecutionStepInfo(newEsi);
                replaceFieldsAndTypesInsideList(node,
                        allTransformations,
                        environment);

            });
        }
    }


}
