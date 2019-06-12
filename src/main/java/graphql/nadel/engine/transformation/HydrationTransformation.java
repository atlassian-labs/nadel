package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.language.AbstractNode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.geFirstLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.mapChildren;
import static graphql.util.TreeTransformerUtil.changeNode;

public class HydrationTransformation extends FieldTransformation {


    private InnerServiceHydration innerServiceHydration;

    private FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();


    public HydrationTransformation(InnerServiceHydration innerServiceHydration) {
        this.innerServiceHydration = innerServiceHydration;
    }

    @Override
    public AbstractNode getDefinition() {
        return innerServiceHydration;
    }

    @Override
    public TraversalControl apply(ApplyEnvironment environment) {
        super.apply(environment);

        TraverserContext<Node> context = environment.getTraverserContext();
        List<RemoteArgumentDefinition> arguments = innerServiceHydration.getArguments();
        assertTrue(arguments.size() == 1, "only hydration with one argument are supported");
        RemoteArgumentDefinition remoteArgumentDefinition = arguments.get(0);
        RemoteArgumentSource remoteArgumentSource = remoteArgumentDefinition.getRemoteArgumentSource();
        assertTrue(remoteArgumentSource.getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD,
                "only object field arguments are supported at the moment");
        List<String> hydrationSourceName = remoteArgumentSource.getPath();

        Field newField = FieldUtils.pathToFields(hydrationSourceName, getFieldId());
        changeNode(context, newField);
        return TraversalControl.ABORT;
    }

    public InnerServiceHydration getInnerServiceHydration() {
        return innerServiceHydration;
    }

    @Override
    public UnapplyResult unapplyResultNode(ExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {

        // TODO: handle merged fields by extracting nodes handling transformed field and nodes which are not transformed


        // we can have a list of hydration inputs. E.g.: $source.userIds (this is a list of leafs)
        // or we can have a list of things inside the path: e.g.: $source.issues.userIds (this is a list of objects)
        if (transformedNode instanceof ListExecutionResultNode) {
            ExecutionResultNode child = transformedNode.getChildren().get(0);
            if (child instanceof LeafExecutionResultNode) {
                return handleListOfLeafs((ListExecutionResultNode) transformedNode, allTransformations, environment);
            } else if (child instanceof ObjectExecutionResultNode) {
                return handleListOfObjects((ListExecutionResultNode) transformedNode, allTransformations, environment);
            } else {
                return assertShouldNeverHappen("Not implemented yet");
            }
        }

        LeafExecutionResultNode leafNode = geFirstLeafNode(transformedNode);
        LeafExecutionResultNode changedNode = unapplyLeafNode(transformedNode.getFetchedValueAnalysis().getExecutionStepInfo(), leafNode, allTransformations, environment);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }

    private UnapplyResult handleListOfObjects(ListExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        // if we have more merged fields than transformations
        // we handle here a list of objects with each object containing one node
        FetchedValueAnalysis fetchedValueAnalysis = transformedNode.getFetchedValueAnalysis();

        FetchedValueAnalysis mappedFVA = mapToOriginalFields(fetchedValueAnalysis, allTransformations, environment);

        ExecutionResultNode changedNode = mapChildren(transformedNode, objectChild -> {
            environment.parentExecutionStepInfo = mappedFVA.getExecutionStepInfo();
            LeafExecutionResultNode leaf = (LeafExecutionResultNode) objectChild.getChildren().get(0);
            return unapplyLeafNode(objectChild.getFetchedValueAnalysis().getExecutionStepInfo(), leaf, allTransformations, environment);
        });

        changedNode = changedNode.withNewFetchedValueAnalysis(mappedFVA);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private UnapplyResult handleListOfLeafs(ListExecutionResultNode listExecutionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        FetchedValueAnalysis fetchedValueAnalysis = listExecutionResultNode.getFetchedValueAnalysis();
        FetchedValueAnalysis mappedFVA = mapToOriginalFields(fetchedValueAnalysis, allTransformations, environment);


        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode leafNode : listExecutionResultNode.getChildren()) {
            environment.parentExecutionStepInfo = mappedFVA.getExecutionStepInfo();
            LeafExecutionResultNode newChild = unapplyLeafNode(leafNode.getFetchedValueAnalysis().getExecutionStepInfo(), (LeafExecutionResultNode) leafNode, allTransformations, environment);
            newChildren.add(newChild);
        }
        ExecutionResultNode changedNode = listExecutionResultNode.withNewFetchedValueAnalysis(mappedFVA).withNewChildren(newChildren);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private LeafExecutionResultNode unapplyLeafNode(ExecutionStepInfo correctESI, LeafExecutionResultNode leafNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        FetchedValueAnalysis leafFetchedValueAnalysis = leafNode.getFetchedValueAnalysis();
        ExecutionStepInfo leafESI = leafFetchedValueAnalysis.getExecutionStepInfo();

        // we need to build a correct ESI based on the leaf node and the current node for the overall schema
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
            ExecutionStepInfo esiWithMappedField = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi);
            return executionStepInfoMapper.mapExecutionStepInfo(esiWithMappedField, environment);
        };
        return fetchedValueAnalysisMapper.mapFetchedValueAnalysis(fetchedValueAnalysis, environment,
                esiMapper);
    }
}
