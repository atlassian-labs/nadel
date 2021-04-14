package graphql.nadel.engine.execution.transformation;

import graphql.Internal;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SelectionSet;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.engine.execution.FieldMetadataUtil;
import graphql.nadel.engine.execution.PathMapper;
import graphql.nadel.engine.execution.UnapplyEnvironment;
import graphql.nadel.engine.result.ExecutionResultNode;
import graphql.nadel.engine.result.ListExecutionResultNode;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.util.TraversalControl;

import java.util.List;

import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.nadel.engine.execution.FieldMetadataUtil.addFieldMetadata;
import static graphql.nadel.engine.execution.transformation.FieldUtils.addTransformationIdToChildren;
import static graphql.nadel.engine.execution.transformation.FieldUtils.getSubTree;
import static graphql.nadel.engine.execution.transformation.FieldUtils.mapChildren;
import static graphql.nadel.engine.execution.transformation.FieldUtils.pathToFields;
import static graphql.util.TreeTransformerUtil.changeNode;

@Internal
public class FieldRenameTransformation extends FieldTransformation {

    PathMapper pathMapper = new PathMapper();
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
        List<String> existingIds = FieldMetadataUtil.getTransformationIds(environment.getField(), environment.getMetadataByFieldId());
        Field changedNode = environment.getField().transform(builder -> builder.name(mappingDefinition.getInputPath().get(0)));
        addFieldMetadata(changedNode, getTransformationId(), true, environment.getMetadataByFieldId());
        addTransformationIdToChildren(environment.getField(), environment.getFragmentDefinitionMap(), getTransformationId(), environment.getMetadataByFieldId());
        SelectionSet selectionSetWithIds = changedNode.getSelectionSet();
        if (path.size() > 1) {
            Field firstChildField = pathToFields(path.subList(1, path.size()), environment.getField(), getTransformationId(), existingIds, false, selectionSetWithIds, environment.getMetadataByFieldId());
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

        NormalizedQueryField matchingNormalizedOverallField = getMatchingNormalizedQueryFieldBasedOnParent(environment.correctParentNode);
        // the result tree should be in terms of the overall schema
        ExecutionResultNode resultNode = getSubTree(executionResultNode, mappingDefinition.getInputPath().size() - 1);

        resultNode = mapToOverallFieldAndTypes(resultNode, allTransformations, matchingNormalizedOverallField);
        resultNode = replaceFieldsAndTypesInsideList(resultNode, allTransformations, matchingNormalizedOverallField);
        // the new path is the parent + the original result key
        ResultPath mappedPath = environment.correctParentNode.getResultPath().segment(resultNode.getResultKey());
        resultNode = resultNode.transform(builder -> builder.resultPath(mappedPath));

        return new UnapplyResult(resultNode, TraversalControl.CONTINUE);
    }

    private ExecutionResultNode replaceFieldsAndTypesInsideList(ExecutionResultNode node,
                                                                List<FieldTransformation> allTransformations,
                                                                NormalizedQueryField normalizedQueryField) {

        if (node instanceof ListExecutionResultNode) {
            return mapChildren(node, child -> {
                ExecutionResultNode newChild = mapToOverallFieldAndTypes(child, allTransformations, normalizedQueryField);
                return replaceFieldsAndTypesInsideList(newChild,
                        allTransformations,
                        normalizedQueryField
                );
            });
        }
        return node;
    }


}
