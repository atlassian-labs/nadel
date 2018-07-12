package graphql.nadel;

import graphql.language.Definition;
import graphql.language.FieldDefinition;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InputMappingDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NadelAntlrToLanguage extends GraphqlAntlrToLanguage {


    public NadelAntlrToLanguage(CommonTokenStream tokens) {
        super(tokens);
    }

    public StitchingDsl createStitchingDsl(StitchingDSLParser.StitchingDSLContext ctx) {
        StitchingDsl.Builder stitchingDsl = StitchingDsl.newStitchingDSL();
        List<ServiceDefinition> serviceDefintions = ctx.serviceDefinition().stream().map(this::createServiceDefinition).collect(Collectors.toList());
        stitchingDsl.serviceDefinitions(serviceDefintions);
        return stitchingDsl.build();
    }

    private ServiceDefinition createServiceDefinition(StitchingDSLParser.ServiceDefinitionContext serviceDefinitionContext) {
        ServiceDefinition.Builder builder = ServiceDefinition.newServiceDefinition();
        builder.name(serviceDefinitionContext.name().getText());
        List<Definition> definitions = createTypeSystemDefinitions(serviceDefinitionContext.typeSystemDefinition());
        builder.definitions(definitions);
        return builder.build();
    }


    private List<Definition> createTypeSystemDefinitions(List<StitchingDSLParser.TypeSystemDefinitionContext> typeSystemDefinitionContexts) {
        return typeSystemDefinitionContexts.stream().map(this::createTypeSystemDefinition).collect(Collectors.toList());
    }

    @Override
    protected FieldDefinition createFieldDefinition(StitchingDSLParser.FieldDefinitionContext ctx) {
        FieldDefinition fieldDefinition = super.createFieldDefinition(ctx);
        if (ctx.fieldTransformation() == null) {
            return fieldDefinition;
        }
        FieldDefinitionWithTransformation.Builder builder = FieldDefinitionWithTransformation.newFieldDefinitionWithTransformation(fieldDefinition);
        builder.fieldTransformation(createFieldTransformation(ctx.fieldTransformation()));
        return builder.build();
    }

    private FieldTransformation createFieldTransformation(StitchingDSLParser.FieldTransformationContext ctx) {
        if (ctx.inputMappingDefinition() != null) {
            return new FieldTransformation(createInputMappingDefinition(ctx.inputMappingDefinition()), null, new ArrayList<>());
        }
        return null;
    }

    private InputMappingDefinition createInputMappingDefinition(StitchingDSLParser.InputMappingDefinitionContext ctx) {
        return new InputMappingDefinition(ctx.name().getText(), null, new ArrayList<>());
    }

    //
//    @Override
//    public Void visitFieldTransformation(StitchingDSLParser.FieldTransformationContext ctx) {
//        FieldDefinition fieldDefinition = (FieldDefinition) getFromContextStack(ContextProperty.FieldDefinition);
//        ObjectTypeDefinition objectTypeDefinition = (ObjectTypeDefinition) getFromContextStack(ContextProperty.ObjectTypeDefinition);
//        FieldTransformation fieldTransformation = new FieldTransformation();
//        fieldTransformation.setParentDefinition(objectTypeDefinition);
//        fieldTransformation.setTargetName(fieldDefinition.getName());
//        fieldTransformation.setTargetType(fieldDefinition.getType());
//        if (ctx.inputMappingDefinition() != null) {
//            fieldTransformation.setTargetName(ctx.inputMappingDefinition().name().getText());
//            this.stitchingDsl.getTransformationsByFieldDefinition().put(fieldDefinition, fieldTransformation);
//        }
//        if (ctx.innerServiceTransformation() != null) {
//            StitchingDSLParser.InnerServiceTransformationContext transContext = ctx.innerServiceTransformation();
//            fieldTransformation.setTargetName(transContext.fieldName().getText());
//            fieldTransformation.setServiceName(transContext.serviceName().getText());
//            if (transContext.remoteCallDefinition() != null) {
//                Map<String, FieldReference> m = transContext
//                        .remoteCallDefinition()
//                        .remoteArgumentList()
//                        .remoteArgumentPair()
//                        .stream()
//                        .collect(
//                                Collectors.toMap(p -> p.name().getText(), p -> new FieldReference(p.inputMappingDefinition().name().getText()))
//                        );
//                fieldTransformation.setArguments(m);
//            }
//
//            this.stitchingDsl.getTransformationsByFieldDefinition().put(fieldDefinition, fieldTransformation);
//        }
//        return null;
//    }
//
//
//    private Type createType(StitchingDSLParser.TypeContext typeContext) {
//
//        if (typeContext.typeName() != null) {
//            return new TypeName(typeContext.typeName().name().getText());
//        } else if (typeContext.listType() != null) {
//            return new ListType(createType(typeContext.listType().type()));
//        } else if (typeContext.nonNullType() != null) {
//            StitchingDSLParser.NonNullTypeContext nonNullTypeContext = typeContext.nonNullType();
//            Type subType;
//            if (nonNullTypeContext.typeName() != null) {
//                subType = new TypeName(nonNullTypeContext.typeName().name().getText());
//            } else {
//                subType = new ListType(createType(typeContext.listType().type()));
//            }
//            return new NonNullType(subType);
//        }
//        return Assert.assertShouldNeverHappen();
//    }
//
//
//    @Override
//    public Void visitChildren(RuleNode node) {
//        if (getContextStack().size() > 0 && getContextStack().getFirst().contextProperty == ContextProperty.FieldDefinition) {
//            ServiceDefinition serviceDefinition = (ServiceDefinition) getFromContextStack(NadelContextProperty.ServiceDefinition);
//            this.stitchingDsl.getServiceByField().put((FieldDefinition) getContextStack().getFirst().value, serviceDefinition);
//        }
//        return super.visitChildren(node);
//    }
}
