package graphql.nadel.dsl;

import graphql.Internal;

import java.util.Objects;

@Internal
public class TypeMappingDefinition {
    private final String underlyingName;
    private final String overallName;

    public TypeMappingDefinition(String underlyingName, String overallName) {
        this.underlyingName = underlyingName;
        this.overallName = overallName;
    }

    public String getUnderlyingName() {
        return underlyingName;
    }

    public String getOverallName() {
        return overallName;
    }

    @Override
    public String toString() {
        return "TypeMappingDefinition{" +
            "underlyingName='" + underlyingName + '\'' +
            ", overallName='" + overallName + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeMappingDefinition that = (TypeMappingDefinition) o;
        return Objects.equals(underlyingName, that.underlyingName) && Objects.equals(overallName, that.overallName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(underlyingName, overallName);
    }
}
