package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
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

public class HydrationTransformation extends AbstractFieldTransformation {


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
        return TreeTransformerUtil.changeNode(context, newField);
    }

    public InnerServiceHydration getInnerServiceHydration() {
        return innerServiceHydration;
    }

    @Override
    public ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        // we only care about leaf results here, even if the LIST nodes also references HydrationTransformation because
        // LIST share the field with all children
        if (!(executionResultNode instanceof LeafExecutionResultNode)) {
            return null;
        }

//        LeafExecutionResultNode leafExecutionResultNode = getLeafNode(executionResultNode);
        FetchedValueAnalysis fetchedValueAnalysis = executionResultNode.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> {
            ExecutionStepInfo esiWithMappedField = replaceFieldsWithOriginalFields(allTransformations, esi);
            return executionStepInfoMapper.mapExecutionStepInfo(esiWithMappedField, environment);
        };
        FetchedValueAnalysis mappedFVA = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(fetchedValueAnalysis, environment,
                esiMapper);

        if (fetchedValueAnalysis.isNullValue()) {
            // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
            return changeFieldInResultNode(executionResultNode, getOriginalField());
        } else {
            return new HydrationInputNode(this, mappedFVA, null);
        }
    }
}
