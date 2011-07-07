/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

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
 *                  |
 *                  v
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
class SawToothIntervalMap {
    private IntervalMap[] maps;

    private int halvingThresold;
    private int cur;


    /**
     * Create a new SawToothIntervalMap with the specified halving thresold.
     */
    SawToothIntervalMap(int _halvingThresold) {
        this.maps = new IntervalMap[] { new IntervalMap(), new IntervalMap() };
        this.cur = 0;
        this.halvingThresold = _halvingThresold;
    }

    /**
     * Returns the halving thresold of this map.
     */
    public int halvingThresold() {
        return this.halvingThresold;
    }

    /**
     * See IntervalMap.addInterval()
     */
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

    /**
     * See IntervalMap.removeIntervals()
     */
    public boolean removeIntervals(long begin, int length) {
        boolean fst = this.maps[0].removeIntervals(begin, length);
        boolean snd = this.maps[1].removeIntervals(begin, length);
        return (fst || snd);
    }

    /**
     * See IntervalMap.nextFreePoint()
     * The time complexities are the same except for
     * the worst case which is O(n^2*log(n)) but really unlikely
     */
    public long nextFreePoint(long offset) {
        long sndOffset;
        do {
            sndOffset = this.maps[0].nextFreePoint(offset);
            offset = this.maps[1].nextFreePoint(sndOffset);
        } while (offset != sndOffset);

        return offset;
    }

    /**
     * See IntervalMap.containsInterval()
     */
    public boolean containsInterval(long begin, int length) {
        return (this.maps[0].containsInterval(begin, length)
               || this.maps[1].containsInterval(begin, length));
    }

    /**
     * Remove the first halvingThresold()/2 entries of the map
     * or less if the map doesn't have that many entries yet
     */
    public void clearFirstHalf() {
        // The oldest entries are always in the other map(1 - cur)
        // except if we haven't used it yet.
        if (this.maps[1-cur].size() != 0) {
            this.maps[1-cur] = new IntervalMap();
        } else {
            this.maps[cur] = new IntervalMap();
        }
    }

    public String toString() {
        return this.maps[0].toString() + this.maps[1].toString();
    }
}
