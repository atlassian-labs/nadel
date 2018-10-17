package graphql.nadel;

import com.atlassian.braid.transformation.SchemaTransformation;
import graphql.PublicSpi;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Factory that provides {@link SchemaTransformation}s to be applied on a stitched schema.
 */
@PublicSpi
public interface SchemaTransformationsFactory {
    SchemaTransformationsFactory DEFAULT = (Map<String, TypeDefinitionRegistry> serviceTypeDefinitions) ->
            Collections.emptyList();

    /**
     * Creates a collection of {@link SchemaTransformation}s that will be passed to braid using
     * {@link com.atlassian.braid.Braid.BraidBuilder#customSchemaTransformations(List)}.
     *
     * @param serviceTypeDefinitions map containing  {@link TypeDefinitionRegistry} (as a value) for each service name
     *                               (key) defined in Nadel DSL. May be used to add new types based on directives etc.
     *
     * @return schema transformations to be applied on stitched schema, may be empty, cannot be null.
     */
    List<SchemaTransformation> create(Map<String, TypeDefinitionRegistry> serviceTypeDefinitions);
}
