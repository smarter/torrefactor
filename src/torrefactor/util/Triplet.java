package torrefactor.util;

import java.io.*;

/**
 * An immutable class to store a triplet of objects.
 */
public class Triplet<F, S, T> implements Serializable {
    private final F first;
    private final S second;
    private final T third;
    private static final long serialVersionUID = 1L;

    /**
     * Creates a triplet with the following invariants:
     * new Triplet(f,s,t).first() == f;
     * new Triplet(f,s,t).second() == s;
     * new Triplet(f,s,t).third() == t;
     * @param first The first element of the triplet
     * @param second The second element of the triplet
     * @param third The third element of the triplet
     */
    public Triplet(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Returns the first element of the triplet
     */
    public F first() {
        return this.first;
    }

    /**
     * Returns the second element of the triplet
     */
    public S second() {
        return this.second;
    }

    /**
     * Returns the third element of the triplet
     */
    public T third() {
        return this.third;
    }
}
