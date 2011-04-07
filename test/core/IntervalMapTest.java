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

    private void checkBounds(IntervalMap m, int begin, int end) {
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
