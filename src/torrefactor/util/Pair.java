package torrefactor.util;

import java.io.*;

/**
 * An immutable class to store a pair of objects.
 */
public class Pair<F, S> implements Serializable {
    private final F first;
    private final S second;
    private static final long serialVersionUID = 1L;

    /**
     * Creates a pair with the following invariants:
     * new Pair(f,s).first() == f;
     * new Pair(f,s).second() == s;
     * @param first The first element of the pair
     * @param second The second element of the pair
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first element of the pair.
     */
    public F first() {
        return this.first;
    }

    /**
     * Returns the second element of the pair.
     */
    public S second() {
        return this.second;
    }
}
