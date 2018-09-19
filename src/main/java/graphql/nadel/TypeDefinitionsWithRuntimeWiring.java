package graphql.nadel;

import graphql.PublicApi;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Stores {@link TypeDefinitionRegistry} and collection of {@link TypeRuntimeWiring}s.
 */
@PublicApi
public class TypeDefinitionsWithRuntimeWiring {
    private final TypeDefinitionRegistry typeDefinitionRegistry;
    private final Consumer<RuntimeWiring.Builder> runtimeWiringConsumer;

    private TypeDefinitionsWithRuntimeWiring(TypeDefinitionRegistry typeDefinitionRegistry,
                                             Consumer<RuntimeWiring.Builder> runtimeWiringConsumer) {
        this.typeDefinitionRegistry = typeDefinitionRegistry;
        this.runtimeWiringConsumer = runtimeWiringConsumer;
    }

    public static Builder newTypeDefinitionWithRuntimeWiring() {
        return new Builder();
    }

    public TypeDefinitionRegistry typeDefinitionRegistry() {
        return typeDefinitionRegistry;
    }

    public Consumer<RuntimeWiring.Builder> runtimeWiringConsumer() {
        return runtimeWiringConsumer;
    }

    public static class Builder {
        private TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        private Consumer<RuntimeWiring.Builder> runtimeWiringConsumer = builder -> {
        };

        private Builder() {
        }

        public Builder typeDefinitionRegistry(TypeDefinitionRegistry registry) {
            this.typeDefinitionRegistry = Objects.requireNonNull(registry, "registry");
            return this;
        }

        public Builder runtimeWiringConsumer(Consumer<RuntimeWiring.Builder> runtimeWiringConsumer) {
            this.runtimeWiringConsumer = Objects.requireNonNull(runtimeWiringConsumer, "runtimeWiringConsumer");
            return this;
        }

        public TypeDefinitionsWithRuntimeWiring build() {
            return new TypeDefinitionsWithRuntimeWiring(this.typeDefinitionRegistry, this.runtimeWiringConsumer);
        }
    }
}
