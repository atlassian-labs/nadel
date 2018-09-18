package graphql.nadel;

import graphql.PublicApi;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores {@link TypeDefinitionRegistry} and collection of {@link TypeRuntimeWiring}s.
 */
@PublicApi
public class TypeDefinitionsWithRuntimeWiring {
    private final TypeDefinitionRegistry typeDefinitionRegistry;
    private final List<TypeRuntimeWiring> typeRuntimeWirings;

    private TypeDefinitionsWithRuntimeWiring(TypeDefinitionRegistry typeDefinitionRegistry,
                                             List<TypeRuntimeWiring> typeRuntimeWirings) {
        this.typeDefinitionRegistry = typeDefinitionRegistry;
        this.typeRuntimeWirings = typeRuntimeWirings;
    }

    public static Builder newTypeDefinitionWithRuntimeWiring() {
        return new Builder();
    }

    public TypeDefinitionRegistry typeDefinitionRegistry() {
        return typeDefinitionRegistry;
    }

    public List<TypeRuntimeWiring> typeRuntimeWirings() {
        return typeRuntimeWirings;
    }

    public static class Builder {
        private TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        private List<TypeRuntimeWiring> typeRuntimeWirings = new ArrayList<>();

        private Builder() {
        }

        public Builder typeDefinitionRegistry(TypeDefinitionRegistry registry) {
            this.typeDefinitionRegistry = Objects.requireNonNull(registry, "registry");
            return this;
        }

        public Builder typeRuntimeWiring(TypeRuntimeWiring wiring) {
            this.typeRuntimeWirings.add(Objects.requireNonNull(wiring, "wiring"));
            return this;
        }

        public TypeDefinitionsWithRuntimeWiring build() {
            return new TypeDefinitionsWithRuntimeWiring(this.typeDefinitionRegistry, this.typeRuntimeWirings);
        }
    }
}
