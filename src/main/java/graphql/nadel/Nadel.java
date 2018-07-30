package graphql.nadel;

import com.atlassian.braid.Braid;
import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.source.GraphQLRemoteRetriever;
import com.atlassian.braid.source.GraphQLRemoteSchemaSource;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.Definition;
import graphql.language.SDLDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@PublicApi
public class Nadel {

    private final String dsl;
    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory;
    private Braid braid;

    private final Map<String, TypeDefinitionRegistry> typesByService = new LinkedHashMap<>();
    private final Map<String, SchemaNamespace> namespaceByService = new LinkedHashMap<>();

    public Nadel(String dsl, GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory) {
        this.dsl = dsl;
        this.graphQLRemoteRetrieverFactory = graphQLRemoteRetrieverFactory;
        this.stitchingDsl = this.parser.parseDSL(dsl);

        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.typesByService.put(serviceDefinition.getName(), typeDefinitionRegistry);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            namespaceByService.put(serviceDefinition.getName(), schemaNamespace);

        }
        Map<SchemaNamespace, List<Link>> linksByNamespace = createLinks();
        List<SchemaSource> schemaSources = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {

            SchemaNamespace namespace = namespaceByService.get(serviceDefinition.getName());
            TypeDefinitionRegistry typeDefinitionRegistry = typesByService.get(serviceDefinition.getName());
            GraphQLRemoteRetriever remoteRetriever = this.graphQLRemoteRetrieverFactory.createRemoteRetriever(serviceDefinition);
            List<Link> links = linksByNamespace.getOrDefault(namespace, new ArrayList<>());
            SchemaSource schemaSource = createSchemaSource(typeDefinitionRegistry, namespace, remoteRetriever, links);
            schemaSources.add(schemaSource);
        }

        AsyncExecutionStrategy asyncExecutionStrategy = new AsyncExecutionStrategy();
        this.braid = Braid.builder()
                .executionStrategy(asyncExecutionStrategy)
                .schemaSources(schemaSources)
                .build();
    }

    private SchemaSource createSchemaSource(TypeDefinitionRegistry typeDefinitionRegistry,
                                            SchemaNamespace schemaNamespace,
                                            GraphQLRemoteRetriever<Object> remoteRetriever,
                                            List<Link> links) {
        return new GraphQLRemoteSchemaSource<>(schemaNamespace,
                typeDefinitionRegistry,
                typeDefinitionRegistry,
                remoteRetriever,
                links,
                DocumentMappers.identity());
    }

    public TypeDefinitionRegistry buildRegistry(ServiceDefinition serviceDefinition) {
        List<GraphQLError> errors = new ArrayList<>();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
        for (Definition definition : serviceDefinition.getTypeDefinitions()) {
            if (definition instanceof SDLDefinition) {
                typeRegistry.add((SDLDefinition) definition).ifPresent(errors::add);
            }
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }

    private Map<SchemaNamespace, List<Link>> createLinks() {
        Map<SchemaNamespace, List<Link>> result = new LinkedHashMap<>();
//        Map<FieldDefinition, FieldTransformation> transformationsByFieldDefinition = this.stitchingDsl.getTransformationsByFieldDefinition();
//        transformationsByFieldDefinition.forEach((fieldDefinition, fieldTransformation) -> {
//            ServiceDefinition serviceDefinition = this.stitchingDsl.getServiceByField().get(fieldDefinition);
//            SchemaNamespace schemaNamespace = assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName()));
//            result.putIfAbsent(schemaNamespace, new ArrayList<>());
//
//
//            String parentType = fieldTransformation.getParentDefinition().getName();
//
//            String originalFieldName = fieldDefinition.getName();
//            Type targetType = assertNotNull(fieldTransformation.getTargetType());
//            String targetTypeName = TypeInfo.typeInfo(targetType).getTypeName().getName();
//            String newFieldName = fieldTransformation.getTargetName();
//
//            String queryField = targetTypeName.toLowerCase();
//            SchemaNamespace targetNamespace = findNameSpace(targetTypeName);
//            Link link = Link
//                    .from(schemaNamespace, parentType, newFieldName, originalFieldName)
//                    .to(targetNamespace, targetTypeName, queryField, "id")
//                    .replaceFromField()
//                    .build();
//            result.get(schemaNamespace).add(link);
//        });
        return result;
    }

//    private SchemaNamespace findNameSpace(String type) {
//        Map<String, TypeDefinitionRegistry> typesByService = this.stitchingDsl.getTypesByService();
//        AtomicReference<String> serviceName = new AtomicReference<>();
//        typesByService.forEach((service, typeDefinitionRegistry) -> {
//            if (typeDefinitionRegistry.hasType(new TypeName(type))) {
//                serviceName.set(service);
//            }
//        });
//        if (serviceName.get() != null) {
//            return assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceName.get()));
//        }
//        return Assert.assertShouldNeverHappen();
//    }


    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
    }

}
