package graphql.nadel;

import graphql.Internal;

import java.util.Objects;


@Internal
public class TuplesThree<T1, T2, T3> {


    private final T1 t1;
    private final T2 t2;
    private final T3 t3;

    TuplesThree(T1 t1, T2 t2, T3 t3) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

    public T1 getT1() {
        return t1;
    }

    public T2 getT2() {
        return t2;
    }

    public T3 getT3() {
        return t3;
    }


    @Override
    public String toString() {
        return "TuplesThree{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                ", t3=" + t3 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TuplesThree<?, ?, ?> that = (TuplesThree<?, ?, ?>) o;
        return Objects.equals(getT1(), that.getT1()) && Objects.equals(getT2(), that.getT2()) && Objects.equals(getT3(), that.getT3());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getT1(), getT2(), getT3());
    }
}
