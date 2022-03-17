package graphql.nadel.dsl;

import graphql.Internal;

import java.util.List;
import java.util.Objects;

@Internal
public class FieldMappingDefinition {
    private final List<String> inputPath;

    public FieldMappingDefinition(List<String> inputPath) {
        this.inputPath = inputPath;
    }

    public List<String> getInputPath() {
        return inputPath;
    }

    @Override
    public String toString() {
        return "FieldMappingDefinition{" +
            "inputPath=" + inputPath +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldMappingDefinition that = (FieldMappingDefinition) o;
        return Objects.equals(inputPath, that.inputPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputPath);
    }
}
