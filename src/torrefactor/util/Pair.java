package torrefactor.util;

import java.io.*;


public class Pair<F, S> implements Serializable {
    private final F first;
    private final S second;
    private static final long serialVersionUID = 1L;

    public Pair(F _first, S _second) {
        this.first = _first;
        this.second = _second;
    }

    public F first() {
        return this.first;
    }

    public S second() {
        return this.second;
    }
}
