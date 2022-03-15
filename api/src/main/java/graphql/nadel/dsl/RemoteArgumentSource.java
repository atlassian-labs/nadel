package graphql.nadel.dsl;

import graphql.Internal;

import java.util.List;
import java.util.Objects;

@Internal
public class RemoteArgumentSource {
    public enum SourceType {
        OBJECT_FIELD,
        FIELD_ARGUMENT,
    }

    private final String argumentName;

    // for OBJECT_FIELD
    private final List<String> pathToField;

    private final SourceType sourceType;

    public RemoteArgumentSource(String argumentName, List<String> pathToField, SourceType sourceType) {
        this.argumentName = argumentName;
        this.pathToField = pathToField;
        this.sourceType = sourceType;
    }

    public List<String> getPathToField() {
        return pathToField;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    @Override
    public String toString() {
        return "RemoteArgumentSource{" +
            "argumentName='" + argumentName + '\'' +
            ", pathToField=" + pathToField +
            ", sourceType=" + sourceType +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteArgumentSource that = (RemoteArgumentSource) o;
        return Objects.equals(argumentName, that.argumentName)
            && Objects.equals(pathToField, that.pathToField)
            && sourceType == that.sourceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(argumentName, pathToField, sourceType);
    }
}
