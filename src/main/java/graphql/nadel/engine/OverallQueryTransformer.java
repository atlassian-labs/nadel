package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Argument;
import graphql.language.AstNodeAdapter;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FieldDefinition;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.ObjectField;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.nadel.Operation;
import graphql.nadel.Service;
import graphql.nadel.dsl.ExtendedFieldDefinition;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.engine.transformation.ApplyEnvironment;
import graphql.nadel.engine.transformation.ApplyResult;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.engine.transformation.OverallTypeInfo;
import graphql.nadel.engine.transformation.OverallTypeInformation;
import graphql.nadel.engine.transformation.RecordOverallTypeInformation;
import graphql.nadel.engine.transformation.RemovedFieldData;
import graphql.nadel.hooks.NewVariableValue;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.schema.idl.TypeInfo;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformer;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.nadel.engine.NodeTypeContext.newNodeTypeContext;
import static graphql.nadel.util.Util.getTypeMappingDefinitionFor;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.util.FpKit.groupingByUniqueKey;
import static graphql.util.FpKit.map;
import static graphql.util.TreeTransformerUtil.changeNode;

public class OverallQueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(OverallQueryTransformer.class);

    private final ValuesResolver valuesResolver = new ValuesResolver();
    private final RecordOverallTypeInformation recordOverallTypeInformation = new RecordOverallTypeInformation();

    private final Map<String, List<RemovedFieldData>> removedFieldMap = new HashMap<>();

    public Map<String, List<RemovedFieldData>> getRemovedFieldMap() {
        return removedFieldMap;
    }

    QueryTransformationResult transformHydratedTopLevelField(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName,
            Operation operation,
            Field topLevelField,
            GraphQLCompositeType topLevelFieldTypeOverall,
            ServiceExecutionHooks serviceExecutionHooks,
            Service service,
            Object serviceContext,
            ExecutionStepInfo hydratedFieldStepInfo) {
        long startTime = System.currentTimeMillis();
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
        Map<String, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();
        Map<String, Object> variableValues = new LinkedHashMap<>(executionContext.getVariables());

        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        SelectionSet topLevelFieldSelectionSet = transformNode(
                executionContext,
                underlyingSchema,
                topLevelField.getSelectionSet(),
                topLevelFieldTypeOverall,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext,
                hydratedFieldStepInfo);

        Field transformedTopLevelField = topLevelField.transform(builder -> builder.selectionSet(topLevelFieldSelectionSet));

        transformedTopLevelField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, transformedTopLevelField, topLevelFieldTypeOverall);

        List<VariableDefinition> variableDefinitions = buildReferencedVariableDefinitions(referencedVariables, executionContext.getGraphQLSchema(), typeRenameMappings);
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        Map<String, FragmentDefinition> transformedFragments = transformFragments(executionContext,
                underlyingSchema,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext,
                hydratedFieldStepInfo);

        SelectionSet newOperationSelectionSet = newSelectionSet().selection(transformedTopLevelField).build();
        OperationDefinition operationDefinition = newOperationDefinition()
                .name(operationName)
                .operation(operation.getAstOperation())
                .selectionSet(newOperationSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Document newDocument = newDocument(operationDefinition, transformedFragments);

        MergedField transformedMergedField = MergedField.newMergedField(transformedTopLevelField).build();
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformHydratedTopLevelField time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                Collections.singletonList(transformedMergedField),
                typeRenameMappings,
                referencedVariableNames,
                transformationByResultField,
                transformedFragments,
                variableValues);

    }

    QueryTransformationResult transformMergedFields(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName, Operation operation,
            List<MergedField> mergedFields,
            ServiceExecutionHooks serviceExecutionHooks,
            Service service,
            Object serviceContext,
            ExecutionStepInfo esi) {
        long startTime = System.currentTimeMillis();
        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        Set<String> fragmentsDirectlyReferenced = new LinkedHashSet<>();
        Map<String, FieldTransformation> transformationByResultField = new LinkedHashMap<>();
        Map<String, String> typeRenameMappings = new LinkedHashMap<>();
        Map<String, VariableDefinition> referencedVariables = new LinkedHashMap<>();
        Map<String, Object> variableValues = new LinkedHashMap<>(executionContext.getVariables());

        List<MergedField> transformedMergedFields = new ArrayList<>();
        List<Field> transformedFields = new ArrayList<>();

        for (MergedField mergedField : mergedFields) {
            List<Field> fields = mergedField.getFields();

            List<Field> transformed = map(fields, field -> {
                GraphQLObjectType rootType = operation.getRootType(executionContext.getGraphQLSchema());
                Field newField = transformNode(
                        executionContext,
                        underlyingSchema,
                        field,
                        rootType,
                        transformationByResultField,
                        typeRenameMappings,
                        fragmentsDirectlyReferenced,
                        referencedVariables,
                        nadelContext,
                        serviceExecutionHooks,
                        variableValues,
                        service,
                        serviceContext, esi);
                // Case happens when the high level field is removed
                if (newField == null) {
                    newField = field.transform(builder -> builder.selectionSet(SelectionSet.newSelectionSet().build()));
                }
                // if all child fields of the high level field are removed then the top-level field is nulled

                GraphQLOutputType fieldType = rootType.getFieldDefinition(field.getName()).getType();
                newField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, newField, fieldType);
                return newField;
            });
            transformedFields.addAll(transformed);
            MergedField transformedMergedField = MergedField.newMergedField(transformed).build();
            transformedMergedFields.add(transformedMergedField);

        }
        List<VariableDefinition> variableDefinitions = buildReferencedVariableDefinitions(referencedVariables, executionContext.getGraphQLSchema(), typeRenameMappings);
        List<String> referencedVariableNames = new ArrayList<>(referencedVariables.keySet());

        // create a new Document including referenced Fragments
        SelectionSet newSelectionSet = newSelectionSet(transformedFields).build();

        OperationDefinition operationDefinition = newOperationDefinition()
                .operation(operation.getAstOperation())
                .name(operationName)
                .selectionSet(newSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Map<String, FragmentDefinition> transformedFragments = transformFragments(
                executionContext,
                underlyingSchema,
                executionContext.getFragmentsByName(),
                transformationByResultField,
                typeRenameMappings,
                fragmentsDirectlyReferenced,
                referencedVariables,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext, esi);

        Document newDocument = newDocument(operationDefinition, transformedFragments);

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("OverallQueryTransformer.transformMergedFields time: {}, executionId: {}", elapsedTime, executionContext.getExecutionId());
        return new QueryTransformationResult(
                newDocument,
                operationDefinition,
                transformedMergedFields,
                typeRenameMappings, referencedVariableNames,
                transformationByResultField,
                transformedFragments,
                variableValues);
    }

    private Document newDocument(OperationDefinition operationDefinition, Map<String, FragmentDefinition> transformedFragments) {
        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (FragmentDefinition transformedFragment : transformedFragments.values()) {
            newDocumentBuilder.definition(transformedFragment);
        }
        return newDocumentBuilder.build();
    }


    private Map<String, FragmentDefinition> transformFragments(ExecutionContext executionContext,
                                                               GraphQLSchema underlyingSchema,
                                                               Map<String, FragmentDefinition> fragments,
                                                               Map<String, FieldTransformation> transformationByResultField,
                                                               Map<String, String> typeRenameMappings,
                                                               Set<String> referencedFragmentNames,
                                                               Map<String, VariableDefinition> referencedVariables,
                                                               ServiceExecutionHooks serviceExecutionHooks,
                                                               Map<String, Object> variableValues,
                                                               Service service,
                                                               Object serviceContext, ExecutionStepInfo esi) {

        Set<String> fragmentsToTransform = new LinkedHashSet<>(referencedFragmentNames);
        List<FragmentDefinition> transformedFragments = new ArrayList<>();
        while (!fragmentsToTransform.isEmpty()) {
            String fragmentName = fragmentsToTransform.iterator().next();
            Set<String> newReferencedFragments = new LinkedHashSet<>();
            FragmentDefinition transformedFragment = transformFragmentDefinition(
                    executionContext,
                    underlyingSchema,
                    fragments.get(fragmentName),
                    transformationByResultField,
                    typeRenameMappings,
                    newReferencedFragments,
                    referencedVariables,
                    serviceExecutionHooks,
                    variableValues,
                    service,
                    serviceContext,
                    esi);
            transformedFragments.add(transformedFragment);
            fragmentsToTransform.addAll(newReferencedFragments);
            fragmentsToTransform.remove(fragmentName);
        }
        return groupingByUniqueKey(transformedFragments, FragmentDefinition::getName);
    }

    private FragmentDefinition transformFragmentDefinition(ExecutionContext executionContext,
                                                           GraphQLSchema underlyingSchema,
                                                           FragmentDefinition fragmentDefinitionWithoutTypeInfo,
                                                           Map<String, FieldTransformation> transformationByResultField,
                                                           Map<String, String> typeRenameMappings,
                                                           Set<String> referencedFragmentNames,
                                                           Map<String, VariableDefinition> referencedVariables,
                                                           ServiceExecutionHooks serviceExecutionHooks,
                                                           Map<String, Object> variableValues,
                                                           Service service,
                                                           Object serviceContext, ExecutionStepInfo esi) {
        NadelContext nadelContext = (NadelContext) executionContext.getContext();

        OverallTypeInformation<FragmentDefinition> overallTypeInformation = recordOverallTypeInformation.recordOverallTypes(
                fragmentDefinitionWithoutTypeInfo,
                executionContext.getGraphQLSchema(),
                null);


        Transformer transformer = new Transformer(
                executionContext,
                underlyingSchema,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                overallTypeInformation,
                variableValues,
                service,
                serviceContext,
                esi);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(NodeTypeContext.class, newNodeTypeContext().build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(fragmentDefinitionWithoutTypeInfo, new TraverserVisitorStub<Node>() {
                    @Override
                    public TraversalControl enter(TraverserContext<Node> context) {
                        return context.thisNode().accept(context, transformer);
                    }
                },
                rootVars
        );
        //noinspection unchecked
        return (FragmentDefinition) newNode;
    }

    private List<VariableDefinition> buildReferencedVariableDefinitions(Map<String, VariableDefinition> referencedVariables,
                                                                        GraphQLSchema graphQLSchema,
                                                                        Map<String, String> typeRenameMappings) {
        List<VariableDefinition> variableDefinitions = new ArrayList<>();
        for (VariableDefinition vd : referencedVariables.values()) {
            TypeInfo typeInfo = TypeInfo.typeInfo(vd.getType());

            GraphQLType type = graphQLSchema.getType(typeInfo.getName());
            TypeMappingDefinition mappingDefinition = getTypeMappingDefinitionFor(type);
            if (mappingDefinition != null) {
                typeRenameMappings.put(mappingDefinition.getUnderlyingName(), mappingDefinition.getOverallName());
                String newName = mappingDefinition.getUnderlyingName();
                TypeInfo newTypeInfo = typeInfo.renameAs(newName);
                vd = vd.transform(builder -> builder.type(newTypeInfo.getRawType()));
            }
            variableDefinitions.add(vd);
        }
        return variableDefinitions;
    }

    private <T extends Node> T transformNode(ExecutionContext executionContext,
                                             GraphQLSchema underlyingSchema,
                                             T nodeWithoutTypeInfo,
                                             GraphQLCompositeType parentTypeOverall,
                                             Map<String, FieldTransformation> transformationByResultField,
                                             Map<String, String> typeRenameMappings,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables,
                                             NadelContext nadelContext,
                                             ServiceExecutionHooks serviceExecutionHooks,
                                             Map<String, Object> variableValues,
                                             Service service,
                                             Object serviceContext, ExecutionStepInfo esi) {
        OverallTypeInformation<T> overallTypeInformation = recordOverallTypeInformation.recordOverallTypes(
                nodeWithoutTypeInfo,
                executionContext.getGraphQLSchema(),
                parentTypeOverall);


        Transformer transformer = new Transformer(executionContext,
                underlyingSchema,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                overallTypeInformation,
                variableValues,
                service,
                serviceContext, esi);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        String underlyingParentName = getUnderlyingTypeNameAndRecordMapping(parentTypeOverall, typeRenameMappings);
        GraphQLOutputType underlyingSchemaParent = (GraphQLOutputType) underlyingSchema.getType(underlyingParentName);
        rootVars.put(NodeTypeContext.class, newNodeTypeContext()
                .outputTypeUnderlying(underlyingSchemaParent)
                .build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(nodeWithoutTypeInfo, new TraverserVisitorStub<Node>() {
                    @Override
                    public TraversalControl enter(TraverserContext<Node> context) {
                        return context.thisNode().accept(context, transformer);
                    }
                },
                rootVars
        );

        //noinspection unchecked
        return (T) newNode;
    }

    private String getUnderlyingTypeNameAndRecordMapping(GraphQLCompositeType typeOverall, Map<String, String> typeRenameMappings) {
        TypeMappingDefinition mappingDefinition = getTypeMappingDefinitionFor(typeOverall);
        if (mappingDefinition == null) {
            return typeOverall.getName();
        }
        typeRenameMappings.put(mappingDefinition.getUnderlyingName(), mappingDefinition.getOverallName());
        return mappingDefinition.getUnderlyingName();
    }

    private class Transformer extends NodeVisitorStub {

        final ExecutionContext executionContext;
        final GraphQLSchema underlyingSchema;
        final Map<String, FieldTransformation> transformationByResultField;
        final Map<String, String> typeRenameMappings;
        final Set<String> referencedFragmentNames;
        final Map<String, VariableDefinition> referencedVariables;
        final NadelContext nadelContext;
        private final Map<String, VariableDefinition> variableDefinitions;
        final ServiceExecutionHooks serviceExecutionHooks;
        private OverallTypeInformation<?> overallTypeInformation;
        private Service service;
        private Object serviceContext;
        private Map<String, Object> variableValues;
        private ExecutionStepInfo esi;

        Transformer(ExecutionContext executionContext,
                    GraphQLSchema underlyingSchema,
                    Map<String, FieldTransformation> transformationByResultField,
                    Map<String, String> typeRenameMappings,
                    Set<String> referencedFragmentNames,
                    Map<String, VariableDefinition> referencedVariables,
                    NadelContext nadelContext,
                    ServiceExecutionHooks serviceExecutionHooks,
                    OverallTypeInformation overallTypeInformation,
                    Map<String, Object> variableValues,
                    Service service,
                    Object serviceContext, ExecutionStepInfo esi) {
            this.executionContext = executionContext;
            this.underlyingSchema = underlyingSchema;
            this.transformationByResultField = transformationByResultField;
            this.typeRenameMappings = typeRenameMappings;
            this.referencedFragmentNames = referencedFragmentNames;
            this.referencedVariables = referencedVariables;
            this.nadelContext = nadelContext;
            this.serviceExecutionHooks = serviceExecutionHooks;
            this.overallTypeInformation = overallTypeInformation;
            OperationDefinition operationDefinition = executionContext.getOperationDefinition();
            this.variableDefinitions = FpKit.getByName(operationDefinition.getVariableDefinitions(), VariableDefinition::getName);
            this.variableValues = variableValues;
            this.service = service;
            this.serviceContext = serviceContext;
            this.esi = esi;
        }

        @Override
        public TraversalControl visitVariableReference(VariableReference variableReference, TraverserContext<Node> context) {
            VariableDefinition variableDefinition = variableDefinitions.get(variableReference.getName());
            referencedVariables.put(variableDefinition.getName(), variableDefinition);
            return super.visitVariableReference(variableReference, context);
        }

        @Override
        public TraversalControl visitObjectField(ObjectField node, TraverserContext<Node> context) {

            NodeTypeContext nodeTypeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLUnmodifiedType unmodifiedType = unwrapAll(nodeTypeContext.getInputValueDefinitionUnderlying().getType());
            //
            // technically a scalar type can have an AST object field - eg field( arg : Json) -> field(arg : { ast : "here" })
            if (unmodifiedType instanceof GraphQLInputObjectType) {
                GraphQLInputObjectType inputObjectType = (GraphQLInputObjectType) unmodifiedType;
                GraphQLInputObjectField inputObjectTypeField = inputObjectType.getField(node.getName());
                nodeTypeContext = nodeTypeContext.transform(builder -> builder.inputValueDefinitionUnderlying(inputObjectTypeField));
                context.setVar(NodeTypeContext.class, nodeTypeContext);
            }
            return TraversalControl.CONTINUE;
        }

        @Override
        public TraversalControl visitArgument(Argument argument, TraverserContext<Node> context) {

            NodeTypeContext nodeTypeContext = context.getVarFromParents(NodeTypeContext.class);

            GraphQLFieldDefinition fieldDefinition = nodeTypeContext.getFieldDefinitionUnderlying();
            GraphQLArgument graphQLArgument = fieldDefinition.getArgument(argument.getName());
            String argumentName = graphQLArgument.getName();
            Object argumentValue = nodeTypeContext.getFieldArgumentValues().getOrDefault(argumentName, null);

            NodeTypeContext newContext = nodeTypeContext.transform(builder -> builder
                    .argumentValue(argumentValue)
                    .argumentDefinitionUnderlying(graphQLArgument)
                    .inputValueDefinitionUnderlying(graphQLArgument));
            context.setVar(NodeTypeContext.class, newContext);
            return TraversalControl.CONTINUE;
        }

        @Override
        protected TraversalControl visitValue(Value<?> value, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLInputValueDefinition inputValueDefinition = typeContext.getInputValueDefinitionUnderlying();

            OverallTypeInfo overallTypeInfo = overallTypeInformation.getOverallTypeInfo(getId(value));

            HooksVisitArgumentValueEnvironmentImpl hooksVisitArgumentValueEnvironment = new HooksVisitArgumentValueEnvironmentImpl(
                    inputValueDefinition,
                    overallTypeInfo.getGraphQLInputValueDefinition(),
                    typeContext.getArgumentDefinitionUnderlying(),
                    overallTypeInfo.getGraphQLArgument(),
                    context,
                    value,
                    variableValues,
                    service,
                    serviceContext);

            NewVariableValue newVariableValue = serviceExecutionHooks.visitArgumentValueInQuery(hooksVisitArgumentValueEnvironment);
            if (newVariableValue != null) {
                variableValues.put(newVariableValue.getName(), newVariableValue.getValue());
            }

            return TraversalControl.CONTINUE;
        }


        @Override
        public TraversalControl visitField(Field field, TraverserContext<Node> context) {

            if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
                return TraversalControl.CONTINUE;
            }

            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            OverallTypeInfo overallTypeInfo = overallTypeInformation.getOverallTypeInfo(getId(field));
            if (overallTypeInfo == null) {
                // this means we have a new field which was added by a transformation and we don't have overall type info about it
                updateTypeContext(context, typeContext.getOutputTypeUnderlying());
                return TraversalControl.CONTINUE;
            }
            GraphQLFieldDefinition fieldDefinitionOverall = overallTypeInfo.getFieldDefinition();
            GraphQLNamedOutputType fieldTypeOverall = (GraphQLNamedOutputType) GraphQLTypeUtil.unwrapAll(fieldDefinitionOverall.getType());

            Optional<Node> parentNode = context.getParentNodes().stream().filter(t -> t instanceof Field).findFirst();
            Field parentField = esi.getField().getSingleField();

            String id = null;
            if (parentNode.isPresent()) {
                id = getId(parentNode.get());
            } else if (parentField != null) {
                // When current field is hydrated and parentNode is not accessible
                id = getId(parentField);
            }

            Optional<GraphQLError> isFieldAllowed = serviceExecutionHooks.isFieldAllowed(field, fieldDefinitionOverall, nadelContext.getUserSuppliedContext());
            if ((isFieldAllowed.isPresent() && id != null)) {
                GraphQLObjectType fieldContainer = null;
                if (esi != null && unwrapAll(esi.getType()) instanceof GraphQLObjectType) {
                    fieldContainer = (GraphQLObjectType) unwrapAll(esi.getType());
                }

                addFieldToRemovedMap(field, fieldDefinitionOverall.getType(), fieldContainer, isFieldAllowed.get(), id);
                return TreeTransformerUtil.deleteNode(context);
            }

            extractAndRecordTypeMappingDefinition(fieldTypeOverall.getName());
            FieldTransformation transformation = createTransformation(fieldDefinitionOverall);
            if (transformation != null) {
                //
                // major side effect alert - we are relying on transformation to call TreeTransformerUtil.changeNode
                // inside itself here
                //
                ApplyEnvironment applyEnvironment = createApplyEnvironment(field, context, overallTypeInfo);
                ApplyResult applyResult = transformation.apply(applyEnvironment);
                Field changedField = (Field) applyEnvironment.getTraverserContext().thisNode();


                String fieldId = FieldMetadataUtil.getUniqueRootFieldId(changedField);
                transformationByResultField.put(fieldId, transformation);

                if (transformation instanceof FieldRenameTransformation) {
                    maybeAddUnderscoreTypeName(context, changedField, fieldTypeOverall);
                }
                if (applyResult.getTraversalControl() == TraversalControl.CONTINUE) {
                    updateTypeContext(context, typeContext.getOutputTypeUnderlying());
                }
                return applyResult.getTraversalControl();

            } else {
                maybeAddUnderscoreTypeName(context, field, fieldTypeOverall);
            }
            updateTypeContext(context, typeContext.getOutputTypeUnderlying());
            return TraversalControl.CONTINUE;
        }


        private void updateTypeContext(TraverserContext<Node> context, GraphQLOutputType currentOutputTypeUnderlying) {
            Field newField = (Field) context.thisNode();
            GraphQLFieldsContainer fieldsContainerUnderlying = (GraphQLFieldsContainer) unwrapAll(currentOutputTypeUnderlying);
            GraphQLFieldDefinition fieldDefinitionUnderlying = Introspection.getFieldDef(underlyingSchema, fieldsContainerUnderlying, newField.getName());
            GraphQLOutputType newOutputTypeUnderlying = fieldDefinitionUnderlying.getType();


            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(underlyingSchema.getCodeRegistry(), fieldDefinitionUnderlying.getArguments(), newField.getArguments(), executionContext.getVariables());
            NodeTypeContext.Builder newTypeContext = newNodeTypeContext()
                    .field(newField)
                    .outputTypeUnderlying(newOutputTypeUnderlying)
                    .fieldsContainerUnderlying(fieldsContainerUnderlying)
                    .fieldDefinitionUnderlying(fieldDefinitionUnderlying)
                    .fieldArgumentValues(argumentValues);
            context.setVar(NodeTypeContext.class, newTypeContext.build());

        }

        ApplyEnvironment createApplyEnvironment(Field field, TraverserContext<Node> context, OverallTypeInfo overallTypeInfo) {
            return new ApplyEnvironment(field, overallTypeInfo.getFieldDefinition(), overallTypeInfo.getFieldsContainer(), context);
        }


        private void maybeAddUnderscoreTypeName(TraverserContext<Node> traverserContext, Field field, GraphQLOutputType fieldType) {
            Field changedNode = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, field, fieldType);
            if (changedNode != field) {
                TreeTransformerUtil.changeNode(traverserContext, changedNode);
            }
        }


        @Override
        public TraversalControl visitInlineFragment(InlineFragment inlineFragment, TraverserContext<Node> context) {
            // inline fragments are allowed not have type conditions, if so the parent type counts

            TypeName typeCondition = inlineFragment.getTypeCondition();
            if (typeCondition == null) {
                return TraversalControl.CONTINUE;
            }

            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeCondition);
            String underlyingTypeName = typeCondition.getName();
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                InlineFragment changedFragment = inlineFragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
                    f.typeCondition(newTypeName);
                });
                underlyingTypeName = typeMappingDefinition.getUnderlyingName();
                changeNode(context, changedFragment);
            }
            updateTypeContextForInlineFragment(underlyingTypeName, context);
            //TODO: what if all fields inside inline fragment get deleted? we should recheck it on LEAVING the node
            //(after transformations are applied); So we can see what happened. Alternative would be  to do second pass
            return TraversalControl.CONTINUE;
        }


        private void updateTypeContextForInlineFragment(String underlyingType, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLCompositeType fragmentConditionUnderlying = (GraphQLCompositeType) underlyingSchema.getType(underlyingType);
            context.setVar(NodeTypeContext.class, typeContext.transform(builder -> builder
                    .outputTypeUnderlying(fragmentConditionUnderlying)));
        }

        @Override
        public TraversalControl visitFragmentDefinition(FragmentDefinition fragment, TraverserContext<Node> context) {
            TypeName typeName = fragment.getTypeCondition();
            TypeMappingDefinition typeMappingDefinition = typeTransformationForFragment(executionContext, typeName);
            String underlyingTypeName = typeName.getName();
            if (typeMappingDefinition != null) {
                recordTypeRename(typeMappingDefinition);
                FragmentDefinition changedFragment = fragment.transform(f -> {
                    TypeName newTypeName = newTypeName(typeMappingDefinition.getUnderlyingName()).build();
                    f.typeCondition(newTypeName);
                });
                underlyingTypeName = typeMappingDefinition.getUnderlyingName();
                changeNode(context, changedFragment);
            }

            updateTypeContextForFragmentDefinition(fragment, underlyingTypeName, context);
            return TraversalControl.CONTINUE;
        }

        private void updateTypeContextForFragmentDefinition(FragmentDefinition fragmentDefinition, String underlyingTypeName, TraverserContext<Node> context) {
            NodeTypeContext typeContext = context.getVarFromParents(NodeTypeContext.class);
            GraphQLCompositeType fragmentConditionUnderlying = (GraphQLCompositeType) underlyingSchema.getType(underlyingTypeName);
            context.setVar(NodeTypeContext.class, typeContext.transform(builder -> builder
                    .outputTypeUnderlying(fragmentConditionUnderlying)));
        }


        @Override
        public TraversalControl visitFragmentSpread(FragmentSpread fragmentSpread, TraverserContext<Node> context) {
            referencedFragmentNames.add(fragmentSpread.getName());
            return TraversalControl.CONTINUE;
        }

        private TypeMappingDefinition recordTypeRename(TypeMappingDefinition typeMappingDefinition) {
            if (typeMappingDefinition != null) {
                typeRenameMappings.put(typeMappingDefinition.getUnderlyingName(), typeMappingDefinition.getOverallName());
            }
            return typeMappingDefinition;
        }

        @SuppressWarnings("ConstantConditions")
        private TypeMappingDefinition typeTransformationForFragment(ExecutionContext executionContext, TypeName typeNameOverall) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeNameOverall.getName());
            assertTrue(type instanceof GraphQLFieldsContainer, "Expected type '%s' to be an field container type", typeNameOverall);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }


        @SuppressWarnings("ConstantConditions")
        private TypeMappingDefinition extractAndRecordTypeMappingDefinition(String typeNameOverall) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeNameOverall);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        private TypeMappingDefinition extractAndRecordTypeMappingDefinition(GraphQLSchema graphQLSchema, GraphQLType type) {

            TypeMappingDefinition typeMappingDefinition = getTypeMappingDefinitionFor(type);
            recordTypeRename(typeMappingDefinition);

            if (type instanceof GraphQLInterfaceType) {
                GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) type;

                graphQLSchema.getImplementations(interfaceType).forEach(objectType -> {
                    extractAndRecordTypeMappingDefinition(graphQLSchema, objectType);
                });
            }
            if (type instanceof GraphQLUnionType) {
                GraphQLUnionType unionType = (GraphQLUnionType) type;
                unionType.getTypes().forEach(typeMember -> {
                    extractAndRecordTypeMappingDefinition(graphQLSchema, typeMember);
                });
            }
            return typeMappingDefinition;
        }
    }

    private void addFieldToRemovedMap(Field field, GraphQLOutputType type, GraphQLObjectType fieldContainer, GraphQLError graphQLError, String id) {
        RemovedFieldData removedFieldData = new RemovedFieldData(field, type, fieldContainer, graphQLError);
        removedFieldMap.computeIfAbsent(id, k -> new ArrayList<>()).add(removedFieldData);
    }


    private graphql.nadel.dsl.FieldTransformation transformationDefinitionForField(FieldDefinition definition) {
        if (definition instanceof ExtendedFieldDefinition) {
            return ((ExtendedFieldDefinition) definition).getFieldTransformation();
        }
        return null;
    }

    private FieldTransformation createTransformation(GraphQLFieldDefinition fieldDefinitionOverallSchema) {
        graphql.nadel.dsl.FieldTransformation definition = transformationDefinitionForField(fieldDefinitionOverallSchema.getDefinition());

        if (definition == null) {
            return null;
        }
        if (definition.getFieldMappingDefinition() != null) {
            return new FieldRenameTransformation(definition.getFieldMappingDefinition());
        } else if (definition.getUnderlyingServiceHydration() != null) {
            return new HydrationTransformation(definition.getUnderlyingServiceHydration());
        } else {
            return assertShouldNeverHappen();
        }
    }

}
