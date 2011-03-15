package torrefactor.util;


public class Pair<F, S> {
    private final F first;
    private final S second;

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
