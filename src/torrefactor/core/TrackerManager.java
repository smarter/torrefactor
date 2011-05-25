package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;
import torrefactor.core.Tracker.Event;

import java.io.*;
import java.net.*;
import java.util.*;


/**
 * Manage a list of trackers and provide convenient mothods to make announces.
 */
public class TrackerManager {
    private static Logger LOG = new Logger();
    private List<List<String>> tiers;
    private long nextAnnounceTime = 0;
    private Torrent torrent;
    protected int uniqKey;

    // Trackers are stored this way:    (t=tracker, b=backup)
    // [ [t1, t2, t3,... ], [b1,...], [b2,...], ...]
    public TrackerManager (Torrent torrent) {
        Random random = new Random();
        this.tiers = torrent.announceList;
        blackList();
        this.torrent = torrent;
        // Remove the sign
        this.uniqKey = random.nextInt() >>> 1;
    }

    /**
     * Remove blacklisted trackers.
     */
    private void blackList() {
        // In Haskell, this mess could be reduced to:
        // filter (not . null) $ map (filter $ not . (isInfixOf "tracker.thepiratebay.org")) tiers
        Iterator<List<String>> tierIt = this.tiers.iterator();
        while (tierIt.hasNext()) {
            List<String> tier = tierIt.next();
            Iterator<String> trackerIt = tier.iterator();
            while (trackerIt.hasNext()) {
                String tracker = trackerIt.next();
                // TPB tracker has been permanently shut down, avoid timeout
                if (tracker.contains("tracker.thepiratebay.org")) {
                    trackerIt.remove();
                }
            }
            if (tier.isEmpty()) {
                tierIt.remove();
            }
        }
    }

    /**
     * Announce an event to the tracker.
     *
     * @param event the event to announce
     * @return a List of Pair (address ip as byte[], port) returned by the
     *         trackers. (Can be null)
     */
    public List<Pair<byte[], Integer>> announce (Event event) {
        List<Pair<byte[], Integer>> peersList = null;
        LOG.info(this, "Announcing...");

        for (List<String> tier: this.tiers) {
            peersList = announceTier(event, tier);
            if (peersList != null) {
                break;
            }
        }
        return peersList;
    }

    /**
     * Make an announce as per Bittorrent multitracker specification for the
     * given tier.
     *
     * @param event        the event to announce
     * @param tier        the tier to announce
     * @return a List of Pair (address ip as byte[], port) returned by the
     *         trackers. (Can be null)
     */
    private List<Pair<byte[], Integer>>
    announceTier (Event event, List<String> tier) {
        ArrayList<Pair<byte[], Integer>> peersList = null;
        Tracker tracker;

        LOG.debug(this, "Trying tier: " + tier);
        for (String uri: tier) {
            try {
                tracker = getTracker(uri);
                peersList = tracker.announce(this.torrent, event);
            } catch (Exception e) {
                LOG.error(this ,e);
                e.printStackTrace();
                continue;
            }
            if (peersList == null) {
                continue;
            }
            tier.remove(uri);
            tier.add(0, uri);
            this.nextAnnounceTime = tracker.getNextAnnounceTime();
            LOG.debug("Next announce at: " + this.nextAnnounceTime);
        }
        return peersList;
    }

    /**
     * Return the time when we are allowed to do the next annouce.
     */
    public long nextAnnounceTime () {
        return this.nextAnnounceTime;
    }

    /**
     * Return true if we should do an announce.
     */
    public boolean canAnnounce () {
        return System.currentTimeMillis() > this.nextAnnounceTime;
    }

    /**
     * Create a Tracker object for the given uri.
     *
     * @param uri    the uri of the tracker
     */
    public Tracker getTracker (String uri) {
        Tracker tracker = null;
        LOG.debug(this, "Get tracker: " + uri);
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
            LOG.debug(this,
                    "Cannot construct tracker object for uri: \"" + uri + "\"");
            e.printStackTrace();
        }
        return tracker;
    }
}
