package torrefactor.core.dht;

import torrefactor.core.*;
import torrefactor.util.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A class used internally by NodeManager to handle DHT queries.
 * It is responsible for sending and receiving messages.
 */
public class NodeConnector implements Runnable {
    private final static Logger LOG = new Logger();

    private int curTransactionId;

    /**
     * Maps a query transaction id with the Future that will contains
     * its response when it arrives.
     */
    private Map<Integer, FutureSetter<KRPCMessage>> transactionMap;

    private DatagramSocket socket;

    static final int PACKET_LENGTH = 1024; //in bytes
    static final int SLEEP_DELAY = 100; //in milliseconds

    /**
     * Create a new NodeConnector(there should be only one of them).
     */
    public NodeConnector() {
        this.socket = DatagramSockets.getDatagramSocket();
        this.curTransactionId = -1;
        this.transactionMap = new HashMap<Integer, FutureSetter<KRPCMessage>>();
    }

    /**
     * The main loop handles KRPC message received on port() and send back responses
     * if needed.
     */
    public void run() {
        long time = 0;
        while (true) {
            try {
                byte[] buf = new byte[PACKET_LENGTH];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.socket.receive(packet);
                handleMessage(packet.getAddress(), packet.getPort(), packet.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The port on which we listen for KRPC messages
     */
    public int port() {
        return this.socket.getPort();
    }

    /**
     * Send a KRPC query msg to this ip/port and add it to the transactionMap
     * (so that we will be able to handle the response when it comes)
     * @return a Future that will hold the response
     */
    public Future<KRPCMessage> sendQuery(InetAddress ip, int port, KRPCMessage msg) {
        msg.setTransactionId(++curTransactionId);
        FutureSetter<KRPCMessage> answer = new FutureSetter<KRPCMessage>();
        transactionMap.put(msg.transactionId(), answer);
        try {
            send(ip, port, msg);
        } catch (IOException e) {
            answer.cancel(true);
        }
        return answer;
    }

    /**
     * Send a KRPC message msg to this ip/port.
     */
    private void send(InetAddress ip, int port, KRPCMessage msg) throws IOException {
        byte[] msgArray = msg.toByteArray();
        DatagramPacket packet = new DatagramPacket(msgArray, msgArray.length, ip,
                                                   port);
        socket.send(packet);
    }

    /**
     * Handle a KRPC message received from ip/port. If it's a query, an answer
     * will be sent.
     */
    private void handleMessage(InetAddress ip, int port, byte[] msgArray) {
        String logHdr = ip.toString() + ":" + port;
        KRPCMessage msg = null;
        try {
            msg = new KRPCMessage(msgArray);
        } catch (InvalidBDecodeException e) {
            e.printStackTrace();
            return;
        }
        switch (msg.type()) {
        case query: {
            KRPCMessage resp = null;
            switch(msg.queryType()) {
            case ping: {
                resp = msg.emptyResponse();
                break;
            }
            case find_node: {
                List<Node> nodes = NodeManager.instance().findNode(msg.target());
                resp = msg.findNodeResponse(nodes);
                break;
            }
            case get_peers: {
                List<byte[]> peersId = NodeManager.instance().peersForTorrent(msg.infoHash());
                if (peersId.size() != 0) {
                    resp = msg.getPeersResponse(peersId);
                } else {
                    List<Node> nodes = NodeManager.instance().findNode(msg.target());
                    resp = msg.getPeersNodeResponse(nodes);
                }
                break;
            }
            case announce_peer: {
                if (msg.token() != NodeManager.instance().token()) {
                    //resp = error
                    LOG.error(logHdr, "Got announce_peer with wrong token");
                } else {
                    NodeManager.instance().addPeer(ip, msg.port(), msg.infoHash());
                    resp = msg.emptyResponse();
                }
                break;
            }
            }
            try {
                send(ip, port, resp);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            break;
        }
        case response: {
            FutureSetter<KRPCMessage> answer = transactionMap.get(msg.transactionId());
            if (answer == null) {
                LOG.error(logHdr, "Got response without matching query");
                return;
            }
            answer.set(msg);
            break;
        }
        case error: {
            LOG.error(logHdr, msg.error().toString());
            break;
        }
        }
    }

    /**
     * Send a find_node query. See http://bittorrent.org/beps/bep_0005.html#find-node
     * @param node The node to send the query to.
     * @param id The node id we're looking for
     * @return a Future that will hold the response.
     */
    public Future<KRPCMessage> findNode(Node node, byte[] id) {
        KRPCMessage msg = KRPCMessage.findNode(id);
        return sendQuery(node.ip(), node.port(), msg);
    }

    /**
     * Send a get_peers query. See http://bittorrent.org/beps/bep_0005.html#get-peers
     * @param node The node to send the query to.
     * @param infoHash The torrent info hash we're looking for
     * @return a Future that will hold the response.
     */
    public Future<KRPCMessage> getPeers(Node node, byte[] infoHash) {
        KRPCMessage msg = KRPCMessage.getPeers(infoHash);
        return sendQuery(node.ip(), node.port(), msg);
    }

    /**
     * Send an announce_peer query. See http://bittorrent.org/beps/bep_0005.html#announce-peer
     * @param node The node to send the query to.
     * @param infoHash The torrent info hash we're announcing
     * @param token A token received from a previous get_peers query
     * @return a Future that will hold the response.
     */
    public Future<KRPCMessage> announcePeer(Node node, byte[] infoHash, int token) {
        KRPCMessage msg = KRPCMessage.announcePeer(infoHash, node.port(), token);
        return sendQuery(node.ip(), node.port(), msg);
    }
}
