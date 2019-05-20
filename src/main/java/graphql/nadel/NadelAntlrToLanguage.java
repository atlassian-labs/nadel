package graphql.nadel;

import graphql.Internal;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.nadel.dsl.CommonDefinition;
import graphql.nadel.dsl.EnumTypeDefinitionWithTransformation;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.InputObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.InterfaceTypeDefinitionWithTransformation;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.ScalarTypeDefinitionWithTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.dsl.TypeMappingDefinition;
import graphql.nadel.dsl.UnionTypeDefinitionWithTransformation;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import graphql.parser.MultiSourceReader;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.nadel.dsl.FieldDefinitionWithTransformation.newFieldDefinitionWithTransformation;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.CONTEXT;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT;
import static graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD;

@Internal
public class NadelAntlrToLanguage extends GraphqlAntlrToLanguage {

    public NadelAntlrToLanguage(CommonTokenStream tokens, MultiSourceReader multiSourceReader) {
        super(tokens, multiSourceReader);
    }

    public StitchingDsl createStitchingDsl(StitchingDSLParser.StitchingDSLContext ctx) {
        StitchingDsl.Builder stitchingDsl = StitchingDsl.newStitchingDSL();
        List<ServiceDefinition> serviceDefintions = ctx.serviceDefinition().stream()
                .map(this::createServiceDefinition)
                .collect(Collectors.toList());
        stitchingDsl.serviceDefinitions(serviceDefintions);
        if (ctx.commonDefinition() != null) {
            stitchingDsl.commonDefinition(createCommonDefinition(ctx.commonDefinition()));
        }
        return stitchingDsl.build();
    }

    private CommonDefinition createCommonDefinition(StitchingDSLParser.CommonDefinitionContext commonDefinitionContext) {
        CommonDefinition.Builder builder = CommonDefinition.newCommonDefinition();
        List<SDLDefinition> definitions = createTypeSystemDefinitions(commonDefinitionContext.typeSystemDefinition());
        builder.sourceLocation(getSourceLocation(commonDefinitionContext));
        builder.comments(getComments(commonDefinitionContext));
        builder.typeDefinitions(definitions);
        return builder.build();
    }

    private ServiceDefinition createServiceDefinition(StitchingDSLParser.ServiceDefinitionContext serviceDefinitionContext) {
        ServiceDefinition.Builder builder = ServiceDefinition.newServiceDefinition();
        builder.name(serviceDefinitionContext.name().getText());
        List<SDLDefinition> definitions = createTypeSystemDefinitions(serviceDefinitionContext.typeSystemDefinition());
        builder.definitions(definitions);
        return builder.build();
    }


    private List<SDLDefinition> createTypeSystemDefinitions(List<StitchingDSLParser.TypeSystemDefinitionContext> typeSystemDefinitionContexts) {
        return typeSystemDefinitionContexts.stream().map(this::createTypeSystemDefinition).collect(Collectors.toList());
    }

    @Override
    protected FieldDefinition createFieldDefinition(StitchingDSLParser.FieldDefinitionContext ctx) {
        FieldDefinition fieldDefinition = super.createFieldDefinition(ctx);
        if (ctx.fieldTransformation() == null) {
            return fieldDefinition;
        }
        FieldDefinitionWithTransformation.Builder builder = newFieldDefinitionWithTransformation(fieldDefinition);
        builder.fieldTransformation(createFieldTransformation(ctx.fieldTransformation()));
        return builder.build();
    }

    private FieldTransformation createFieldTransformation(StitchingDSLParser.FieldTransformationContext ctx) {
        if (ctx.fieldMappingDefinition() != null) {
            return new FieldTransformation(createFieldMappingDefinition(ctx.fieldMappingDefinition()),
                    getSourceLocation(ctx), new ArrayList<>());
        } else if (ctx.innerServiceHydration() != null) {
            return new FieldTransformation(createInnerServiceHydration(ctx.innerServiceHydration()),
                    getSourceLocation(ctx), new ArrayList<>());
        } else {
            return assertShouldNeverHappen();
        }
    }

    private FieldMappingDefinition createFieldMappingDefinition(StitchingDSLParser.FieldMappingDefinitionContext ctx) {
        return new FieldMappingDefinition(ctx.name().getText(), getSourceLocation(ctx), new ArrayList<>());
    }

