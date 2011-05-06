package torrefactor.core;

/**
 * An IntervalMap with a limited number of elements specified at the object
 * creation. Once the number of elements reaches halvingThresold(), the next
 * element inserted will cause the first halvingThresold()/2 elements inserted to
 * be discarded and marked for garbage collection.  The discarding is done in
 * constant time.
 *
 * Internally, this is implemented by maintaining two instances of IntervalMap
 * and querying both when needed. This only changes the complexity of the
 * IntervalMap operations by a constant factor. Except for the nextFreePoint()
 * operation whose worst case goes from O(n*log(n)) to O(n^2*log(n)) but this
 * is unlikely to happen in practice and not a problem for small maps.
 *
 * Here's a handy graph showing the number of elements in the map during
 * normal operation of this class:
 * <pre>
 *                skier
 *  			    |
 *  			    v
 * elements in map
 *    ↑——————————————————————————————————————————————————  ← halving threshold
 *    │                   /|                 _/|
 *    │                  / |             ___/  |
 *    │             ☺   /  |            /      |
 *    │            (|) /   |           /       |
 *    │            ↙↙ /    |       ___/        |     _/
 *    │              /     |      /            |    /
 *    │             /      |     /             | __/
 *    │            /       |____/              |/           ←——— Half
 *    │           /
 *    │          /
 *    │         /
 *    │        /
 *    │     __/
 *    │   _/
 *    │  /
 *    │_/
 *    └—————————————————————————————————————————————————→ time
 *      ↑↑ ↑  ↑↑↑↑↑↑↑↑↑↑↑↑↑     ↑↑↑   ↑↑↑   ↑ ↑↑↑  ↑↑ ↑
 *
 * ↑ = insertion
 *
 * ( Skier on the graph of the elements in this map is merely a feature of your
 *   imagination and is in any case not a feature of SawtoothIntervalMap ;) )
 * </pre>
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

    public boolean addInterval(long begin, int length) {
        this.maps[cur].addInterval(begin, length);
        if (this.maps[cur].size() >= halvingThresold/2) {
            cur = 1 - cur;
            if (this.maps[cur].size() != 0) {
                this.maps[cur] = new IntervalMap();
            }
        }
        return this.maps[cur].addInterval(begin, length);
    }

    public boolean removeIntervals(long begin, int length) {
        boolean fst = this.maps[0].removeIntervals(begin, length);
        boolean snd = this.maps[1].removeIntervals(begin, length);
        return (fst || snd);
    }

    public long nextFreePoint(long offset) {
        long sndOffset;
        do {
            sndOffset = this.maps[0].nextFreePoint(offset);
            offset = this.maps[1].nextFreePoint(sndOffset);
        } while (offset != sndOffset);

        return offset;
    }

    public boolean containsInterval(long begin, int length) {
        return (this.maps[0].containsInterval(begin, length)
               || this.maps[1].containsInterval(begin, length));
    }
}
