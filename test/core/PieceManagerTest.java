package test.core;

import torrefactor.core.DataManager;
import torrefactor.core.PieceManager;
import torrefactor.util.Pair;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;
/*
public class PieceManagerTest {

    public static void main (String[] args) {
        org.junit.runner.JUnitCore.main("test.core.PieceManagerTest");
    }

    //           [----]    [----]
    //         10^  50^ 100^    ^140
    private PieceManager init() {
        File file = new File("data/test/dummy");
        Long fileSize = (long) (1 << 8);
        Pair<File, Long> fpair = new Pair<File, Long>(file, fileSize);
        List<Pair<File, Long>> fpairs = new ArrayList<Pair<File, Long>>(1);
        fpairs.add(fpair);
        int pieceLength = (1 << 4);
        byte[] fileHash = new byte[20];
        java.util.Arrays.fill(fileHash, (byte) 0);
        PieceManager p = null;
        try {
            p = new PieceManager(fpairs, pieceLength, fileHash);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        p.addBlock(0, 10, 41);
        p.addBlock(0, 100, 41);
        return p;
    }

    private void checkBounds(PieceManager p, int begin, int end) {
        //System.out.println("Expected end: " + end + " Found end: " + p.blockMap.get(begin));
        assertTrue(p.blockMap.get(begin) != null);
        assertTrue(p.blockMap.get(begin) == end);
    }

    //       [-] [----]    [----]
    //      0^ ^5
    @Test public void noOverlap() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 0, 6));
        checkBounds(p, 0, 5);
        checkBounds(p, 10, 50);
        checkBounds(p, 100, 140);
    }

    //       [---[-]--]    [----]   --> [--------]    [----]
    //      0^     ^30
    @Test public void overlapLeft() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 0, 31));
        checkBounds(p, 0, 50);
        checkBounds(p, 100, 140);
    }

    //       [---[----]-]  [----]   --> [----------]  [----]
    //      0^          ^70
    @Test public void overlapLeftRight() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 0, 71));
        checkBounds(p, 0, 70);
        checkBounds(p, 100, 140);
    }

    //       [---[----]----[----]-] --> [-------------------]
    //      0^                    ^200
    @Test public void englob() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 0, 201));
        checkBounds(p, 0, 200);
    }


    //           [-[-]]    [----]   -->    [----]    [----]
    //           20^ ^40
    @Test public void contained() {
        PieceManager p = init();
        assertFalse(p.addBlock(0, 20, 21));
        checkBounds(p, 10, 50);
        checkBounds(p, 100, 140);
    }

    //           [-[--]--] [----]   -->    [-------] [----]
    //           20^     ^70
    @Test public void overlapRight() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 20, 51));
        checkBounds(p, 10, 70);
        checkBounds(p, 100, 140);
    }

    //           [-[--]    [--]-]   -->    [--------------]
    //           20^          ^120
    @Test public void overlapRightLeft() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 20, 101));
        checkBounds(p, 10, 140);
    }

    //           [----[--] [----]   -->    [-------] [----]
    //              50^  ^70
    @Test public void overlapBeginRightBorder() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 50, 21));
        checkBounds(p, 10, 70);
        checkBounds(p, 100, 140);
    }

    //           [----[----]----]   -->    [--------------]
    //              50^    ^100
    @Test public void overlapRightBorderLeftBorder() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 50, 51));
        checkBounds(p, 10, 140);
    }

    //           [-]--]    [----]   -->    [----]    [----]
    //         10^ ^30
    @Test public void overlapLeftBorderContained() {
        PieceManager p = init();
        assertFalse(p.addBlock(0, 10, 30));
        checkBounds(p, 10, 50);
        checkBounds(p, 100, 140);
    }

    //           [----]-]  [----]   -->    [------]  [----]
    //         10^      ^60
    @Test public void overlapLeftBorderInside() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 10, 51));
        checkBounds(p, 10, 60);
        checkBounds(p, 100, 140);
    }

    //         [-[----]    [----]   -->  [------]    [----]
    //        0^ ^10
    @Test public void overlapLeftBorderOutside() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 0, 11));
        checkBounds(p, 0, 50);
        checkBounds(p, 100, 140);
    }

    //           [--|-]    [-|--]   -->    [--]        [--]
    //              ^20   120^
    @Test public void removeOverlapLeftRight() {
        PieceManager p = init();
        assertTrue(p.removeBlocks(0, 20, 101));
        checkBounds(p, 10, 19);
        checkBounds(p, 121, 140);
    }

    //           [--|--------|--]   -->    [--]        [--]
    //         10^  ^20   120^  ^140
    @Test public void removeOverlapRightLeft() {
        PieceManager p = init();
        assertTrue(p.addBlock(0, 10, 131));
        checkBounds(p, 10, 140);
        assertTrue(p.removeBlocks(0, 20, 101));
        checkBounds(p, 10, 19);
        checkBounds(p, 121, 140);
    }

    //           [|--|]    [----]   -->    []  []    [----]
    //          12^  ^38
    @Test public void removeContained() {
        PieceManager p = init();
        assertTrue(p.removeBlocks(0, 12, 27));
        checkBounds(p, 10, 11);
        checkBounds(p, 39, 50);
        checkBounds(p, 100, 140);
    }


    @Test public void removeNothing() {
        PieceManager p = init();
        assertFalse(p.removeBlocks(0, 500, 42));
    }
}
*/
