package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;
import torrefactor.core.Tracker.Event;

import java.io.*;
import java.net.*;
import java.util.*;


public class TrackerManager {
    private List<List<String>> tiers;
    private long nextAnnounceTime = 0;
    private Torrent torrent;
    protected int uniqKey;

    // Trackers are stored this way:    (t=tracker, b=backup)
    // [ [t1, t2, t3,... ], [b1,...], [b2,...], ...]
    public TrackerManager (Torrent torrent) {
        Random random = new Random();
        this.tiers = torrent.announceList;
        this.torrent = torrent;
        this.uniqKey = random.nextInt();
    }

    public List<Pair<byte[], Integer>> announce (Event event) {
        List<Pair<byte[], Integer>> peersList = null;
        System.err.println("Announcing...");

        for (List<String> tier: this.tiers) {
            peersList = announceTier(event, tier);
            if (peersList != null) {
                break;
            }
        }
        return peersList;
    }

    private List<Pair<byte[], Integer>>
    announceTier (Event event, List<String> tier) {
        ArrayList<Pair<byte[], Integer>> peersList = null;
        Tracker tracker;

        System.err.println("Trying tier: " + tier);
        for (String uri: tier) {
            try {
                //if (! uri.substring(0,3).equals("udp")) {
                //    System.out.println("Skipping: " + uri);
                //    continue;
                //}
                tracker = getTracker(uri);
                peersList = tracker.announce(this.torrent, event);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (peersList == null) {
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

    public Tracker getTracker (String uri) {
        Tracker tracker = null;
        System.err.println("Get tracker: " + uri);
        try {
            if (uri.substring(0, 6).equals("udp://")) {
                tracker = new UdpTracker(uri, this.uniqKey);
            } else if (uri.substring(0, 7).equals("http://")) {
                tracker = new HttpTracker(uri, this.uniqKey);
            } else {
                throw new UnsupportedOperationException (
                    "Don't know how to handle uri \"" + uri + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Cannot construct tracker object for uri: \""
                               + uri + "\"");
        }
        return tracker;
    }
}
