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
    protected int interval = 1200;
    protected int port;
    protected int seeders = -1;
    protected int leechers = -1;


    public Tracker () {} 
    public Tracker (String _uri) {}
    abstract ArrayList<ArrayList> announce (Torrent torrent, Event event)
    throws IOException, InvalidBencodeException;


    public int getInverval () {
        return this.interval;
    }

    public long getLastActive () {
        return this.lastActive;
    }

    public long getNextAnnounceTime () {
        return this.lastActive + (long) this.interval;
    }

    public boolean canAnnounce () {
        if (new Date().getTime() < this.lastActive + (long) this.interval) {
            return false;
        }
        return true;
    }

    protected void updateActive () {
        this.lastActive = new Date().getTime();
    }

}
