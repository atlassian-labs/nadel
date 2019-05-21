package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.FetchedValueAnalysisMapper;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static graphql.util.TreeTransformerUtil.changeNode;

public class FieldRenameTransformation extends AbstractFieldTransformation {

    FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();
    private final FieldMappingDefinition mappingDefinition;

    public FieldRenameTransformation(FieldMappingDefinition mappingDefinition) {
        this.mappingDefinition = mappingDefinition;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);
        String fieldId = UUID.randomUUID().toString();
        Field changedNode = environment.getField().transform(t -> t.name(mappingDefinition.getInputName()).additionalData(NADEL_FIELD_ID, fieldId));
        return changeNode(environment.getTraverserContext(), changedNode);
    }

    @Override
    public ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode,
                                                 List<FieldTransformation> allTransformations,
                                                 UnapplyEnvironment environment) {

        FetchedValueAnalysis fetchedValueAnalysis = executionResultNode.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> {
            ExecutionStepInfo esiWithMappedField = replaceFieldsWithOriginalFields(allTransformations, esi);
            return executionStepInfoMapper.mapExecutionStepInfo(esiWithMappedField, environment);
        };
        FetchedValueAnalysis mappedFVA = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(fetchedValueAnalysis, environment,
                esiMapper);

        return executionResultNode.withNewFetchedValueAnalysis(mappedFVA);
    }


}
