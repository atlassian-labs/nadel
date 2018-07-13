package graphql.nadel;

import graphql.Assert;
import graphql.language.Definition;
import graphql.language.FieldDefinition;
import graphql.nadel.dsl.FieldDefinitionWithTransformation;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.InnerServiceTransformation;
import graphql.nadel.dsl.InputMappingDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.nadel.parser.GraphqlAntlrToLanguage;
import graphql.nadel.parser.antlr.StitchingDSLParser;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        } else if (ctx.innerServiceTransformation() != null) {
            return new FieldTransformation(createInnerServiceTransformation(ctx.innerServiceTransformation()), null, new ArrayList<>());
        } else {
            return Assert.assertShouldNeverHappen();
        }
    }

    private InputMappingDefinition createInputMappingDefinition(StitchingDSLParser.InputMappingDefinitionContext ctx) {
        return new InputMappingDefinition(ctx.name().getText(), null, new ArrayList<>());
    }

    private InnerServiceTransformation createInnerServiceTransformation(StitchingDSLParser.InnerServiceTransformationContext ctx) {
        String serviceName = ctx.serviceName().getText();
        String topLevelField = ctx.topLevelField().getText();

        Map<String, InputMappingDefinition> inputMappingDefinitionMap = new LinkedHashMap<>();
        List<StitchingDSLParser.RemoteArgumentPairContext> remoteArgumentPairContexts = ctx.remoteCallDefinition().remoteArgumentList().remoteArgumentPair();
        for (StitchingDSLParser.RemoteArgumentPairContext remoteArgumentPairContext : remoteArgumentPairContexts) {
            inputMappingDefinitionMap.put(remoteArgumentPairContext.name().getText(), createInputMappingDefinition(remoteArgumentPairContext.inputMappingDefinition()));
        }
        return new InnerServiceTransformation(null, new ArrayList<>(), serviceName, topLevelField, inputMappingDefinitionMap);
    }

}
