package graphql.nadel;

import graphql.Internal;

import java.util.Objects;


@Internal
public class NadelTuple2<T1, T2> {


    private final T1 t1;
    private final T2 t2;

    NadelTuple2(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {
        return t1;
    }

    public T2 getT2() {
        return t2;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NadelTuple2<?, ?> nadelTuple2 = (NadelTuple2<?, ?>) o;
        return Objects.equals(t1, nadelTuple2.t1) &&
                Objects.equals(t2, nadelTuple2.t2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }


    @Override
    public String toString() {
        return "NadelTuple2{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }
}
