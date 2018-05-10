package graphql.nadel;

import graphql.Assert;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.LinkedField;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.RuleNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.parser.StringValueParsing.parseSingleQuotedString;

public class NadelAntlrToLanguage extends GraphqlAntlrToLanguage {

    private StitchingDsl stitchingDsl;


    public NadelAntlrToLanguage(CommonTokenStream tokens) {
        super(tokens);
    }

    enum NadelContextProperty {
        ServiceDefinition,

    }

    private static class NadelContextEntry {
        final NadelContextProperty contextProperty;
        final Object value;

        public NadelContextEntry(NadelContextProperty contextProperty, Object value) {
            this.contextProperty = contextProperty;
            this.value = value;
        }
    }

    private final Deque<NadelContextEntry> contextStack = new ArrayDeque<>();
    private final List<ContextEntry> contextEntriesRecorder = new ArrayList<>();

    private boolean recording;

    private void startRecording() {
        recording = true;
        contextEntriesRecorder.clear();
    }

    private void stopRecording() {
        recording = false;
    }

    @Override
    protected void addContextProperty(ContextProperty contextProperty, Object value) {
        super.addContextProperty(contextProperty, value);
        if (recording) {
            contextEntriesRecorder.add(getContextStack().getFirst());
        }
    }

    protected void addContextProperty(NadelContextProperty contextProperty, Object value) {
        contextStack.addFirst(new NadelContextEntry(contextProperty, value));
    }

    private void popNadelContext() {
        contextStack.removeFirst();
    }

    protected Object getFromContextStack(NadelContextProperty contextProperty) {
        return getFromContextStack(contextProperty, false);
    }

    @SuppressWarnings("SameParameterValue")
    private Object getFromContextStack(NadelContextProperty contextProperty, boolean required) {
        for (NadelContextEntry contextEntry : contextStack) {
            if (contextEntry.contextProperty == contextProperty) {
                return contextEntry.value;
            }
        }
        if (required) {
            assertShouldNeverHappen("not found %s", contextProperty);
        }
        return null;
    }

    public StitchingDsl getStitchingDsl() {
        return stitchingDsl;
    }

    @Override
    public Void visitStitchingDSL(StitchingDSLParser.StitchingDSLContext ctx) {
        stitchingDsl = new StitchingDsl();
        setResult(new Document());
        return super.visitStitchingDSL(ctx);
    }

    @Override
    public Void visitServiceDefinition(StitchingDSLParser.ServiceDefinitionContext ctx) {
        String url = parseSingleQuotedString(ctx.serviceUrl().stringValue().getText());
        String name = ctx.name().getText();
        ServiceDefinition def = new ServiceDefinition(name, url);
        addContextProperty(NadelContextProperty.ServiceDefinition, def);
        super.visitChildren(ctx);
        popNadelContext();
        stitchingDsl.getServiceDefinitions().add(def);
        return null;
    }


    @Override
    public Void visitTypeDefinition(StitchingDSLParser.TypeDefinitionContext ctx) {
        startRecording();
        super.visitTypeDefinition(ctx);
        stopRecording();
        TypeDefinition typeDefinition = (TypeDefinition) contextEntriesRecorder.get(0).value;
        ServiceDefinition serviceDefinition = (ServiceDefinition) getFromContextStack(NadelContextProperty.ServiceDefinition);
        serviceDefinition.getTypeDefinitions().add(typeDefinition);
        return null;
    }


    //    @Override
    public Void visitFieldTransformation(StitchingDSLParser.FieldTransformationContext ctx) {
        FieldDefinition fieldDefinition = (FieldDefinition) getFromContextStack(ContextProperty.FieldDefinition);
        ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) getFromContextStack(ContextProperty.ObjectTypeDefinition);
        FieldTransformation fieldTransformation = new FieldTransformation();
        if (ctx.linkDefinition() != null) {
            StitchingDSLParser.LinkDefinitionContext link = ctx.linkDefinition();
            fieldTransformation.setTargetName(link.fieldName().getText());
            fieldTransformation.setParentDefinition(objectTypeDefinition);
            fieldTransformation.setArgumentName(link.argumentName().name().getText());
            fieldTransformation.setTopLevelField(link.topLevelField().name().getText());
            fieldTransformation.setAdded(link.added() != null);
            this.stitchingDsl.getTransformationsByFieldDefinition().put(fieldDefinition, fieldTransformation);
        }
        return null;
    }


    private Type createType(StitchingDSLParser.TypeContext typeContext) {

        if (typeContext.typeName() != null) {
            return new TypeName(typeContext.typeName().name().getText());
        } else if (typeContext.listType() != null) {
            return new ListType(createType(typeContext.listType().type()));
        } else if (typeContext.nonNullType() != null) {
            StitchingDSLParser.NonNullTypeContext nonNullTypeContext = typeContext.nonNullType();
            Type subType;
            if (nonNullTypeContext.typeName() != null) {
                subType = new TypeName(nonNullTypeContext.typeName().name().getText());
            } else {
                subType = new ListType(createType(typeContext.listType().type()));
            }
            return new NonNullType(subType);
        }
        return Assert.assertShouldNeverHappen();
    }


    @Override
    public Void visitChildren(RuleNode node) {
        if (getContextStack().size() > 0 && getContextStack().getFirst().contextProperty == ContextProperty.FieldDefinition) {
            ServiceDefinition serviceDefinition = (ServiceDefinition) getFromContextStack(NadelContextProperty.ServiceDefinition);
            this.stitchingDsl.getServiceByField().put((FieldDefinition) getContextStack().getFirst().value, serviceDefinition);
        }
        return super.visitChildren(node);
    }

    @Override
    public Void visitLinkDefinition(StitchingDSLParser.LinkDefinitionContext ctx) {
        String topLevelField = ctx.topLevelField().name().getText();
        String variableName = ctx.variableName().name().getText();
        String fieldName = ctx.fieldName().name().getText();
        ServiceDefinition serviceDefinition = (ServiceDefinition) getFromContextStack(NadelContextProperty.ServiceDefinition);
        ObjectTypeDefinition parentType = (ObjectTypeDefinition) getFromContextStack(ContextProperty.ObjectTypeDefinition);
        LinkedField linkedField = new LinkedField();
        linkedField.setArgumentName(ctx.argumentName().name().getText());
        linkedField.setFieldName(fieldName);
        linkedField.setVariableName(variableName);
        linkedField.setTopLevelQueryField(topLevelField);
        linkedField.setParentType(parentType.getName());
        serviceDefinition.getLinks().add(linkedField);
        return super.visitLinkDefinition(ctx);
    }
}
