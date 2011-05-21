package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class represent a tracker.
 */
abstract class Tracker {
    public String uri;
    public String statusMessage;
    public enum Event { none, started, stopped, completed };

    protected long lastActive = 0;
    protected int interval = 1200;   // 20 min
    protected int port;
    protected int seeders = -1;
    protected int leechers = -1;
    protected int uniqKey;

    
    public Tracker () {}

    /**
     * Create a new tracker for the given uri
     *
     * @param _uri    the uri of the tracker
     */
    public Tracker (String _uri) {}

    /**
     * Create a new tracker for the given uri and use a uniqKey
     *
     * @param _uri        the uri of the tracker
     * @param uniqKey    our own uniqKey
     */
    public Tracker (String _uri, int uniqKey) {}

    /**
     * Announce an event to the tracker.
     *
     * @param torrent    the torrent concerned by the event
     * @param event        the event to announce
     */
    abstract ArrayList<Pair<byte[], Integer>>
        announce (Torrent torrent, Event event)
        throws IOException, InvalidBDecodeException;

    /**
     * Return the inverval at which we should announce.
     */
    public int getInverval () {
        return this.interval;
    }

    /**
     * Return the time at which the last announce was made.
     */
    public long getLastActive () {
        return this.lastActive;
    }

    /**
     * Return the time at which we should do the next announce.
     */
    public long getNextAnnounceTime () {
        return this.lastActive + (((long) this.interval) * 1000);
    }

    /**
     * Return true if we are allowed to announce again.
     */
    public boolean canAnnounce () {
        if (System.currentTimeMillis() < this.lastActive
                                         + (((long) this.interval) * 1000)) {
            return false;
        }
        return true;
    }

    protected void updateActive () {
        this.lastActive = System.currentTimeMillis();
    }

}
