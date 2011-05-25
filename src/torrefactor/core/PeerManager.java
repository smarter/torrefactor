package torrefactor.core;

import torrefactor.core.*;
import torrefactor.core.dht.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * The PeerManager thread keeps track of the known and the active
 * Peers and decide when to send announces to the known trackers
 * and when to ask Peers for a block of data.
 */
public class PeerManager implements Runnable {
    private static Logger LOG = new Logger();

    private volatile boolean stopped;

    public enum State {Stopped, Announcing, Started, Seeding};
    private volatile State state;

    private Torrent torrent;
    private volatile Map<InetAddress, Peer> peerMap;
    private Map<InetAddress, Peer> activeMap;
    private TrackerManager trackerManager;
    private LinkedBlockingQueue<Peer> newPeers =
        new LinkedBlockingQueue<Peer>();

    //Volatile since several PeerManager could be instantiated at the same time
    private static volatile byte[] peerId;

    static final String idInfo = "-TF0010-";

    int peersReceived;

    static final int MAX_PEERS = 25;
    // In milliseconds
    static final int SLEEP_DELAY = 10;
    // Number of blocks requested at the same time per peer
    static final int MAX_QUEUED_REQUESTS = 10;
    // Time to sleep before retrying annouce when no tracker responded
    static final long TRACKER_RETRY_SLEEP = 5000;

    public PeerManager(Torrent _torrent) {
        // Make sure we have a peerId
        PeerManager.peerId();

        this.stopped = true;
        this.peersReceived = 0;
        this.torrent = _torrent;
        this.peerMap = new HashMap<InetAddress, Peer>();
        this.activeMap = new HashMap<InetAddress, Peer>();
        this.trackerManager = new TrackerManager(this.torrent);
    }

