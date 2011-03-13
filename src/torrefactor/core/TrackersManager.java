package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;
import torrefactor.core.Tracker.Event;

import java.io.*;
import java.net.*;
import java.util.*;


public class TrackersManager {
    private ArrayList<LinkedList<String>> tiers;
    private long nextAnnounceTime = 0;
    private Torrent torrent;

    // Trackers are stored this way:    (t=tracker, b=backup)
    // [ [t1, t2, t3,... ], [b1,...], [b2,...], ...]
    public TrackersManager (Torrent torrent) {
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
                tracker = TrackerFactory.getTracker(uri);
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
}
