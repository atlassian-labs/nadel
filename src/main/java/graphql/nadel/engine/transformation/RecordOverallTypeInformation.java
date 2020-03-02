package graphql.nadel.engine.transformation;

import graphql.language.Argument;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.language.ObjectField;
import graphql.language.Value;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputValueDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

/**
 * Traverses a query and saves type information while doing it.
 */
public class RecordOverallTypeInformation {


    public <T extends Node> OverallTypeInformation<T> recordOverallTypes(T node, GraphQLSchema graphQLSchema, GraphQLOutputType rootOutputType) {

        Map<String, OverallTypeInfo> overallTypeInfoMap = new LinkedHashMap<>();

        NodeVisitorStub recordTypeInfos = new NodeVisitorStub() {

            @Override
            public TraversalControl visitArgument(Argument argument, TraverserContext<Node> context) {
                GraphQLFieldDefinition fieldDefinition = context.getVarFromParents(GraphQLFieldDefinition.class);
                GraphQLArgument graphQLArgument = fieldDefinition.getArgument(argument.getName());
                context.setVar(GraphQLArgument.class, graphQLArgument);
                context.setVar(GraphQLInputValueDefinition.class, graphQLArgument);
                OverallTypeInfo overallTypeInfo = new OverallTypeInfo(null, fieldDefinition, graphQLArgument, null);
                overallTypeInfoMap.put(getId(argument), overallTypeInfo);
                return TraversalControl.CONTINUE;
            }

            @Override
            public TraversalControl visitObjectField(ObjectField node, TraverserContext<Node> context) {
                GraphQLInputValueDefinition graphQLInputValueDefinition = context.getVarFromParents(GraphQLInputValueDefinition.class);
                GraphQLUnmodifiedType unmodifiedType = unwrapAll(graphQLInputValueDefinition.getType());

                // technically a scalar type can have an AST object field - eg field( arg : Json) -> field(arg : { ast : "here" })
                if (unmodifiedType instanceof GraphQLInputObjectType) {
                    GraphQLInputObjectType inputObjectType = (GraphQLInputObjectType) unmodifiedType;
                    GraphQLInputObjectField inputObjectTypeField = inputObjectType.getField(node.getName());
                    context.setVar(GraphQLInputValueDefinition.class, inputObjectTypeField);
                }
                return TraversalControl.CONTINUE;
            }

            @Override
            protected TraversalControl visitValue(Value<?> value, TraverserContext<Node> context) {
                GraphQLArgument graphQLArgument = context.getVarFromParents(GraphQLArgument.class);
                GraphQLInputValueDefinition graphQLInputValueDefinition = context.getVarFromParents(GraphQLInputValueDefinition.class);
                OverallTypeInfo overallTypeInfo = new OverallTypeInfo(
                        null,
                        null,
                        graphQLArgument,
                        graphQLInputValueDefinition);
                overallTypeInfoMap.put(getId(value), overallTypeInfo);
                return TraversalControl.CONTINUE;
            }

            @Override
            public TraversalControl visitFragmentDefinition(FragmentDefinition fragmentDefinition, TraverserContext<Node> context) {
                GraphQLOutputType outputType = (GraphQLOutputType) assertNotNull(graphQLSchema.getType(fragmentDefinition.getTypeCondition().getName()));
                context.setVar(GraphQLOutputType.class, outputType);
                return TraversalControl.CONTINUE;
            }

            @Override
            public TraversalControl visitInlineFragment(InlineFragment inlineFragment, TraverserContext<Node> context) {
                if (inlineFragment.getTypeCondition() == null) {
                    return TraversalControl.CONTINUE;
                }
                GraphQLOutputType outputType = (GraphQLOutputType) assertNotNull(graphQLSchema.getType(inlineFragment.getTypeCondition().getName()));
                context.setVar(GraphQLOutputType.class, outputType);
                return TraversalControl.CONTINUE;
            }

            @Override
            public TraversalControl visitField(Field field, TraverserContext<Node> context) {
                if (field.getName().equals(TypeNameMetaFieldDef.getName())) {
                    return TraversalControl.CONTINUE;
                }
                GraphQLFieldsContainer fieldsContainer = (GraphQLFieldsContainer) unwrapAll(context.getVarFromParents(GraphQLOutputType.class));
                GraphQLFieldDefinition fieldDefinition = assertNotNull(fieldsContainer.getFieldDefinition(field.getName()), "field %s not found for type %s", field.getName(), fieldsContainer.getName());
                context.setVar(GraphQLFieldDefinition.class, fieldDefinition);
                GraphQLOutputType newOutputType = fieldDefinition.getType();
                context.setVar(GraphQLOutputType.class, newOutputType);

                OverallTypeInfo overallTypeInfo = new OverallTypeInfo(fieldsContainer, fieldDefinition, null, null);
                overallTypeInfoMap.put(getId(field), overallTypeInfo);

                return TraversalControl.CONTINUE;
            }
        };
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        if (rootOutputType != null) {
            rootVars.put(GraphQLOutputType.class, rootOutputType);
        }
        NodeTraverser nodeTraverser = new NodeTraverser(rootVars, Node::getChildren);
        nodeTraverser.depthFirst(recordTypeInfos, node);
        return new OverallTypeInformation<>(overallTypeInfoMap);
    }

}
