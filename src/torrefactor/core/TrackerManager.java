package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;
import torrefactor.core.Tracker.Event;

import java.io.*;
import java.net.*;
import java.util.*;


public class TrackerManager {
    private static Log LOG = Log.getInstance();
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
        // Remove the sign
        this.uniqKey = random.nextInt() >>> 1;
    }

    public List<Pair<byte[], Integer>> announce (Event event) {
        List<Pair<byte[], Integer>> peersList = null;
        LOG.log(Log.INFO, this, "Announcing...");

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

        LOG.log(Log.DEBUG, this, "Trying tier: " + tier);
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
        LOG.log(Log.DEBUG, this, "Get tracker: " + uri);
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
            LOG.log(Log.DEBUG, this,
                    "Cannot construct tracker object for uri: \"" + uri + "\"");
        }
        return tracker;
    }
}
