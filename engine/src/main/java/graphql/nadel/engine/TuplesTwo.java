package graphql.nadel.engine;

import graphql.Internal;

import java.util.Objects;


@Internal
public class TuplesTwo<T1, T2> {


    private final T1 t1;
    private final T2 t2;

    TuplesTwo(T1 t1, T2 t2) {
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
        TuplesTwo<?, ?> tuplesTwo = (TuplesTwo<?, ?>) o;
        return Objects.equals(t1, tuplesTwo.t1) &&
                Objects.equals(t2, tuplesTwo.t2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }


    @Override
    public String toString() {
        return "TuplesTwo{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }
}
