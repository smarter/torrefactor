
package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerManager implements Runnable {

    private volatile boolean stopped;

    private Torrent torrent;
    private Map<InetAddress, Peer> peerMap;
    private Map<InetAddress, Peer> activeMap;
    private TrackerManager trackerManager;

    final byte[] peerId = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
    int port = 6881;
    int interval;
    String trackerId;
    int seeders;
    int leechers;
    static final int MAX_PEERS = 25;
    // In milliseconds
    static final int ANNOUNCE_DELAY = 30*60*1000;
    static final int SLEEP_DELAY = 1000;

    public PeerManager(Torrent _torrent) {
        this.torrent = _torrent;
        this.peerMap = new HashMap<InetAddress, Peer>();
        this.activeMap = new HashMap<InetAddress, Peer>();
        this.trackerManager = new TrackerManager(this.torrent);
    }

    public void run() {
        long time = System.currentTimeMillis();
        try {
            ArrayList<Pair<byte[], Integer>> peersList;
            peersList = this.trackerManager.announce(
                                                Tracker.Event.started);
            updateMap(peersList);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        while (!stopped) {
            if (System.currentTimeMillis() - time > ANNOUNCE_DELAY) {
                try {
                    ArrayList<Pair<byte[], Integer>> peersList;
                    peersList = this.trackerManager.announce(
                                                        Tracker.Event.none);
                    updateMap(peersList);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                time = System.currentTimeMillis();
            }
            int i = MAX_PEERS - activeMap.size();
            for (Map.Entry<InetAddress, Peer> peerEntry : peerMap.entrySet()) {
                if (activeMap.containsKey(peerEntry.getKey())) continue;
                new Thread(peerEntry.getValue()).start();
                activeMap.put(peerEntry.getKey(), peerEntry.getValue());
                i--;
                if (i == 0) break;
            }
            Iterator<Map.Entry<InetAddress, Peer>> it = activeMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<InetAddress, Peer> peerEntry = it.next();
                if (!peerEntry.getValue().isValid()) {
                    it.remove();
                    this.peerMap.remove(peerEntry.getKey());
                }
                if (!peerEntry.getValue().isConnected() || peerEntry.getValue().isChokingUs()) {
                    System.out.print(".");
                    continue;
                }
                this.torrent.downloaded += peerEntry.getValue().popDownloaded();
                this.torrent.uploaded += peerEntry.getValue().popUploaded();
                try {
                    int[] requestParams = new int[3];
                    boolean ok = this.torrent.pieceManager.getFreeBlock(peerEntry.getValue().bitfield(), requestParams);
                    if (!ok) {
                        continue;
                    }
                    peerEntry.getValue().sendRequest(requestParams[0], requestParams[1], requestParams[2]); // should be made asynchronous
                } catch (IOException e) {
                    e.printStackTrace();
                    peerEntry.getValue().invalidate();
                    //removeBlock
                }
            }
            try {
                Thread.currentThread().sleep(SLEEP_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() {
        this.stopped = true;
    }

    private void updateMap(ArrayList<Pair<byte[], Integer>> peersList)
    throws IOException, UnknownHostException {
        Map<InetAddress, Peer> oldMap = new HashMap<InetAddress, Peer>(peerMap);
        for (Pair<byte[], Integer> p: peersList) {
            System.out.println("Updating peerMap...");
            InetAddress addr = InetAddress.getByAddress(p.first());
            int port = (p.second());
            updateMap(addr, port, oldMap);
        }
    }

    private void updateMap(InetAddress ip, int port, Map<InetAddress, Peer> oldMap)
    throws IOException, UnknownHostException {
        if (oldMap.containsKey(ip)) {
            this.peerMap.put(ip, oldMap.get(ip));
        } else {
            this.peerMap.put(ip, new Peer(ip, port, this.torrent));
        }
    }
}
