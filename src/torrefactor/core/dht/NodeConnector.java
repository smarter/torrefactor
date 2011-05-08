package torrefactor.core.dht;

import torrefactor.core.*;
import torrefactor.util.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class NodeConnector implements Runnable {
    private final static Logger LOG = new Logger();

    private int port;
    private int curTransactionId;
    private Map<Integer, FutureSetter<KRPCMessage>> transactionMap;
    private volatile boolean stopped;
    private DatagramSocket socket;

    static final int PACKET_LENGTH = 1024; //in bytes
    static final int SLEEP_DELAY = 100; //in milliseconds

    public NodeConnector() {
        this.socket = DatagramSockets.getDatagramSocket();
        this.curTransactionId = -1;
        this.stopped = false;
    }

    public void run() {
        long time = 0;
        while (!this.stopped) {
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

    public int port() {
        return this.socket.getPort();
    }

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

    private void send(InetAddress ip, int port, KRPCMessage msg) throws IOException {
        byte[] msgArray = msg.toByteArray();
        DatagramPacket packet = new DatagramPacket(msgArray, msgArray.length, ip,
                                                   port);
        socket.send(packet);
    }

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

    public Future<KRPCMessage> findNode(Node node, byte[] id) {
        KRPCMessage msg = KRPCMessage.findNode(id);
        return sendQuery(node.ip(), node.port(), msg);
    }

    public Future<KRPCMessage> getPeers(Node node, byte[] infoHash) {
        KRPCMessage msg = KRPCMessage.getPeers(infoHash);
        return sendQuery(node.ip(), node.port(), msg);
    }

    public Future<KRPCMessage> announcePeer(Node node, byte[] infoHash, int token) {
        KRPCMessage msg = KRPCMessage.announcePeer(infoHash, node.port(), token);
        return sendQuery(node.ip(), node.port(), msg);
    }
}
