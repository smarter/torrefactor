package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

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
    public Tracker (String _uri) {}
    public Tracker (String _uri, int uniqKey) {}
    abstract ArrayList<Pair<byte[], Integer>>
        announce (Torrent torrent, Event event)
        throws IOException, InvalidBDecodeException;


    public int getInverval () {
        return this.interval;
    }

    public long getLastActive () {
        return this.lastActive;
    }

    public long getNextAnnounceTime () {
        return this.lastActive + ((long) this.interval * 1000);
    }

    public boolean canAnnounce () {
        if (System.currentTimeMillis() < this.lastActive
                                         + ((long) this.interval * 1000)) {
            return false;
        }
        return true;
    }

    protected void updateActive () {
        this.lastActive = System.currentTimeMillis();
    }

}
