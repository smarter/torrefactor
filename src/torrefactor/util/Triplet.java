package torrefactor.util;

import java.io.*;


public class Triplet<F, S, T> implements Serializable {
    private final F first;
    private final S second;
    private final T third;
    private static final long serialVersionUID = 1L;

    public Triplet(F _first, S _second, T _third) {
        this.first = _first;
        this.second = _second;
        this.third = _third;
    }

    public F first() {
        return this.first;
    }

    public S second() {
        return this.second;
    }

    public T third() {
        return this.third;
    }
}
