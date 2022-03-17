package graphql.nadel.dsl;

import graphql.Internal;

import java.util.Objects;

@Internal
public class RemoteArgumentDefinition {
    private final String name;
    private final RemoteArgumentSource remoteArgumentSource;

    public RemoteArgumentDefinition(
        String name,
        RemoteArgumentSource remoteArgumentSource
    ) {
        this.name = name;
        this.remoteArgumentSource = remoteArgumentSource;
    }

    public String getName() {
        return name;
    }

    public RemoteArgumentSource getRemoteArgumentSource() {
        return remoteArgumentSource;
    }

    @Override
    public String toString() {
        return "RemoteArgumentDefinition{" +
            "name='" + name + '\'' +
            ", remoteArgumentSource=" + remoteArgumentSource +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteArgumentDefinition that = (RemoteArgumentDefinition) o;
        return Objects.equals(name, that.name)
            && Objects.equals(remoteArgumentSource, that.remoteArgumentSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, remoteArgumentSource);
    }
}
