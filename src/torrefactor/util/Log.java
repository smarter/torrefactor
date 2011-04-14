package torrefactor.util;

import torrefactor.util.Triplet;
import torrefactor.util.Quad;
import torrefactor.util.GenericArray;

import java.util.*;
import java.io.*;

public class Log {
    private static Log instance;
    private PrintStream printStream = System.err;
    private GenericArray<Quad<Integer, Long, Object, String>> ring;
    private boolean printAdded = true;
    private int enableMask;
    private int disableMask;
    private int reservedBits = 255;
    public static final int ERROR = 1;
    public static final int WARNING = 1 << 1;
    public static final int INFO = 1 << 2;
    public static final int DEBUG = 1 << 3;

    // indicate where the next element will be put
    private int ringPosition = 0;

    public Log () {
        if (instance != null) {
            System.err.println ("Creating a new instance of Log while an "
                                + "instance already exist. It's probably not "
                                + "what you want.");
        }
        this.ring = new GenericArray<Quad<Integer, Long, Object, String>> (50);
        this.enableMask = ERROR | WARNING | INFO | DEBUG;
        this.disableMask = 0;
    }

    /* alias for get instance */
    public static Log i () {
        return getInstance ();
    }

    public static Log getInstance () {
        synchronized (Log.class) {
            if (instance == null) {
                instance = new Log ();
            }
        }
        return instance;
    }

    public void setStream (PrintStream stream) {
        this.printStream = stream;
    }

    public void setStream (OutputStream stream) {
        this.printStream = new PrintStream (stream);
    }

    public void setEnableMask (int mask) {
        this.enableMask = mask;
    }
    
    public void setDisableMask (int mask) {
        this.disableMask = mask;
    }

    public int getUnusedBitForMask () {
        int mask = 1;
        while (mask != 1 << 31) {
            if ((this.reservedBits & mask) == 0) {
                this.reservedBits = this.reservedBits | mask;
                return mask;
            }
            mask = mask << 1;
        }
        System.err.println ("Log: no free bit.");
        return 0;
    }

    public void log (int level, Object object, String message) {
        Quad<Integer, Long, Object, String> quad =
            new Quad<Integer, Long, Object, String> (level,
                                                     System.currentTimeMillis(),
                                                     object,
                                                     message);
        synchronized (this) {
            this.ring.put (this.ringPosition, quad);
            this.ringPosition ++;
            if (ringPosition >= this.ring.length) {
                this.ringPosition = 0;
            }
        }
        if (this.printAdded) print (quad);
    }

    private void print (Quad<Integer, Long, Object, String> quad) {
        print (quad, this.enableMask, this.disableMask);
    }

    private void print (Quad<Integer, Long, Object, String> quad,
                        int enableMask, int disableMask) {
        if (quad == null) return;
        if ((quad.first () & enableMask & (~disableMask)) == 0) {
            return;
        }
        this.printStream.print ("[" + quad.second () + "]" + "["
                                + quad.third ().getClass (). getName () + "]"
                                + " " + quad.fourth () + "\n");
    }

    public void printRing () {
        printRing (this.enableMask, this.disableMask);
    }

    /* Prints all message in the ring regardless of their flags. */
    public void printRingAll () {
        printRing (~0, 0);
    }

    /* Prints all message in the ring matching the specified flags. */
    public void printRing (int enableMask, int disableMask) {
        int current = this.ringPosition;
        int end = current - 1;
        if (end == -1) end = this.ring.length - 1;
        while (current != end) {
            print (this.ring.get (current), enableMask, disableMask);
            current ++;
            if (current == this.ring.length) {
                current = 0;
            }
        }
        print (this.ring.get (current));
    }

    // Print at most the last num elements from the ring or all elements if num
    // is less than 1.
    public void printRing (int num) {
        if (num > this.ring.length || num < 1) {
            printRing ();
            return;
        }
        int end = this.ringPosition - 1;
        int current = this.ringPosition - num;
        if (current < 0) current = this.ring.length - current;
        if (end == -1) end = this.ring.length - 1;
        while (current != end) {
            print (this.ring.get (current));
            current ++;
            if (current == this.ring.length) {
                current = 0;
            }
        }
        print (this.ring.get (current));
    }
}

