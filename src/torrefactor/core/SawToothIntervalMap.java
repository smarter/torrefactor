package torrefactor.core;

class SawToothIntervalMap implements java.io.Serializable {
    private IntervalMap[] maps;

    private int halvingThresold;
    private int cur;

    SawToothIntervalMap(int _halvingThresold) {
        this.maps = new IntervalMap[] { new IntervalMap(), new IntervalMap() };
        this.cur = 0;
        this.halvingThresold = _halvingThresold;
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
