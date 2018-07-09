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
import graphql.nadel.dsl.FieldReference;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.dsl.TypeTransformation;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.RuleNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.Assert.assertShouldNeverHappen;

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
        String name = ctx.name().getText();
        ServiceDefinition def = new ServiceDefinition(name);
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

    @Override
    public Void visitTypeTransformation(StitchingDSLParser.TypeTransformationContext ctx) {
        ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) getFromContextStack(ContextProperty.ObjectTypeDefinition);
        TypeTransformation typeTransformation = new TypeTransformation();
        typeTransformation.setTargetName(ctx.innerTypeTransformation().name().getText());
        this.stitchingDsl.getTransformationsByTypeDefinition().put(objectTypeDefinition, typeTransformation);
        return null;
    }

    @Override
    public Void visitFieldTransformation(StitchingDSLParser.FieldTransformationContext ctx) {
        FieldDefinition fieldDefinition = (FieldDefinition) getFromContextStack(ContextProperty.FieldDefinition);
        ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) getFromContextStack(ContextProperty.ObjectTypeDefinition);
        FieldTransformation fieldTransformation = new FieldTransformation();
        fieldTransformation.setParentDefinition(objectTypeDefinition);
        fieldTransformation.setTargetName(fieldDefinition.getName());
        fieldTransformation.setTargetType(fieldDefinition.getType());
        if (ctx.inputMappingDefinition() != null) {
            fieldTransformation.setTargetName(ctx.inputMappingDefinition().name().getText());
            this.stitchingDsl.getTransformationsByFieldDefinition().put(fieldDefinition, fieldTransformation);
        }
        if (ctx.innerServiceTransformation() != null) {
            StitchingDSLParser.InnerServiceTransformationContext transContext = ctx.innerServiceTransformation();
            fieldTransformation.setTargetName(transContext.fieldName().getText());
            fieldTransformation.setServiceName(transContext.serviceName().getText());
            if (transContext.remoteCallDefinition() != null) {
                Map<String, FieldReference> m = transContext
                        .remoteCallDefinition()
                        .remoteArgumentList()
                        .remoteArgumentPair()
                        .stream()
                        .collect(
                                Collectors.toMap(p -> p.name().getText(), p -> new FieldReference(p.inputMappingDefinition().name().getText()))
                        );
                fieldTransformation.setArguments(m);
            }

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
}
