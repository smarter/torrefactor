package torrefactor.util;

import torrefactor.util.Triplet;
import torrefactor.util.Quad;
import torrefactor.util.GenericArray;

import java.util.*;
import java.io.*;

/**
 * This class manage a ring containing the log message. It can displays them as
 * they are added. It can also display the ring whenever you want.
 *
 * This class as some functionality that permits to enable/disable the logged
 * message with flags (1 bit = 1 flag). But this functionality has finally not
 * been used in the project and is therefore not well documented and not
 * tested.
 */
public class Log {
    private static Log instance;
    private PrintStream printStream = System.err;
    private GenericArray<Quad<Integer, Long, Object, String>> ring;
    private boolean printAdded = true;
    private int enableMask;
    private int disableMask;
    private int reservedBits = 255;
    private long refTime = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 1 << 1;
    public static final int INFO = 1 << 2;
    public static final int DEBUG = 1 << 3;

    // indicate where the next element will be put
    private int ringPosition = 0;

    private Log () {
        if (instance != null) {
            System.err.println ("Creating a new instance of Log while an "
                                + "instance already exist. It's probably not "
                                + "what you want.");
        }
        this.ring = new GenericArray<Quad<Integer, Long, Object, String>> (50);
        this.enableMask = ERROR | WARNING | INFO | DEBUG;
        this.disableMask = 0;
        this.refTime = System.currentTimeMillis ();
        log (INFO, this, "Logging started at " + this.refTime);
    }

    /**
     * Return the instance of this class if it exists or create a new instance
     * and return it.
     */
    public static Log getInstance () {
        synchronized (Log.class) {
            if (instance == null) {
                instance = new Log ();
            }
        }
        return instance;
    }

    /**
     * Set the stream where the message will be printed.
     */
    public void setStream (PrintStream stream) {
        this.printStream = stream;
    }

    /**
     * Set the stream where the message will be printed.
     */
    public void setStream (OutputStream stream) {
        this.printStream = new PrintStream (stream);
    }

    /**
     * Set the mask which select enabled messages.
     */
    public void setEnableMask (int mask) {
        this.enableMask = mask;
    }
    
    /**
     * Set the mask which disable message.
     */
    public void setDisableMask (int mask) {
        this.disableMask = mask;
    }

    /**
     * Return the next unused bit in mask.
     */
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

    /**
     * Log a message.
     *
     * @param level     the mask of the message
     * @param object    the header of the message
     * @param message   the message to log
     */
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

    /**
     * Print the specified message.
     */
    private void print (Quad<Integer, Long, Object, String> quad) {
        print (quad, this.enableMask, this.disableMask);
    }

    /**
     * Print the specified message.
     */
    private void print (Quad<Integer, Long, Object, String> quad,
                        int enableMask, int disableMask) {
        if (quad == null) return;
        if ((quad.first () & enableMask & (~disableMask)) == 0) {
            return;
        }

        String header;
        Class headerClass = quad.third().getClass();
        if (headerClass == String.class) {
            header = (String) quad.third();
        } else {
            header = headerClass.getName();
        }

        this.printStream.print ("[" + (quad.second () - this.refTime) + "]"
                                + "[" + header + "]"
                                + " " + quad.fourth () + "\n");
    }

    /**
     * Print the ring of message.
     */
    public void printRing () {
        printRing (this.enableMask, this.disableMask);
    }

    /**
     * Prints all message in the ring regardless of their flags.
     */
    public void printRingAll () {
        printRing (~0, 0);
    }

    /**
     * Prints all message in the ring matching the specified flags.
     */
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

    /**
     * Print at most the last num elements from the ring or all elements if num
     *is less than 1.
     */
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

