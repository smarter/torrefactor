package torrefactor.util;

import java.io.*;


public class Quad<F, S, T, O> implements Serializable {
    private final F first;
    private final S second;
    private final T third;
    private final O fourth;
    private static final long serialVersionUID = 1L;

    public Quad(F _first, S _second, T _third, O _fourth) {
        this.first = _first;
        this.second = _second;
        this.third = _third;
        this.fourth = _fourth;
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

    public O fourth() {
        return this.fourth;
    }
}