    private InnerServiceHydration createInnerServiceHydration(StitchingDSLParser.InnerServiceHydrationContext ctx) {
        String serviceName = ctx.serviceName().getText();
        String topLevelField = ctx.topLevelField().getText();

        List<RemoteArgumentDefinition> remoteArguments = new ArrayList<>();
        List<StitchingDSLParser.RemoteArgumentPairContext> remoteArgumentPairContexts = ctx.remoteCallDefinition()
                .remoteArgumentPair();
        for (StitchingDSLParser.RemoteArgumentPairContext remoteArgumentPairContext : remoteArgumentPairContexts) {
            remoteArguments.add(createRemoteArgumentDefinition(remoteArgumentPairContext));
        }
        String objectIdentifier = "id";
        if (ctx.objectIdentifier() != null) {
            objectIdentifier = ctx.objectIdentifier().name().getText();
        }
        Integer batchSize = null;
        if (ctx.batchSize() != null) {
            batchSize = Integer.parseInt(ctx.batchSize().intValue().getText());
        }
        return new InnerServiceHydration(getSourceLocation(ctx), new ArrayList<>(), serviceName, topLevelField,
                remoteArguments, objectIdentifier, batchSize);
    }

    @Override
    protected ObjectTypeDefinition createObjectTypeDefinition(StitchingDSLParser.ObjectTypeDefinitionContext ctx) {
        ObjectTypeDefinition objectTypeDefinition = super.createObjectTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return objectTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return ObjectTypeDefinitionWithTransformation.newObjectTypeDefinitionWithTransformation(objectTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }

    @Override
    protected UnionTypeDefinition createUnionTypeDefinition(StitchingDSLParser.UnionTypeDefinitionContext ctx) {
        UnionTypeDefinition unionTypeDefinition = super.createUnionTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return unionTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return UnionTypeDefinitionWithTransformation.newUnionTypeDefinitionWithTransformation(unionTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }

    @Override
    protected InterfaceTypeDefinition createInterfaceTypeDefinition(StitchingDSLParser.InterfaceTypeDefinitionContext ctx) {
        InterfaceTypeDefinition interfaceTypeDefinition = super.createInterfaceTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return interfaceTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return InterfaceTypeDefinitionWithTransformation.newInterfaceTypeDefinitionWithTransformation(interfaceTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }

    @Override
    protected InputObjectTypeDefinition createInputObjectTypeDefinition(StitchingDSLParser.InputObjectTypeDefinitionContext ctx) {
        InputObjectTypeDefinition inputObjectTypeDefinition = super.createInputObjectTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return inputObjectTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return InputObjectTypeDefinitionWithTransformation.newInputObjectTypeDefinitionWithTransformation(inputObjectTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }

    @Override
    protected EnumTypeDefinition createEnumTypeDefinition(StitchingDSLParser.EnumTypeDefinitionContext ctx) {
        EnumTypeDefinition enumTypeDefinition = super.createEnumTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return enumTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return EnumTypeDefinitionWithTransformation.newEnumTypeDefinitionWithTransformation(enumTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }

    @Override
    protected ScalarTypeDefinition createScalarTypeDefinition(StitchingDSLParser.ScalarTypeDefinitionContext ctx) {
        ScalarTypeDefinition scalarTypeDefinition = super.createScalarTypeDefinition(ctx);
        if (ctx.typeTransformation() == null) {
            return scalarTypeDefinition;
        }
        TypeMappingDefinition typeMappingDefinition = createTypeMappingDef(ctx.typeTransformation(), ctx.name());
        return ScalarTypeDefinitionWithTransformation.newScalarTypeDefinitionWithTransformation(scalarTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
    }


    private TypeMappingDefinition createTypeMappingDef(StitchingDSLParser.TypeTransformationContext typeTransformationContext, StitchingDSLParser.NameContext name) {
        TypeMappingDefinition typeMappingDefinition = new TypeMappingDefinition(null, new ArrayList<>());
        typeMappingDefinition.setUnderlyingName(typeTransformationContext.typeMappingDefinition().name().getText());
        typeMappingDefinition.setOverallName(name.getText());
        return typeMappingDefinition;
    }

    private RemoteArgumentDefinition createRemoteArgumentDefinition(StitchingDSLParser.RemoteArgumentPairContext
                                                                            remoteArgumentPairContext) {
        return new RemoteArgumentDefinition(remoteArgumentPairContext.name().getText(),
                createRemoteArgumentSource(remoteArgumentPairContext.remoteArgumentSource()),
                getSourceLocation(remoteArgumentPairContext));
    }

    private RemoteArgumentSource createRemoteArgumentSource(StitchingDSLParser.RemoteArgumentSourceContext ctx) {
        RemoteArgumentSource.SourceType argumentType = null;
        String argumentName = null;

        if (ctx.fieldArgumentReference() != null) {
            argumentName = ctx.fieldArgumentReference().name().getText();
            argumentType = FIELD_ARGUMENT;
        } else if (ctx.contextArgumentReference() != null) {
            argumentName = ctx.contextArgumentReference().name().getText();
            argumentType = CONTEXT;
        } else if (ctx.sourceObjectReference() != null) {
            argumentName = ctx.sourceObjectReference().name().getText();
            argumentType = OBJECT_FIELD;
        } else {
            assertShouldNeverHappen();
        }

        return new RemoteArgumentSource(argumentName, argumentType, getSourceLocation(ctx));
    }
}
