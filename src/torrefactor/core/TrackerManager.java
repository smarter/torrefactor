package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;
import torrefactor.core.Tracker.Event;

import java.io.*;
import java.net.*;
import java.util.*;


public class TrackerManager {
    private ArrayList<LinkedList<String>> tiers;
    private long nextAnnounceTime = 0;
    private Torrent torrent;

    // Trackers are stored this way:    (t=tracker, b=backup)
    // [ [t1, t2, t3,... ], [b1,...], [b2,...], ...]
    public TrackerManager (Torrent torrent) {
        this.tiers = torrent.announceList;
        this.torrent = torrent;
    }

    public ArrayList<ArrayList> announce (Event event) {
        ArrayList<ArrayList> peersList = null;
        System.err.println("Announcing...");

        for (LinkedList<String> tier: this.tiers) {
            peersList = announceTier(event, tier);
            if (peersList != null) {
                break;
            }
        }
        return peersList;
    }

    private ArrayList<ArrayList> announceTier (Event event,
                                    LinkedList<String> tier) {
        ArrayList<ArrayList> peersList = null;
        Tracker tracker;

        for (String uri: tier) {
            try {
                tracker = getTracker(uri);
                peersList = tracker.announce(this.torrent, event);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            tier.remove(uri);
            tier.add(0, uri);
            this.nextAnnounceTime = tracker.getNextAnnounceTime();
        }
        return peersList;
    }

    public long nextAnnounceTime () {
        return this.nextAnnounceTime;
    }

    public static Tracker getTracker (String uri) {
        Tracker tracker = null;
        try {
            if (uri.substring(0, 6).equals("udp://")) {
                tracker = new UdpTracker(uri);
            } else if (uri.substring(0, 7).equals("http://")) {
                tracker = new HttpTracker(uri);
            }/* else if (uri.substring(0, 8).equals("https://")) {
                return new HttpTracker (uri);
            }*/
            throw new UnsupportedOperationException (
                    "Don't know how to handle uri \"" + uri + "\"");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Cannot construct tracker object for uri: \""
                               + uri + "\"");
        }
        return tracker;
    }
}
