package graphql.nadel.engine;

import graphql.execution.ExecutionContext;
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
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.engine.transformation.ApplyEnvironment;
import graphql.nadel.engine.transformation.ApplyResult;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.FieldTypeInfo;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.engine.transformation.OverallTypeInformation;
import graphql.nadel.engine.transformation.RecordOverallTypeInformation;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.language.OperationDefinition.newOperationDefinition;
import static graphql.language.SelectionSet.newSelectionSet;
import static graphql.language.TypeName.newTypeName;
import static graphql.nadel.engine.NodeTypeContext.newNodeTypeContext;
import static graphql.nadel.util.FpKit.toMapCollector;
import static graphql.nadel.util.Util.getTypeMappingDefinitionFor;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static graphql.util.FpKit.map;
import static graphql.util.TreeTransformerUtil.changeNode;
import static java.util.function.Function.identity;

public class OverallQueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(OverallQueryTransformer.class);

    private final ValuesResolver valuesResolver = new ValuesResolver();
    private final RecordOverallTypeInformation recordOverallTypeInformation = new RecordOverallTypeInformation();

    QueryTransformationResult transformHydratedTopLevelField(
            ExecutionContext executionContext,
            GraphQLSchema underlyingSchema,
            String operationName,
            Operation operation,
            Field topLevelField,
            GraphQLCompositeType topLevelFieldType,
            ServiceExecutionHooks serviceExecutionHooks,
            Service service,
            Object serviceContext
    ) {
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
                topLevelFieldType,
                transformationByResultField,
                typeRenameMappings,
                referencedFragmentNames,
                referencedVariables,
                nadelContext,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext);

        Field transformedTopLevelField = topLevelField.transform(builder -> builder.selectionSet(topLevelFieldSelectionSet));

        transformedTopLevelField = ArtificialFieldUtils.maybeAddUnderscoreTypeName(nadelContext, transformedTopLevelField, topLevelFieldType);

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
                serviceContext);

        SelectionSet newOperationSelectionSet = newSelectionSet().selection(transformedTopLevelField).build();
        OperationDefinition operationDefinition = newOperationDefinition()
                .name(operationName)
                .operation(operation.getAstOperation())
                .selectionSet(newOperationSelectionSet)
                .variableDefinitions(variableDefinitions)
                .build();

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (String referencedFragmentName : referencedFragmentNames) {
            FragmentDefinition fragmentDefinition = transformedFragments.get(referencedFragmentName);
            newDocumentBuilder.definition(fragmentDefinition);
        }
        Document newDocument = newDocumentBuilder.build();

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
            Object serviceContext
    ) {
        long startTime = System.currentTimeMillis();
        NadelContext nadelContext = (NadelContext) executionContext.getContext();
        Set<String> referencedFragmentNames = new LinkedHashSet<>();
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
                        referencedFragmentNames,
                        referencedVariables,
                        nadelContext,
                        serviceExecutionHooks,
                        variableValues,
                        service,
                        serviceContext);

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
                referencedFragmentNames,
                referencedVariables,
                serviceExecutionHooks,
                variableValues,
                service,
                serviceContext);

        Document.Builder newDocumentBuilder = Document.newDocument();
        newDocumentBuilder.definition(operationDefinition);
        for (String referencedFragmentName : referencedFragmentNames) {
            FragmentDefinition fragmentDefinition = transformedFragments.get(referencedFragmentName);
            newDocumentBuilder.definition(fragmentDefinition);
        }

        Document newDocument = newDocumentBuilder.build();
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
                                                               Object serviceContext) {
        return fragments.values().stream()
                .map(fragment -> transformFragmentDefinition(
                        executionContext,
                        underlyingSchema,
                        fragment,
                        transformationByResultField,
                        typeRenameMappings,
                        referencedFragmentNames,
                        referencedVariables,
                        serviceExecutionHooks,
                        variableValues,
                        service,
                        serviceContext))
                .collect(toMapCollector(FragmentDefinition::getName, identity()));
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
                                                           Object serviceContext) {
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
                serviceContext);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(NodeTypeContext.class, newNodeTypeContext().build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(overallTypeInformation.getNode(), new TraverserVisitorStub<Node>() {
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
                                             GraphQLCompositeType parentType,
                                             Map<String, FieldTransformation> transformationByResultField,
                                             Map<String, String> typeRenameMappings,
                                             Set<String> referencedFragmentNames,
                                             Map<String, VariableDefinition> referencedVariables,
                                             NadelContext nadelContext,
                                             ServiceExecutionHooks serviceExecutionHooks,
                                             Map<String, Object> variableValues,
                                             Service service,
                                             Object serviceContext) {
        OverallTypeInformation<T> overallTypeInformation = recordOverallTypeInformation.recordOverallTypes
                (nodeWithoutTypeInfo,
                        executionContext.getGraphQLSchema(),
                        parentType);


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
                serviceContext);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        GraphQLOutputType underlyingSchemaParent = (GraphQLOutputType) underlyingSchema.getType(parentType.getName());
        rootVars.put(NodeTypeContext.class, newNodeTypeContext()
                .outputTypeUnderlying(underlyingSchemaParent)
                .build());
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Node newNode = treeTransformer.transform(overallTypeInformation.getNode(), new TraverserVisitorStub<Node>() {
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
                    Object serviceContext) {
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

            HooksVisitArgumentValueEnvironmentImpl hooksVisitArgumentValueEnvironment = new HooksVisitArgumentValueEnvironmentImpl(
                    inputValueDefinition,
                    typeContext.getArgumentDefinitionUnderlying(),
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
            FieldTypeInfo fieldTypeInfo = getFieldTypeInfo(field);
            if (fieldTypeInfo != null) {
                GraphQLFieldDefinition fieldDefinitionOverall = fieldTypeInfo.getFieldDefinition();
                GraphQLNamedOutputType fieldType = (GraphQLNamedOutputType) GraphQLTypeUtil.unwrapAll(fieldDefinitionOverall.getType());

                TypeMappingDefinition typeMappingDefinition = typeTransformation(executionContext, fieldType.getName());
                if (typeMappingDefinition != null) {
                    recordTypeRename(typeMappingDefinition);
                }
                FieldTransformation transformation = createTransformation(fieldDefinitionOverall);
                if (transformation != null) {
                    //
                    // major side effect alert - we are relying on transformation to call TreeTransformerUtil.changeNode
                    // inside itself here
                    //
                    ApplyEnvironment applyEnvironment = createApplyEnvironment(field, context, fieldTypeInfo);
                    ApplyResult applyResult = transformation.apply(applyEnvironment);
                    Field changedField = (Field) applyEnvironment.getTraverserContext().thisNode();


                    String fieldId = FieldMetadataUtil.getUniqueRootFieldId(changedField);
                    transformationByResultField.put(fieldId, transformation);

                    if (transformation instanceof FieldRenameTransformation) {
                        maybeAddUnderscoreTypeName(context, changedField, fieldType);
                    }
                    if (applyResult.getTraversalControl() == TraversalControl.CONTINUE) {
                        updateTypeContext(context, typeContext.getOutputTypeUnderlying());
                    }
                    return applyResult.getTraversalControl();

                } else {
                    maybeAddUnderscoreTypeName(context, field, fieldType);
                }
            }

            updateTypeContext(context, typeContext.getOutputTypeUnderlying());
            return TraversalControl.CONTINUE;
        }

        private FieldTypeInfo getFieldTypeInfo(Field field) {
            String id = FieldMetadataUtil.getOverallTypeInfoId(field);
            return overallTypeInformation.getFieldInfoById().get(id);
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

        ApplyEnvironment createApplyEnvironment(Field field, TraverserContext<Node> context, FieldTypeInfo fieldTypeInfo) {
            return new ApplyEnvironment(field, fieldTypeInfo.getFieldDefinition(), fieldTypeInfo.getFieldsContainer(), context);
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
        private TypeMappingDefinition typeTransformationForFragment(ExecutionContext executionContext, TypeName typeName) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeName.getName());
            assertTrue(type instanceof GraphQLFieldsContainer, "Expected type '%s' to be an field container type", typeName);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }


        @SuppressWarnings("ConstantConditions")
        private TypeMappingDefinition typeTransformation(ExecutionContext executionContext, String typeName) {
            GraphQLType type = executionContext.getGraphQLSchema().getType(typeName);
            return extractAndRecordTypeMappingDefinition(executionContext.getGraphQLSchema(), type);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        private TypeMappingDefinition extractAndRecordTypeMappingDefinition(GraphQLSchema graphQLSchema, GraphQLType type) {

            TypeMappingDefinition typeMappingDefinition = getTypeMappingDefinitionFor(type);
            recordTypeRename(typeMappingDefinition);

            if (type instanceof GraphQLInterfaceType) {
                GraphQLInterfaceType interfaceType = (GraphQLInterfaceType) type;

                graphQLSchema.getImplementations(interfaceType).forEach(objectType -> {
                    TypeMappingDefinition definition = extractAndRecordTypeMappingDefinition(graphQLSchema, objectType);
                    recordTypeRename(definition);
                });
            }
            if (type instanceof GraphQLUnionType) {
                GraphQLUnionType unionType = (GraphQLUnionType) type;
                unionType.getTypes().forEach(typeMember -> {
                    TypeMappingDefinition definition = extractAndRecordTypeMappingDefinition(graphQLSchema, typeMember);
                    recordTypeRename(definition);
                });
            }
            return typeMappingDefinition;
        }
    }


    private graphql.nadel.dsl.FieldTransformation transformationDefinitionForField(FieldDefinition definition) {
        if (definition instanceof FieldDefinitionWithTransformation) {
            return ((FieldDefinitionWithTransformation) definition).getFieldTransformation();
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