    public void run() {
        try {
            List<Pair<byte[], Integer>> peersList;
            peersList = null;

            // Announce until a tracker respond
            this.state = State.Announcing;
            peersList = this.trackerManager.announce(Tracker.Event.started);
            int count = 1;
            while (peersList == null) {
                peersList = this.trackerManager.announce(Tracker.Event.started);

                long wait = TRACKER_RETRY_SLEEP * count;
                LOG.info("No tracker responded, retrying in "
                         + wait/1000 + " seconds");
                Thread.sleep(wait);
                count ++;
            }

            this.peersReceived = peersList.size();
            updateMap(peersList);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        this.state = State.Started;
        stopped = false;
        while (!stopped) {

            // Add peer which have been added via addPeer(Peer)
            while (this.newPeers.size() > 0) {
                Peer peer = this.newPeers.poll();
                if (peer == null) continue;

                this.peerMap.put(peer.getAddress(), peer);
                this.activeMap.put(peer.getAddress(), peer);
            }

            if (this.trackerManager.canAnnounce()) {
                    //(peerMap.size() <= this.peersReceived / 2)
                this.state = State.Announcing;
                try {
                    
                    // Add peers from DHT
                    if (NodeManager.instance() != null) {
                        updateMapCompact(
                                NodeManager.instance().peersForTorrent(
                                    this.torrent.infoHash));
                    }

                    // Add peers from announce
                    List<Pair<byte[], Integer>> peersList;
                    peersList = this.trackerManager.announce(
                                                        Tracker.Event.none);
                    updateMap(peersList);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                this.state = State.Started;
            }

            // Start new peers if some died
            int i = MAX_PEERS - activeMap.size();
            for (Map.Entry<InetAddress, Peer> peerEntry : peerMap.entrySet()) {
                if (activeMap.containsKey(peerEntry.getKey())) continue;
                new Thread(peerEntry.getValue()).start();
                activeMap.put(peerEntry.getKey(), peerEntry.getValue());
                i--;
                if (i <= 0) break;
            }

            ArrayList<Integer> newPieces =
                this.torrent.pieceManager.popToAnnounce();

            Iterator<Map.Entry<InetAddress, Peer>> it =
                activeMap.entrySet().iterator();
            activeMapIteration:
            while (it.hasNext()) {

                Map.Entry<InetAddress, Peer> peerEntry = it.next();
                Peer peer = peerEntry.getValue();
                InetAddress peerAddress = peerEntry.getKey();
                if (!peer.isValid()) {
                    it.remove();
                    this.peerMap.remove(peerAddress);
                    continue;
                }

                for (int piece: newPieces) {
                    peer.sendHave(piece);

                    if (this.torrent.isComplete()) {
                        LOG.debug("Torrent is complete");
                        if (ByteArrays.isComplete(peer.bitfield)) {
                            // the peer and us got all the pieces, close the
                            // connection
                            LOG.debug("We both have all pieces: "
                                      + peer.toString());
                            peer.invalidate();
                            it.remove();
                            this.peerMap.remove(peerAddress);

                            // This break has been well tested and won't end up
                            // like xkcd commic: http://xkcd.com/292/
                            // Yeah, I promise you no dinosaur will comme from
                            // your left ;)
                            break activeMapIteration;
                        }
                    }
                }

                if (peer.canRequest()) {
                    //LOG.debug("Cannot request to: " + peer);

                    try {
                        List<DataBlockInfo> infoList =
                            this.torrent.pieceManager.getFreeBlocks(
                                    peer.bitfield,
                                    MAX_QUEUED_REQUESTS);

                        Iterator<DataBlockInfo> iter = infoList.iterator();
                        while (iter.hasNext()) {
                            peer.sendRequest(iter.next());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        peer.invalidate();
                        //removeBlock
                    }
                }
            }

            // Announce complete if we got the last piece
            if (newPieces.size() > 0) {
                if (this.torrent.isComplete()) {
                    this.state = State.Announcing;
                    this.trackerManager.announce(Tracker.Event.completed);
                    this.state = State.Seeding;
                }
            }

            // Sleep a bit to spare CPU and leave time to send messages to
            // peers
            try {
                Thread.currentThread().sleep(SLEEP_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

       // peerManager is now stopped
       for (Map.Entry<InetAddress, Peer> entry : this.peerMap.entrySet()) {
            entry.getValue().invalidate();
       }

       this.state = State.Announcing;
       LOG.debug("Announcing \"stopped\" to trackers (this may take a while "
                 + "if they don't respond promptly)");
       this.trackerManager.announce(Tracker.Event.stopped);
       this.state = State.Stopped;

    }

    public void stop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    private void updateMap(List<Pair<byte[], Integer>> peersList)
    throws IOException, UnknownHostException {
        if (peersList == null) {
            LOG.warning("Didn't get any new peer!");
            return;
        }
        LOG.debug("Got " + peersList.size() + " new peers");
        Map<InetAddress, Peer> newMap = new HashMap<InetAddress, Peer>(peerMap);
        for (Pair<byte[], Integer> p: peersList) {
            InetAddress addr = InetAddress.getByAddress(p.first());
            int port = (p.second());
            updateMap(addr, port, newMap);
        }
        this.peerMap = newMap;
    }

    /**
     * Update peerMap using peersList.
     * @param peersList a list of peers in "compact ip/port" format
     */
    private void updateMapCompact(List<byte[]> peersList)
    throws IOException, UnknownHostException {
        if (peersList == null) {
            return;
        }
        LOG.debug("Got " + peersList.size() + " new peers from DHT");
        Map<InetAddress, Peer> newMap = new HashMap<InetAddress, Peer>(peerMap);
        byte[] ip = new byte[4];
        byte[] portArray = new byte[2];
        for (byte[] peer: peersList) {
            System.arraycopy(peer, 0, ip, 0, 4);
            System.arraycopy(peer, 4, portArray, 0, 2);
            InetAddress addr = InetAddress.getByAddress(ip);
            int port = ByteArrays.toShortInt(portArray);
            updateMap(addr, port, newMap);
        }
        this.peerMap = newMap;
    }

    private void updateMap(InetAddress ip, int port, Map<InetAddress, Peer> map)
    throws IOException, UnknownHostException {
        if (!map.containsKey(ip)) {
            map.put(ip, new Peer(ip, port, this.torrent));
            LOG.debug("" + ip + ":" + port);
        }
    }

    public Map<InetAddress, Peer> getPeerMap () {
        if (this.peerMap == null) return null;
        return Collections.unmodifiableMap(this.peerMap);
    }

    public void addPeer (Peer peer) {
        this.newPeers.offer(peer);
    }

    /**
     * Return our peerId
     */
    public static byte[] peerId() {
        if (peerId == null) {
            // Azureus style, see
            // http://wiki.theory.org/BitTorrentSpecification#peer_id
            Random rand = new Random();
            String idRand = UUID.randomUUID().toString().substring(
                    0, 20 - idInfo.length());
            LOG.debug(idRand);
            peerId = (idInfo + idRand).getBytes();
        }
        return peerId;
    }

    /**
     * Return the current state.
     */
    public State state () {
        return this.state;
    }
}
