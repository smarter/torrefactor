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

package test.core;

import torrefactor.core.IntervalMap;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

public class IntervalMapTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.core.IntervalMapTest");
    }

    //           [----]    [----]
    //         10^  50^ 100^    ^140
    private IntervalMap init() {
        IntervalMap m = new IntervalMap();
        m.addInterval(10, 41);
        m.addInterval(100, 41);
        return m;
    }

    private void checkBounds(IntervalMap m, long begin, long end) {
        //System.out.println("Expected end: " + end + " Found end: " + m.get(begin));
        assertTrue(m.get(begin) != null);
        assertTrue(m.get(begin) == end);
    }

    //       [-] [----]    [----]
    //      0^ ^5
    @Test public void noOverlap() {
        IntervalMap m = init();
        assertTrue(m.addInterval(0, 6));
        checkBounds(m, 0, 5);
        checkBounds(m, 10, 50);
        checkBounds(m, 100, 140);
    }

    //       [---[-]--]    [----]   --> [--------]    [----]
    //      0^     ^30
    @Test public void overlapLeft() {
        IntervalMap m = init();
        assertTrue(m.addInterval(0, 31));
        checkBounds(m, 0, 50);
        checkBounds(m, 100, 140);
    }

    //       [---[----]-]  [----]   --> [----------]  [----]
    //      0^          ^70
    @Test public void overlapLeftRight() {
        IntervalMap m = init();
        assertTrue(m.addInterval(0, 71));
        checkBounds(m, 0, 70);
        checkBounds(m, 100, 140);
    }

    //       [---[----]----[----]-] --> [-------------------]
    //      0^                    ^200
    @Test public void englob() {
        IntervalMap m = init();
        assertTrue(m.addInterval(0, 201));
        checkBounds(m, 0, 200);
    }


    //           [-[-]]    [----]   -->    [----]    [----]
    //           20^ ^40
    @Test public void contained() {
        IntervalMap m = init();
        assertFalse(m.addInterval(20, 21));
        checkBounds(m, 10, 50);
        checkBounds(m, 100, 140);
    }

    //           [-[--]--] [----]   -->    [-------] [----]
    //           20^     ^70
    @Test public void overlapRight() {
        IntervalMap m = init();
        assertTrue(m.addInterval(20, 51));
        checkBounds(m, 10, 70);
        checkBounds(m, 100, 140);
    }

    //           [-[--]    [--]-]   -->    [--------------]
    //           20^          ^120
    @Test public void overlapRightLeft() {
        IntervalMap m = init();
        assertTrue(m.addInterval(20, 101));
        checkBounds(m, 10, 140);
    }

    //           [----[--] [----]   -->    [-------] [----]
    //              50^  ^70
    @Test public void overlapBeginRightBorder() {
        IntervalMap m = init();
        assertTrue(m.addInterval(50, 21));
        checkBounds(m, 10, 70);
        checkBounds(m, 100, 140);
    }

    //           [----[----]----]   -->    [--------------]
    //              50^    ^100
    @Test public void overlapRightBorderLeftBorder() {
        IntervalMap m = init();
        assertTrue(m.addInterval(50, 51));
        checkBounds(m, 10, 140);
    }

    //           [-]--]    [----]   -->    [----]    [----]
    //         10^ ^30
    @Test public void overlapLeftBorderContained() {
        IntervalMap m = init();
        assertFalse(m.addInterval(10, 30));
        checkBounds(m, 10, 50);
        checkBounds(m, 100, 140);
    }

    //           [----]-]  [----]   -->    [------]  [----]
    //         10^      ^60
    @Test public void overlapLeftBorderInside() {
        IntervalMap m = init();
        assertTrue(m.addInterval(10, 51));
        checkBounds(m, 10, 60);
        checkBounds(m, 100, 140);
    }

    //         [-[----]    [----]   -->  [------]    [----]
    //        0^ ^10
    @Test public void overlapLeftBorderOutside() {
        IntervalMap m = init();
        assertTrue(m.addInterval(0, 11));
        checkBounds(m, 0, 50);
        checkBounds(m, 100, 140);
    }

    //           [--|-]    [-|--]   -->    [--]        [--]
    //              ^20   120^
    @Test public void removeOverlapLeftRight() {
        IntervalMap m = init();
        assertTrue(m.removeIntervals(20, 101));
        checkBounds(m, 10, 19);
        checkBounds(m, 121, 140);
    }

    //           [--|--------|--]   -->    [--]        [--]
    //         10^  ^20   120^  ^140
    @Test public void removeOverlapRightLeft() {
        IntervalMap m = init();
        assertTrue(m.addInterval(10, 131));
        checkBounds(m, 10, 140);
        assertTrue(m.removeIntervals(20, 101));
        checkBounds(m, 10, 19);
        checkBounds(m, 121, 140);
    }

    //           [|--|]    [----]   -->    []  []    [----]
    //          12^  ^38
    @Test public void removeContained() {
        IntervalMap m = init();
        assertTrue(m.removeIntervals(12, 27));
        checkBounds(m, 10, 11);
        checkBounds(m, 39, 50);
        checkBounds(m, 100, 140);
    }


    @Test public void removeNothing() {
        IntervalMap m = init();
        assertFalse(m.removeIntervals(500, 42));
    }
}
