package torrefactor.core;

/**
 * An IntervalMap with a limited number of elements specified at the object
 * creation. Once the number of elements reaches 2*halvingThresold(), the next
 * element inserted will cause the first halvingThresold() elements inserted to
 * be discarded and marked for garbage collection.  The discarding is done in
 * constant time.
 *
 * Internally, this is implemented by maintaining two instances of IntervalMap
 * and querying both when needed. This only changes the complexity of the
 * IntervalMap operations by a constant factor. Except for the nextFreePoint()
 * operation whose worst case goes from O(n*log(n)) to O(n^2*log(n)) but this
 * is unlikely to happen in practice and not a problem for small maps.
 */
class SawToothIntervalMap implements java.io.Serializable {
    private IntervalMap[] maps;

    private int halvingThresold;
    private int cur;

    SawToothIntervalMap(int _halvingThresold) {
        this.maps = new IntervalMap[] { new IntervalMap(), new IntervalMap() };
        this.cur = 0;
        this.halvingThresold = _halvingThresold;
    }

    public int halvingThresold() {
        return this.halvingThresold;
    }

    public boolean addInterval(int begin, int length) {
        this.maps[cur].addInterval(begin, length);
        if (this.maps[cur].size() >= halvingThresold/2) {
            cur = 1 - cur;
            if (this.maps[cur].size() != 0) {
                this.maps[cur] = new IntervalMap();
            }
        }
        return this.maps[cur].addInterval(begin, length);
    }

    public boolean removeIntervals(int begin, int length) {
        boolean fst = this.maps[0].removeIntervals(begin, length);
        boolean snd = this.maps[1].removeIntervals(begin, length);
        return (fst || snd);
    }

    public int nextFreePoint(int offset) {
        int sndOffset;
        do {
            sndOffset = this.maps[0].nextFreePoint(offset);
            offset = this.maps[1].nextFreePoint(sndOffset);
        } while (offset != sndOffset);

        return offset;
    }

    public boolean containsInterval(int begin, int length) {
        return (this.maps[0].containsInterval(begin, length)
               || this.maps[1].containsInterval(begin, length));
    }
}
