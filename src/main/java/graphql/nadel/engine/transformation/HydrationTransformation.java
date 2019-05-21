package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.FetchedValueAnalysisMapper;
import graphql.nadel.engine.HydrationInputNode;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TreeTransformerUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.getLeafNode;

public class HydrationTransformation extends FieldTransformation {


    private InnerServiceHydration innerServiceHydration;

    private FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();

    public HydrationTransformation(InnerServiceHydration innerServiceHydration) {
        this.innerServiceHydration = innerServiceHydration;
    }

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        super.apply(environment);

        TraverserContext<Node> context = environment.getTraverserContext();
        List<RemoteArgumentDefinition> arguments = innerServiceHydration.getArguments();
        assertTrue(arguments.size() == 1, "only hydration with one argument are supported");
        RemoteArgumentDefinition remoteArgumentDefinition = arguments.get(0);
        RemoteArgumentSource remoteArgumentSource = remoteArgumentDefinition.getRemoteArgumentSource();
        assertTrue(remoteArgumentSource.getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD,
                "only object field arguments are supported at the moment");
        List<String> hydrationSourceName = remoteArgumentSource.getPath();

        Field newField = FieldUtils.pathToFields(hydrationSourceName);
        String fieldId = UUID.randomUUID().toString();
        newField = newField.transform(builder -> builder.additionalData(NADEL_FIELD_ID, fieldId));
        TreeTransformerUtil.changeNode(context, newField);
        return TraversalControl.ABORT;
    }

    public InnerServiceHydration getInnerServiceHydration() {
        return innerServiceHydration;
    }

    @Override
    public ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        if (executionResultNode instanceof ListExecutionResultNode) {
            FetchedValueAnalysis fetchedValueAnalysis = executionResultNode.getFetchedValueAnalysis();
            FetchedValueAnalysis mappedFVA = mapToOriginalFields(fetchedValueAnalysis, allTransformations, environment);
            return executionResultNode.withNewFetchedValueAnalysis(mappedFVA);
        }

        // we can have collapsed arguments, this is why maybe don't have a leaf node directly
        LeafExecutionResultNode leafNode = getLeafNode(executionResultNode);
        FetchedValueAnalysis leafFetchedValueAnalysis = leafNode.getFetchedValueAnalysis();
        ExecutionStepInfo leafESI = leafFetchedValueAnalysis.getExecutionStepInfo();

        // we need to build a correct ESI based on the leaf node and the current node for the overall schema
        ExecutionStepInfo correctESI = executionResultNode.getFetchedValueAnalysis().getExecutionStepInfo();
        correctESI = correctESI.transform(builder -> builder.type(leafESI.getType()));
        ExecutionStepInfo finalCorrectESI = correctESI;
        leafFetchedValueAnalysis = leafFetchedValueAnalysis.transfrom(builder -> builder.executionStepInfo(finalCorrectESI));

        // we need to use the correct
        FetchedValueAnalysis mappedFVA = mapToOriginalFields(leafFetchedValueAnalysis, allTransformations, environment);
        if (mappedFVA.isNullValue()) {
            // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
            return changeFieldInResultNode(leafNode, getOriginalField());
        } else {
            return new HydrationInputNode(this, mappedFVA, null);
        }
    }

    private FetchedValueAnalysis mapToOriginalFields(FetchedValueAnalysis fetchedValueAnalysis, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> {
            ExecutionStepInfo esiWithMappedField = replaceFieldsWithOriginalFields(allTransformations, esi);
            return executionStepInfoMapper.mapExecutionStepInfo(esiWithMappedField, environment);
        };
        return fetchedValueAnalysisMapper.mapFetchedValueAnalysis(fetchedValueAnalysis, environment,
                esiMapper);
    }
}
