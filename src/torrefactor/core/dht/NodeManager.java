package torrefactor.core.dht;

import torrefactor.core.*;
import torrefactor.core.dht.*;
import torrefactor.util.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A singleton to store and query DHT nodes
 */
public class NodeManager {
    private final static Logger LOG = new Logger();
    private static NodeManager instance = null;
    private NodeConnector nodeConnector;

    /**
     * Our node ID.
     */
    byte[] id;

    private int token;

    /**
     * buckets.get(buckets.size() - 1) contains the closest nodes.
     * buckets.get(0) contains the most distant nodes.
     */
    private List<Bucket> buckets;

    /**
     * Map infohash to list of peers info (in compact format)
     * for the corresponding torrent.
     */
    private Map<byte[], ExpirationList<byte[]>> hashPeerMap;

    /**
     * Sort in decreasing order by common prefix length of the
     * node id with our id
     */
    private Comparator<Node> nodeComparator = new Comparator<Node>() {
        public int compare(Node o1, Node o2) {
            return ByteArrays.commonPrefix(id, o2.id())
            - ByteArrays.commonPrefix(id, o1.id());
            }
    };

    /**
     * A runnable means to be run in a Thread to periodically update the list
     * of peers for every torrent we're downloading and announcing that we're
     * downloading them.
     */
    private Runnable announcer = new Runnable() {
            public void run() {
                while (true) {
                    List<Torrent> torrentList = new ArrayList<Torrent>();
                    synchronized(TorrentManager.instance()) {
                        torrentList.addAll(TorrentManager.instance().torrentList());
                    }
                    for (Torrent torrent : torrentList) {
                        iterativeAnnounceAndGet(torrent.infoHash);
                    }
                    try {
                        Thread.sleep(ANNOUNCE_DELAY);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        };

    static final int RESPONSE_TIMEOUT = 2*60*1000; //in milliseconds
    static final int ANNOUNCE_DELAY = 30*60*1000; //in miliseconds
    static final int EXPIRATION_DELAY = 32; //in minutes

    private NodeManager(InetAddress ip, int port) {
        id = new byte[160/8];
        new Random().nextBytes(id);
        token = new Random().nextInt() & 0xFFFF;
        hashPeerMap = new HashMap<byte[], ExpirationList<byte[]>>();
        buckets = new ArrayList<Bucket>(160);
        buckets.add(new Bucket());
        addNode(ip, port);

        nodeConnector = new NodeConnector();
        Thread connectorThread = new Thread(nodeConnector);
        connectorThread.setDaemon(true);
        connectorThread.start();

        List<Node> shortList = iterativeFindNode(this.id);
        for (Node node : shortList) {
            addNode(node);
        }
        Thread announceThread = new Thread(announcer);
        announceThread.setDaemon(true);
        announceThread.run();
    }

    /**
     * Returns the instance of the NodeManager singleton if it exists
     * or null otherwise.
     */
    public static synchronized NodeManager instance() {
        return NodeManager.instance;
    }

    /**
     * Create the NodeManager singleton if it doesn't exist. This is needed
     * because we need at least one node to bootstrap and become part of the
     * DHT network.
     */
    public static synchronized void setInstance(InetAddress ip, int port) {
        if (NodeManager.instance == null) {
            return;
        }
        NodeManager.instance = new NodeManager(ip, port);
    }

    /**
     * Our node token.
     */
    public int token() {
        return this.token;
    }

    /**
     * Our node DHT port
     */
    public int port() {
        return this.nodeConnector.port();
    }

    /**
     * Try to add the specified node according to the rules described
     * in BEP 5.
     * @return true if the node was added, false otherwise
     */
    public boolean addNode(Node node) {
        int prefix = ByteArrays.commonPrefix(this.id, id);
        int index = Math.min(prefix, buckets.size() - 1);
        Bucket bucket = buckets.get(index);
        if (bucket.isFull() && index == buckets.size() - 1) {
            // bucket contains our own id, split it
            buckets.add(bucket.splitBucket(this.id, index));
            // node could be in the new bucket, so the index may have changed
            index = Math.min(prefix, buckets.size() - 1);
            bucket = buckets.get(index);
        }
        return bucket.add(node);
    }

    /**
     * Send a ping KRPC message to this ip/port and try to
     * add the node according to the rules described in BEP 5 if we get a
     * response.
     * @return true if the node was added, false otherwise
     */
    public boolean addNode(InetAddress ip, int port) {
        LOG.debug("addNode: ip: " + ip + " port: " + port);
        KRPCMessage resp = null;
        try {
            resp = ping(ip, port).get(RESPONSE_TIMEOUT,
                                      TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Node node = new Node(resp.nodeId(), ip, port);
        return addNode(node);
    }

    /**
     * Send a ping KRPC message to this ip/port
     * @return a Future which will contain the response
     */
    public Future<KRPCMessage> ping(InetAddress ip, int port) {
        KRPCMessage msg = KRPCMessage.ping();
        return nodeConnector.sendQuery(ip, port, msg);
    }

    /**
     * Send a ping KRPC message to this node
     * @return a Future which will contain the response
     */
    public Future<KRPCMessage> ping(Node node) {
        return ping(node.ip(), node.port());
    }

    /**
     * Returns a list of the Bucket.CAPACITY closest nodes to id in our buckets.
     */
    List<Node> findNode(byte[] id) {
        int index = bucketIndex(id);
        Bucket bucket = buckets.get(index);
        List<Node> closeList = new ArrayList<Node>(Bucket.CAPACITY);
        closeList.addAll(bucket.nodes());
        for (int i = 0; i < 160 && closeList.size() < Bucket.CAPACITY; i++) {
            //Didn't find enough nodes. Try in buckets closer to our own id
            //first, since they have the same prefix than the id we're looking
            //for.
            if ((index + i) < buckets.size()) {
                    closeList.addAll(buckets.get(index + i).nodes());
            } else if ((index - i) > 0) {
                closeList.addAll(buckets.get(index - i).nodes());
            } else {
                break;
            }
        }
        Collections.sort(closeList, nodeComparator);
        dropAfter(Bucket.CAPACITY, closeList);
        return closeList;
    }

    /**
     * Returns a list of peers in "compact ip/port" format
     * for this torrent that are known by our node.
     */
    public List<byte[]> peersForTorrent(byte[] infoHash) {
        if (hashPeerMap.containsKey(infoHash)) {
            return hashPeerMap.get(infoHash).unwrapCopy();
        } else {
            return null;
        }
    }

    /**
     * Add a new peer to the infoHashMap
     *
     * @param ip        IP of the peer
     * @param port      TCP port for connecting to the peer
     * @param infoHash  info hash of the torrent this peer is on
     */
    public boolean addPeer(InetAddress ip, int port, byte[] infoHash) {
        byte[] peer = ByteArrays.compact(ip, port);
        ExpirationList<byte[]> peers = hashPeerMap.get(infoHash);
        if (peers == null) {
            peers = new ExpirationList<byte[]>(EXPIRATION_DELAY, TimeUnit.MINUTES);
            hashPeerMap.put(infoHash, peers);
        }
        return peers.add(peer);
    }

    /**
     * As described in
     * http://xlattice.sourceforge.net/components/protocol/kademlia/specs.html#lookup
     * @param type  Type of the query. Must be find_node or get_peers
     */
    private List<Node> iterativeFindNode(byte[] id) {
        KRPCMessage query = KRPCMessage.findNode(id);
        int req = Bucket.CAPACITY; // Number of find requests sent in parallel
        List<Node> shortList = findNode(id);
        Node closestNode = null;
        while (closestNode != shortList.get(0)) {
            closestNode = shortList.get(0);
            FutureList<KRPCMessage> futureNodes = queryList(shortList,
                                                            KRPCMessage.QueryType.find_node);
            while (futureNodes.size() != 0) {
                try {
                    KRPCMessage resp = futureNodes.removeAny(RESPONSE_TIMEOUT);
                    if (resp == null) {
                        break;
                    }
                    shortList.addAll(resp.nodes());
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            Collections.sort(shortList, nodeComparator);
            dropAfter(req, shortList);
        }
        return shortList;
    }

    /**
     * Announce that we are downloading the torrent with infohash id and update
     * hashPeerMap with new peers.
     */
    void iterativeAnnounceAndGet(byte[] id) {
        List<Node> list = iterativeFindNode(id);
        FutureList<KRPCMessage> futureNodes = queryList(list, KRPCMessage.QueryType.get_peers);
        boolean gotNewPeers = false;
        for (int i = 0; i < list.size(); i++) {
            try {
                KRPCMessage resp = futureNodes.removeFirst(RESPONSE_TIMEOUT);
                if (resp == null) {
                    break;
                }
                if (!gotNewPeers) {
                    List<byte[]> newPeers = resp.peers();
                    if (newPeers != null) {
                        ExpirationList<byte[]> peers = hashPeerMap.get(id);
                        if (peers == null) {
                            peers = new ExpirationList<byte[]>(EXPIRATION_DELAY, TimeUnit.MINUTES);
                            hashPeerMap.put(id, peers);
                        }
                        peers.addAll(newPeers);
                        gotNewPeers = true;
                    }
                }
                nodeConnector.announcePeer(list.get(i), id, resp.token());
            } catch (Exception e) {
                continue;
            }
        }
    }

    /**
     * Send queries of the specified type to each node of the list.
     *
     * @param list  The list of nodes to query
     * @param type  Must be find_node or get_peers
     *
     * @return      A FutureList with the responses to each query.
     */
    FutureList<KRPCMessage> queryList(List<Node> list, KRPCMessage.QueryType type) {
        FutureList<KRPCMessage> futureNodes = new FutureList<KRPCMessage>();
        for (Node node : list) {
            if (type == KRPCMessage.QueryType.find_node) {
                futureNodes.add(nodeConnector.findNode(node, id));
            } else {
                futureNodes.add(nodeConnector.getPeers(node, id));
            }
        }
        return futureNodes;
    }

    /**
     * Index of bucket that could hold the node with the specified id
     */
    private int bucketIndex(byte[] id) {
        int prefix = ByteArrays.commonPrefix(this.id, id);
        return Math.min(prefix, buckets.size() - 1);
    }

    /**
     * Drop every entry after the n-th one in the list.
     */
    private static <T> void dropAfter(int n, List<T> list) {
        if (list.size() > n) {
            list.subList(n, list.size()).clear();
        }
    }
}
