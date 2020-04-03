package graphql.nadel.engine.transformation;

import graphql.execution.ExecutionPath;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.UnderlyingServiceHydration;
import graphql.nadel.engine.ExecutionResultNodeMapper;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.nadel.engine.PathMapper;
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
import static graphql.execution.MergedField.newMergedField;
import static graphql.nadel.engine.HydrationInputNode.newHydrationInputNode;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldUtils.geFirstLeafNode;
import static graphql.nadel.engine.transformation.FieldUtils.mapChildren;
import static graphql.nadel.util.FpKit.filter;
import static graphql.util.TreeTransformerUtil.changeNode;

public class HydrationTransformation extends FieldTransformation {


    private UnderlyingServiceHydration underlyingServiceHydration;

    ExecutionResultNodeMapper executionResultNodeMapper = new ExecutionResultNodeMapper();
    PathMapper pathMapper = new PathMapper();


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
    public UnapplyResult unapplyResultNode(ExecutionResultNode node, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {

        /*
         * The goal here is to return a flat list HydrationInputNodes which then can be used to create the hydration query.
         */

        NormalizedQueryField matchingNormalizedOverallField = getMatchingNormalizedQueryFieldBasedOnParent(environment.correctParentTypes);

        // we can have a list of hydration inputs. E.g.: $source.userIds (this is a list of leafs)
        // or we can have a list of things inside the path: e.g.: $source.issues.userIds (this is a list of objects)
        if (node instanceof ListExecutionResultNode) {
            if (node.getChildren().size() == 0) {
                return handleEmptyList((ListExecutionResultNode) node, allTransformations, environment);
            }
            ExecutionResultNode child = node.getChildren().get(0);
            if (child instanceof LeafExecutionResultNode) {
                return handleListOfLeafs((ListExecutionResultNode) node, allTransformations, environment, matchingNormalizedOverallField);
            } else if (child instanceof ObjectExecutionResultNode) {
                return handleListOfObjects((ListExecutionResultNode) node, allTransformations, environment, matchingNormalizedOverallField);
            } else {
                return assertShouldNeverHappen("Not implemented yet");
            }
        }

        LeafExecutionResultNode leafNode = geFirstLeafNode(node);
        LeafExecutionResultNode changedNode = unapplyLeafNode(leafNode, allTransformations, environment, matchingNormalizedOverallField);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }

    private UnapplyResult handleEmptyList(ListExecutionResultNode listNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment) {
        ListExecutionResultNode changedList = listNode.transform(builder -> builder.field(newMergedField(getOriginalField()).build()));
        return new UnapplyResult(changedList, TraversalControl.ABORT);
    }

    private UnapplyResult handleListOfObjects(ListExecutionResultNode transformedNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment, NormalizedQueryField matchingNormalizedField) {
        // if we have more merged fields than transformations
        // we handle here a list of objects with each object containing one node

        // this is ListNode->ObjectNode(with exact one leaf child)->LeafNode

        ExecutionResultNode mappedNode = mapToOverallFieldAndTypes(transformedNode, allTransformations, matchingNormalizedField, environment);

        ExecutionResultNode changedNode = mapChildren(transformedNode, objectChild -> {
            LeafExecutionResultNode leaf = (LeafExecutionResultNode) objectChild.getChildren().get(0);
            return unapplyLeafNode(leaf, allTransformations, environment, matchingNormalizedField);
        });

        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private UnapplyResult handleListOfLeafs(ListExecutionResultNode listExecutionResultNode, List<FieldTransformation> allTransformations, UnapplyEnvironment environment, NormalizedQueryField matchingNormalizedField) {
        ExecutionResultNode mappedNode = mapToOverallFieldAndTypes(listExecutionResultNode, allTransformations, matchingNormalizedField, environment);


        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode leafNode : listExecutionResultNode.getChildren()) {
            LeafExecutionResultNode newChild = unapplyLeafNode((LeafExecutionResultNode) leafNode, allTransformations, environment, matchingNormalizedField);
            newChildren.add(newChild);
        }
        ExecutionResultNode changedNode = mappedNode.withNewChildren(newChildren);
        return new UnapplyResult(changedNode, TraversalControl.ABORT);
    }


    private LeafExecutionResultNode unapplyLeafNode(LeafExecutionResultNode leafNode,
                                                    List<FieldTransformation> allTransformations,
                                                    UnapplyEnvironment environment,
                                                    NormalizedQueryField matchingNormalizedField) {


        leafNode = (LeafExecutionResultNode) mapToOverallFieldAndTypes(leafNode, allTransformations, matchingNormalizedField, environment);
        ExecutionPath executionPath = pathMapper.mapPath(leafNode.getExecutionPath(), leafNode.getField(), environment);
        leafNode = leafNode.transform(builder -> builder.executionPath(executionPath));
        if (leafNode.getResolvedValue().isNullValue()) {
            // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
            return changeFieldInResultNode(leafNode, getOriginalField());
        } else {
            return newHydrationInputNode()
                    .hydrationTransformation(this)
                    .field(leafNode.getField())
                    .objectType(leafNode.getObjectType())
                    .fieldDefinition(leafNode.getFieldDefinition())
                    .executionPath(leafNode.getExecutionPath())
                    .resolvedValue(leafNode.getResolvedValue())
                    .elapsedTime(leafNode.getElapsedTime())
                    .normalizedField(matchingNormalizedField)
                    .build();
        }
    }

}
