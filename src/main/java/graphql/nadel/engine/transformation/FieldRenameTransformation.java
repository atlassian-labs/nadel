package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.FetchedValueAnalysisMapper;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;
import java.util.function.BiFunction;

import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.getLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.pathToFields;
import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends FieldTransformation {

    FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        List<String> path = mappingDefinition.getInputPath();
        if (path.size() == 1) {
            Field changedNode = environment.getField().transform(t -> t
                    .name(mappingDefinition.getInputPath().get(0))
                    .additionalData(NADEL_FIELD_ID, getFieldId()));
            return changeNode(environment.getTraverserContext(), changedNode);
        }

        Field finalCurField = pathToFields(path, getFieldId());
        changeNode(environment.getTraverserContext(), finalCurField);
        // skip traversing subtree because the fields are in respect to the underlying schema and not the overall which will break
        return TraversalControl.ABORT;
    }


    @Override
    public TraversalControl unapplyResultNode(ExecutionResultNode executionResultNode,
                                              List<FieldTransformation> allTransformations,
                                              UnapplyEnvironment environment) {
        List<String> path = mappingDefinition.getInputPath();
        if (path.size() == 1) {
            FetchedValueAnalysis fetchedValueAnalysis = executionResultNode.getFetchedValueAnalysis();

            BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> {
                ExecutionStepInfo esiWithMappedField = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi);
                return executionStepInfoMapper.mapExecutionStepInfo(esiWithMappedField, environment);
            };
            FetchedValueAnalysis mappedFVA = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(fetchedValueAnalysis, environment,
                    esiMapper);
            environment.unapplyNode.accept(executionResultNode.withNewFetchedValueAnalysis(mappedFVA));
            return TraversalControl.CONTINUE;
        } else {
            LeafExecutionResultNode leafExecutionResultNode = getLeafNode(executionResultNode);
            // path and type is still wrong here
            LeafExecutionResultNode leafNode = changeFieldInResultNode(leafExecutionResultNode, getOriginalField());
            changeNode(environment.context, leafNode);
            return TraversalControl.ABORT;
        }
    }


}
