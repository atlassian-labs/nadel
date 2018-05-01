package graphql.nadel;

import com.atlassian.braid.Braid;
import com.atlassian.braid.Link;
import com.atlassian.braid.SchemaNamespace;
import com.atlassian.braid.SchemaSource;
import com.atlassian.braid.document.DocumentMappers;
import com.atlassian.braid.source.GraphQLRemoteRetriever;
import com.atlassian.braid.source.GraphQLRemoteSchemaSource;
import graphql.Assert;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.PublicApi;
import graphql.execution.AsyncExecutionStrategy;
import graphql.language.FieldDefinition;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.nadel.dsl.FieldTransformation;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.errors.SchemaProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static graphql.Assert.assertNotNull;

@PublicApi
public class Nadel {

    private final String dsl;
    private StitchingDsl stitchingDsl;
    private Parser parser = new Parser();
    private GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory;
    private Braid braid;

    public Nadel(String dsl, GraphQLRemoteRetrieverFactory graphQLRemoteRetrieverFactory) {
        this.dsl = dsl;
        this.graphQLRemoteRetrieverFactory = graphQLRemoteRetrieverFactory;
        this.stitchingDsl = this.parser.parseDSL(dsl);
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();

        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            TypeDefinitionRegistry typeDefinitionRegistry = buildRegistry(serviceDefinition);
            this.stitchingDsl.getTypesByService().put(serviceDefinition.getName(), typeDefinitionRegistry);
            SchemaNamespace schemaNamespace = SchemaNamespace.of(serviceDefinition.getName());
            this.stitchingDsl.getNamespaceByService().put(serviceDefinition.getName(), schemaNamespace);

        }
        Map<SchemaNamespace, List<Link>> linksByNamespace = createLinks();
        List<SchemaSource> schemaSources = new ArrayList<>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {

            SchemaNamespace namespace = this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName());
            TypeDefinitionRegistry typeDefinitionRegistry = this.stitchingDsl.getTypesByService().get(serviceDefinition.getName());
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
        for (TypeDefinition definition : serviceDefinition.getTypeDefinitions()) {
            typeRegistry.add(definition).ifPresent(errors::add);
        }
        if (errors.size() > 0) {
            throw new SchemaProblem(errors);
        } else {
            return typeRegistry;
        }
    }

    private Map<SchemaNamespace, List<Link>> createLinks() {
        Map<SchemaNamespace, List<Link>> result = new LinkedHashMap<>();
        Map<FieldDefinition, FieldTransformation> transformationsByFieldDefinition = this.stitchingDsl.getTransformationsByFieldDefinition();
        transformationsByFieldDefinition.forEach((fieldDefinition, fieldTransformation) -> {
            ServiceDefinition serviceDefinition = this.stitchingDsl.getServiceByField().get(fieldDefinition);
            SchemaNamespace schemaNamespace = assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceDefinition.getName()));
            result.putIfAbsent(schemaNamespace, new ArrayList<>());


            String parentType = fieldTransformation.getParentDefinition().getName();
            String originalFieldName = fieldDefinition.getName();
            String targetType = TypeInfo.typeInfo(fieldTransformation.getTargetType()).getTypeName().getName();
            String newFieldName = fieldTransformation.getTargetName();
            String queryField = targetType.toLowerCase();
            SchemaNamespace targetNamespace = findNameSpace(targetType);
            Link link = Link
                    .from(schemaNamespace, parentType, newFieldName, originalFieldName)
                    .to(targetNamespace, targetType, queryField, "id")
                    .build();
            result.get(schemaNamespace).add(link);
        });
        return result;
    }

    private SchemaNamespace findNameSpace(String type) {
        Map<String, TypeDefinitionRegistry> typesByService = this.stitchingDsl.getTypesByService();
        AtomicReference<String> serviceName = new AtomicReference<>();
        typesByService.forEach((service, typeDefinitionRegistry) -> {
            if (typeDefinitionRegistry.hasType(new TypeName(type))) {
                serviceName.set(service);
            }
        });
        if (serviceName.get() != null) {
            return assertNotNull(this.stitchingDsl.getNamespaceByService().get(serviceName.get()));
        }
        return Assert.assertShouldNeverHappen();
    }


    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return this.braid.newGraphQL().execute(executionInput);
//        return this.graphql.executeAsync(executionInput);
//        return null;
    }

}
