package graphql.nadel.engine.transformation;

import graphql.language.AstNodeAdapter;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.nadel.engine.FieldMetadataUtil;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformer;
import graphql.util.TreeTransformerUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static graphql.Assert.assertNotNull;
import static graphql.introspection.Introspection.TypeNameMetaFieldDef;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

public class RecordTypeInformation {

    public static class OverallTypeInformation<T extends Node> {
        T node;
        Map<String, FieldTypeInfo> fieldInfoById = new LinkedHashMap<>();

        public OverallTypeInformation(T node, Map<String, FieldTypeInfo> fieldInfoById) {
            this.node = node;
            this.fieldInfoById = fieldInfoById;
        }

        public T getNode() {
            return node;
        }

        public Map<String, FieldTypeInfo> getFieldInfoById() {
            return fieldInfoById;
        }
    }

    public static class FieldTypeInfo {
        GraphQLFieldsContainer fieldsContainer;
        GraphQLFieldDefinition fieldDefinition;

        public FieldTypeInfo(GraphQLFieldsContainer fieldsContainer, GraphQLFieldDefinition fieldDefinition) {
            this.fieldsContainer = fieldsContainer;
            this.fieldDefinition = fieldDefinition;
        }

        public GraphQLFieldsContainer getFieldsContainer() {
            return fieldsContainer;
        }

        public void setFieldsContainer(GraphQLFieldsContainer fieldsContainer) {
            this.fieldsContainer = fieldsContainer;
        }

        public GraphQLFieldDefinition getFieldDefinition() {
            return fieldDefinition;
        }

        public void setFieldDefinition(GraphQLFieldDefinition fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
        }
    }

    public <T extends Node> OverallTypeInformation<T> recordOverallTypes(T node, GraphQLSchema graphQLSchema, GraphQLOutputType rootOutputType) {

        Map<String, FieldTypeInfo> fieldTypeInfoMap = new LinkedHashMap<>();

        NodeVisitorStub recordTypeInfos = new NodeVisitorStub() {

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
                GraphQLFieldDefinition fieldDefinition = fieldsContainer.getFieldDefinition(field.getName());
                GraphQLOutputType newOutputType = fieldDefinition.getType();
                context.setVar(GraphQLOutputType.class, newOutputType);

                String id = UUID.randomUUID().toString();
                FieldTypeInfo fieldTypeInfo = new FieldTypeInfo(fieldsContainer, fieldDefinition);
                fieldTypeInfoMap.put(id, fieldTypeInfo);

                Field changedField = FieldMetadataUtil.setOverallTypeInfoId(field, id);
                return TreeTransformerUtil.changeNode(context, changedField);
            }
        };
        TreeTransformer<Node> treeTransformer = new TreeTransformer<>(AstNodeAdapter.AST_NODE_ADAPTER);
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        if (rootOutputType != null) {
            rootVars.put(GraphQLOutputType.class, rootOutputType);
        }
        Node newNode = treeTransformer.transform(node, new TraverserVisitorStub<Node>() {
                    @Override
                    public TraversalControl enter(TraverserContext<Node> context) {
                        return context.thisNode().accept(context, recordTypeInfos);
                    }
                },
                rootVars
        );
        return new OverallTypeInformation<>((T) newNode, fieldTypeInfoMap);
    }

}
