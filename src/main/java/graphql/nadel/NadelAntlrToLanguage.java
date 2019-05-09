package graphql.nadel;

import graphql.Internal;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldMappingDefinition;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InnerServiceHydration;
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation;
import graphql.nadel.dsl.RemoteArgumentDefinition;
import graphql.nadel.dsl.RemoteArgumentSource;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.dsl.TypeMappingDefinition;
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
        return stitchingDsl.build();
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
        TypeMappingDefinition typeMappingDefinition = new TypeMappingDefinition(null, new ArrayList<>());
        typeMappingDefinition.setUnderlyingName(ctx.typeTransformation().typeMappingDefinition().name().getText());
        typeMappingDefinition.setOverallName(ctx.name().getText());
        return ObjectTypeDefinitionWithTransformation.newObjectTypeDefinitionWithTransformation(objectTypeDefinition)
                .typeMappingDefinition(typeMappingDefinition)
                .build();
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
