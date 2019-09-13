package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;

import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.FieldMetadataUtil.addFieldMetadata;
import static graphql.nadel.engine.transformation.FieldUtils.addFieldIdToChildren;
import static graphql.nadel.engine.transformation.FieldUtils.getSubTree;
import static graphql.nadel.engine.transformation.FieldUtils.mapChildren;
import static graphql.nadel.engine.transformation.FieldUtils.pathToFields;
import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends FieldTransformation {

    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
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
        Field changedNode = environment.getField().transform(builder -> builder.name(mappingDefinition.getInputPath().get(0)));
        changedNode = addFieldMetadata(changedNode, getFieldId(), true);
        Field fieldWithIds = addFieldIdToChildren(environment.getField(), getFieldId());
        SelectionSet selectionSetWithIds = fieldWithIds.getSelectionSet();
        if (path.size() > 1) {
            Field firstChildField = pathToFields(path.subList(1, path.size()), getFieldId(), false, selectionSetWithIds);
            changedNode = changedNode.transform(builder -> builder.selectionSet(newSelectionSet().selection(firstChildField).build()));
        } else {
            changedNode = changedNode.transform(builder -> builder.selectionSet(selectionSetWithIds));
        }
        changeNode(environment.getTraverserContext(), changedNode);
        return new ApplyResult(TraversalControl.CONTINUE);
    }


    @Override
    public UnapplyResult unapplyResultNode(ExecutionResultNode executionResultNode,
                                           List<FieldTransformation> allTransformations,
                                           UnapplyEnvironment environment) {
        ExecutionResultNode subTree = getSubTree(executionResultNode, mappingDefinition.getInputPath().size() - 1);
        ExecutionStepInfo esi = subTree.getExecutionStepInfo();

        esi = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi, environment.parentExecutionStepInfo);
        esi = executionStepInfoMapper.mapExecutionStepInfo(esi, environment);
        ExecutionResultNode resultNode = subTree.withNewExecutionStepInfo(esi);

        resultNode = replaceFieldsAndTypesInsideList(resultNode, allTransformations, environment);

        return new UnapplyResult(resultNode, TraversalControl.CONTINUE);
    }

    private ExecutionResultNode replaceFieldsAndTypesInsideList(ExecutionResultNode node,
                                                                List<FieldTransformation> allTransformations,
                                                                UnapplyEnvironment environment) {

        if (node instanceof ListExecutionResultNode) {
            return mapChildren(node, child -> {
                ExecutionStepInfo newEsi = replaceFieldsAndTypesWithOriginalValues(allTransformations, child.getExecutionStepInfo(), environment.parentExecutionStepInfo);
                return replaceFieldsAndTypesInsideList(child.withNewExecutionStepInfo(newEsi),
                        allTransformations,
                        environment);
            });
        }
        return node;
    }


}
