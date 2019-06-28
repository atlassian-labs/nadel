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
import graphql.nadel.engine.ResolvedValueMapper;
import graphql.nadel.engine.HydrationInputNode;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static graphql.Assert.assertNotEmpty;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.geFirstLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.mapChildren;
import static graphql.util.TreeTransformerUtil.changeNode;

public class HydrationTransformation extends FieldTransformation {


    private InnerServiceHydration innerServiceHydration;

    private ResolvedValueMapper resolvedValueMapper = new ResolvedValueMapper();
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

        Field newField = FieldUtils.pathToFields(hydrationSourceName, getFieldId(), true);
        changeNode(context, newField);
        return TraversalControl.ABORT;
    }

    public InnerServiceHydration getInnerServiceHydration() {
        return innerServiceHydration;
    }

    @Override
    public UnapplyResult unapplyResultNode(ExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {

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
        LeafExecutionResultNode changedNode = unapplyLeafNode(transformedNode.getExecutionStepInfo(), leafNode, allTransformations, environment);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }

    private UnapplyResult handleListOfObjects(ListExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        // if we have more merged fields than transformations
        // we handle here a list of objects with each object containing one node

        ExecutionStepInfo mappedEsi = mapToOriginalFields(transformedNode.getExecutionStepInfo(), allTransformations, environment);

        ExecutionResultNode changedNode = mapChildren(transformedNode, objectChild -> {
            environment.parentExecutionStepInfo = mappedEsi;
            LeafExecutionResultNode leaf = (LeafExecutionResultNode) objectChild.getChildren().get(0);
            return unapplyLeafNode(objectChild.getExecutionStepInfo(), leaf, allTransformations, environment);
        });

        changedNode = changedNode.withNewExecutionStepInfo(mappedEsi);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private UnapplyResult handleListOfLeafs(ListExecutionResultNode listExecutionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        ExecutionStepInfo mappedEsi = mapToOriginalFields(listExecutionResultNode.getExecutionStepInfo(), allTransformations, environment);


        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode leafNode : listExecutionResultNode.getChildren()) {
            environment.parentExecutionStepInfo = mappedEsi;
            LeafExecutionResultNode newChild = unapplyLeafNode(leafNode.getExecutionStepInfo(), (LeafExecutionResultNode) leafNode, allTransformations, environment);
            newChildren.add(newChild);
        }
        ExecutionResultNode changedNode = listExecutionResultNode.withNewExecutionStepInfo(mappedEsi).withNewChildren(newChildren);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private LeafExecutionResultNode unapplyLeafNode(ExecutionStepInfo correctESI, LeafExecutionResultNode leafNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        ExecutionStepInfo leafESI = leafNode.getExecutionStepInfo();

        // we need to build a correct ESI based on the leaf node and the current node for the overall schema
        correctESI = correctESI.transform(builder -> builder.type(leafESI.getType()));

        correctESI = mapToOriginalFields(correctESI, allTransformations, environment);
        if (leafNode.getResolvedValue().isNullValue()) {
            // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
            return changeFieldInResultNode(leafNode, getOriginalField());
        } else {
            return new HydrationInputNode(this, correctESI, leafNode.getResolvedValue(), null);
        }
    }

    private ExecutionStepInfo mapToOriginalFields(ExecutionStepInfo esi,
                                                  List<FieldTransformation> allTransformations,
                                                  UnapplyEnvironment environment) {
        esi = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi, environment.parentExecutionStepInfo);
        return executionStepInfoMapper.mapExecutionStepInfo(esi, environment);
    }
}
