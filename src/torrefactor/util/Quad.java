package torrefactor.util;

import java.io.*;

/**
 * An immutable class to store a quadruplet of objects.
 */
public class Quad<F, S, T, O> implements Serializable {
    private final F first;
    private final S second;
    private final T third;
    private final O fourth;
    private static final long serialVersionUID = 1L;

    /**
     * Creates a triplet with the following invariants:
     * new Quad(f,s,t,o).first() == f;
     * new Quad(f,s,t,o).second() == s;
     * new Quad(f,s,t,o).third() == t;
     * new Quad(f,s,t,o).quad() == o;
     * @param first The first element of the quadruplet
     * @param second The second element of the quadruplet
     * @param third The third element of the quadruplet
     * @param fourth the fourth element of the quadruplet
     */
    public Quad(F first, S second, T third, O fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    /**
     * Returns the first element of the quadruplet
     */
    public F first() {
        return this.first;
    }

    /**
     * Returns the second element of the quadruplet
     */
    public S second() {
        return this.second;
    }

    /**
     * Returns the third element of the quadruplet
     */
    public T third() {
        return this.third;
    }

    /**
     * Returns the fourth element of the quadruplet
     */ 
    public O fourth() {
        return this.fourth;
    }
}
