package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionStepInfo;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.UnderlyingServiceHydration;
import graphql.nadel.engine.ExecutionStepInfoMapper;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.nadel.engine.HydrationInputNode;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.geFirstLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.mapChildren;
import static graphql.nadel.util.FpKit.filter;
import static graphql.util.TreeTransformerUtil.changeNode;

public class HydrationTransformation extends FieldTransformation {


    private UnderlyingServiceHydration underlyingServiceHydration;

    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();


    public HydrationTransformation(UnderlyingServiceHydration underlyingServiceHydration) {
        this.underlyingServiceHydration = underlyingServiceHydration;
    }

    @Override
    public AbstractNode getDefinition() {
        return underlyingServiceHydration;
    }

    @Override
    public ApplyResult apply(ApplyEnvironment environment) {
        setEnvironment(environment);

        TraverserContext<Node> context = environment.getTraverserContext();
        List<RemoteArgumentDefinition> arguments = underlyingServiceHydration.getArguments();

        List<RemoteArgumentDefinition> sourceValues = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.OBJECT_FIELD);
        assertTrue(sourceValues.size() == 1, "exactly one object field source expected");
        List<RemoteArgumentDefinition> argumentValues = filter(arguments, argument -> argument.getRemoteArgumentSource().getSourceType() == RemoteArgumentSource.SourceType.FIELD_ARGUMENT);
        assertTrue(1 + argumentValues.size() == arguments.size(), "only $source and $argument values for arguments are supported");

        RemoteArgumentSource remoteArgumentSource = sourceValues.get(0).getRemoteArgumentSource();
        List<String> hydrationSourceName = remoteArgumentSource.getPath();

        Field newField = FieldUtils.pathToFields(hydrationSourceName, getFieldId(), Collections.emptyList(), true, environment.getMetadataByFieldId());

        // Add back the IDs from the original field e.g. an ID tied to a rename transformation
        FieldMetadataUtil.copyFieldMetadata(environment.getField(), newField, environment.getMetadataByFieldId());

        changeNode(context, newField);
        return new ApplyResult(TraversalControl.ABORT);
    }

    public UnderlyingServiceHydration getUnderlyingServiceHydration() {
        return underlyingServiceHydration;
    }

    @Override
    public UnapplyResult unapplyResultNode(ExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {

        NormalizedQueryField matchingNormalizedField = getMatchingNormalizedQueryFieldBasedOnParent(environment.parentExecutionStepInfo);

        // we can have a list of hydration inputs. E.g.: $source.userIds (this is a list of leafs)
        // or we can have a list of things inside the path: e.g.: $source.issues.userIds (this is a list of objects)
        if (transformedNode instanceof ListExecutionResultNode) {
            if (transformedNode.getChildren().size() == 0) {
                return handleEmptyList((ListExecutionResultNode) transformedNode, allTransformations, environment);
            }
            ExecutionResultNode child = transformedNode.getChildren().get(0);
            if (child instanceof LeafExecutionResultNode) {
                return handleListOfLeafs((ListExecutionResultNode) transformedNode, allTransformations, environment, matchingNormalizedField);
            } else if (child instanceof ObjectExecutionResultNode) {
                return handleListOfObjects((ListExecutionResultNode) transformedNode, allTransformations, environment, matchingNormalizedField);
            } else {
                return assertShouldNeverHappen("Not implemented yet");
            }
        }

        LeafExecutionResultNode leafNode = geFirstLeafNode(transformedNode);
        LeafExecutionResultNode changedNode = unapplyLeafNode(transformedNode.getExecutionStepInfo(), leafNode, allTransformations, environment, matchingNormalizedField);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }

    private UnapplyResult handleEmptyList(ListExecutionResultNode listNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        ListExecutionResultNode changedList = changeFieldInResultNode(listNode, getOriginalField());
        return new UnapplyResult(changedList, TraversalControl.ABORT);
    }

    private UnapplyResult handleListOfObjects(ListExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment, NormalizedQueryField matchingNormalizedField) {
        // if we have more merged fields than transformations
        // we handle here a list of objects with each object containing one node

        ExecutionStepInfo mappedEsi = mapToOriginalFields(transformedNode.getExecutionStepInfo(), allTransformations, environment);

        ExecutionResultNode changedNode = mapChildren(transformedNode, objectChild -> {
            environment.parentExecutionStepInfo = mappedEsi;
            LeafExecutionResultNode leaf = (LeafExecutionResultNode) objectChild.getChildren().get(0);
            return unapplyLeafNode(objectChild.getExecutionStepInfo(), leaf, allTransformations, environment, matchingNormalizedField);
        });

        changedNode = changedNode.withNewExecutionStepInfo(mappedEsi);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private UnapplyResult handleListOfLeafs(ListExecutionResultNode listExecutionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment, NormalizedQueryField matchingNormalizedField) {
        ExecutionStepInfo mappedEsi = mapToOriginalFields(listExecutionResultNode.getExecutionStepInfo(), allTransformations, environment);


        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode leafNode : listExecutionResultNode.getChildren()) {
            environment.parentExecutionStepInfo = mappedEsi;
            LeafExecutionResultNode newChild = unapplyLeafNode(leafNode.getExecutionStepInfo(), (LeafExecutionResultNode) leafNode, allTransformations, environment, matchingNormalizedField);
            newChildren.add(newChild);
        }
        ExecutionResultNode changedNode = listExecutionResultNode.withNewExecutionStepInfo(mappedEsi).withNewChildren(newChildren);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private LeafExecutionResultNode unapplyLeafNode(ExecutionStepInfo correctESI,
                                                    LeafExecutionResultNode leafNode,
                                                    List<FieldTransformation> allTransformations,
                                                    UnapplyEnvironment environment,
                                                    NormalizedQueryField matchingNormalizedField) {
        ExecutionStepInfo leafESI = leafNode.getExecutionStepInfo();

        // we need to build a correct ESI based on the leaf node and the current node for the overall schema
        correctESI = correctESI.transform(builder -> builder.type(leafESI.getType()));

        correctESI = mapToOriginalFields(correctESI, allTransformations, environment);
        if (leafNode.getResolvedValue().isNullValue()) {
            // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
            return changeFieldInResultNode(leafNode, getOriginalField());
        } else {
            return new HydrationInputNode(this,
                    correctESI,
                    leafNode.getResolvedValue(),
                    null,
                    Collections.emptyList(),
                    leafNode.getElapsedTime(),
                    matchingNormalizedField);
        }
    }

    private ExecutionStepInfo mapToOriginalFields(ExecutionStepInfo esi,
                                                  List<FieldTransformation> allTransformations,
                                                  UnapplyEnvironment environment) {
        esi = replaceFieldsAndTypesWithOriginalValues(allTransformations, esi, null);
        return executionStepInfoMapper.mapExecutionStepInfo(esi, environment);
    }
}
