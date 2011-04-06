package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.Pair;

import java.io.*;
import java.util.*;
import java.security.*;

/**
 * A map where keys are beginning of non-overlapping intervals and values are end
 * To prevent overlapping, put() is not available, instead use addInterval.
 * Implemented with a TreeMap to get O(log(n)) lookup, insert, remove, previous and successor
 *
 * Note that this class is NOT thread-safe, thread-safe functions are marked as such.
 * To achieve thread-safeness, you will need to put calls to not thread-safe functions
 * in a synchronized block where the synchronization object is the instance of this class you're
 * using.
 */
public class IntervalMap extends TreeMap<Integer, Integer> {

    /**
     * This function is thread-safe.
     */
    public synchronized boolean addInterval(int begin, int length) {
        int end =  begin + length - 1;
        Map.Entry<Integer, Integer> interval = floorEntry(end + 1);
        if (interval == null) {
            // No interval beginning before our superior born, so no overlap possible
            super.put(begin, end);
            return true;
        }
        if (interval.getKey() <= begin && interval.getValue() >= end) {
            //We're included in an interval, nothing to add
            return false;
        }
        if (interval.getValue() + 1 >= begin) {
            // If our right side overlaps with an interval B, extend our superior born to B's
            end = Math.max(end, interval.getValue());
            // Discard all intervals contained in our interval
            while (interval != null && interval.getKey() > begin) {
                remove(interval.getKey());
                interval = lowerEntry(interval.getKey());
            }
            // If our left side overlaps with or is adjacent to an interval B, extend B's superior born to ours
            if (interval != null && interval.getValue() + 1 >= begin && interval.getValue() <= end) {
                super.put(interval.getKey(), end);
                return true;
            }
        }
        // The interval before our left side doesn't overlap with us or is adjacent to us
        super.put(begin, end);
        return true;
    }

    /**
     * Remove intervals, intervals overlapping but not contained between
     * begin and begin + length - 1 will be shrunk
     *
     * This function is thread-safe.
     */
    public synchronized boolean removeIntervals(int begin, int length) {
        int end = begin + length - 1;
        Map.Entry<Integer, Integer> interval = floorEntry(end);
        if (interval == null || interval.getValue() < begin) {
            return false;
        }
        if (interval.getValue() > end) {
            super.put(end + 1, interval.getValue());
        }
        while (interval != null && interval.getKey() >= begin) {
            remove(interval.getKey());
            interval = lowerEntry(interval.getKey());
        }
        if (interval != null) {
            super.put(interval.getKey(), begin - 1);
        }
        return true;
    }

    /**
     * Return the point bigger than or equal to point
     * that is not contained in any interval or -1 if
     * there is no such point.
     *
     * This function is thread-safe.
     */
    public Integer nextFreePoint(int point) {
        Map.Entry<Integer, Integer> interval = null;
        synchronized (this) {
            interval = floorEntry(point);
        }
        if (interval == null) {
            return point;
        }
        int end = interval.getValue();
        if (end < point) {
            return point;
        }
        return end + 1;
    }

    /**
     * Returns whether or not the interval between begin
     * and begin + length - 1 is contained inside an existing
     * interval in the map.
     *
     * This function is thread-safe.
     */
    public boolean containsInterval(int begin, int length) {
        Map.Entry<Integer, Integer> interval = null;
        synchronized (this) {
            interval = floorEntry(begin);
        }
        int end = begin + length - 1;
        if (interval == null || interval.getValue() < end
            || (interval.getValue() - begin + 1) < length) {
            return false;
        }
        return true;
    }
}
