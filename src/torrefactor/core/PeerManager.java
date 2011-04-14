package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class PeerManager implements Runnable {
    private static Logger LOG = new Logger();

    private volatile boolean stopped;

    private Torrent torrent;
    private Map<InetAddress, Peer> peerMap;
    private Map<InetAddress, Peer> activeMap;
    private TrackerManager trackerManager;

    //Shared by all PeerManager instances
    static byte[] peerId;
    static final String idInfo = "-TF0010-";

    int peersReceived;

    static final int MAX_PEERS = 25;
    // In milliseconds
    static final int ANNOUNCE_DELAY = 30*60*1000;
    static final int SLEEP_DELAY = 10;
    // Number of blocks requested at the same time per peer
    static final int BLOCKS_PER_REQUEST = 10;

    public PeerManager(Torrent _torrent) {
        this.stopped = true;
        if (this.peerId == null) {
            // Azureus style, see http://wiki.theory.org/BitTorrentSpecification#peer_id
            Random rand = new Random();
            String idRand = UUID.randomUUID().toString().substring(0, 20 - idInfo.length());
            LOG.debug(this, idRand);
            this.peerId = new String(idInfo + idRand).getBytes();
        }
        this.peersReceived = 0;
        this.torrent = _torrent;
        this.peerMap = new HashMap<InetAddress, Peer>();
        this.activeMap = new HashMap<InetAddress, Peer>();
        this.trackerManager = new TrackerManager(this.torrent);
    }

    public void run() {
        long time = System.currentTimeMillis();
        try {
            List<Pair<byte[], Integer>> peersList;
            peersList = this.trackerManager.announce(Tracker.Event.started);
            this.peersReceived = peersList.size();
            updateMap(peersList);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        stopped = false;
        while (!stopped) {
            if (System.currentTimeMillis() - time > ANNOUNCE_DELAY || peerMap.size() <= this.peersReceived / 2) {
                try {
                    List<Pair<byte[], Integer>> peersList;
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
                    continue;
                }
                if (!peerEntry.getValue().isConnected() || peerEntry.getValue().isChokingUs()
                    || peerEntry.getValue().isQueueFull()) {
                    LOG.debug(this, ".");
                    continue;
                }
                this.torrent.incrementDownloaded(
                                        peerEntry.getValue().popDownloaded());
                this.torrent.incrementUploaded(
                                        peerEntry.getValue().popUploaded());
                try {
                    List<DataBlockInfo> infoList = this.torrent.pieceManager.getFreeBlocks(peerEntry.getValue().bitfield(), BLOCKS_PER_REQUEST);
                    Iterator<DataBlockInfo> iter = infoList.iterator();
                    while (iter.hasNext()) {
                        peerEntry.getValue().queueRequest(iter.next());
                    }
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

    public boolean isStopped() {
        return this.stopped;
    }

    private void updateMap(List<Pair<byte[], Integer>> peersList)
    throws IOException, UnknownHostException {
        Map<InetAddress, Peer> oldMap = new HashMap<InetAddress, Peer>(peerMap);
        for (Pair<byte[], Integer> p: peersList) {
            LOG.debug(this, "Updating peerMap...");
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
