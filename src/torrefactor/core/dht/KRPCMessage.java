package torrefactor.core.dht;

import torrefactor.core.dht.*;
import torrefactor.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Kademlia RPC(Remote procedure call) message.
 * See http://bittorrent.org/beps/bep_0005.html#krpc-protocol
 */
public class KRPCMessage {
    /**
     * The different types of the message
     */
    public enum Type {
        query, response, error
    };

    /**
     * The different types of queries
     * See http://bittorrent.org/beps/bep_0005.html#queries
     */
    public enum QueryType {
        ping, find_node, get_peers, announce_peer
    };

    /**
     * The different types of errors
     * See http://bittorrent.org/beps/bep_0005.html#errors
     */
    public enum ErrorType {
        GenericError, ServerError, ProtocolError, UnknownMethod, invalid
    };

    private Map<String, BValue> message;

    /**
     * Create a new KRPCMessage from the byte array message which must be
     * a vaild bencoded message.
     */
    public KRPCMessage(byte[] message) throws InvalidBDecodeException {
        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        try {
            this.message = BDecode.decodeDict(stream);
        } catch (IOException e) {
            //can't happen with a ByteArrayInputStream according to the documentation
            e.printStackTrace();
        }
    }

    /**
     * Create a new empty KRPCMessage with the specified type.
     */
    public KRPCMessage(Type type) {
        this.message = new HashMap<String, BValue>();
        setType(type);
        setArgument("id", new BValue(NodeManager.instance().id));
    }

    /**
     * Set the transaction id of this message(used in get_peers response and
     * announce_peers query.
     */
    public void setTransactionId(int id) {
        this.message.put("t", new BValue(id));
    }

    /**
     * Bencode the message.
     */
    public byte[] toByteArray() {
        return BEncode.encode(new BValue(this.message));
    }

    /**
     * Set the type of this message.
     */
    private void setType(Type type) {
        String typeString = null;
        switch (type) {
        case query: {
            typeString = "q";
            break;
        }
        case response: {
            typeString = "r";
            break;
        }
        case error: {
            typeString = "e";
            break;
        }
        }
        this.message.put("y", new BValue(typeString));
    }

    /**
     * Make this message a query and set its type.
     */
    private void setQuery(QueryType type) {
        this.message.put("y", new BValue("q"));
        this.message.put("q", new BValue(type.toString()));
    }

    /**
     * Returns the type of this message.
     */
    public Type type() {
        if (this.message.get("y").toString().equals("y")) {
            return Type.query;
        } else if (this.message.get("r").toString().equals("r")) {
            return Type.response;
        } else {
            return Type.error;
        }
    }

    /**
     * Return the type of query of this message if it's a query,
     * null otherwise.
     */
    public QueryType queryType() {
        if (type() != Type.query) {
            return null;
        }
        return QueryType.valueOf(this.message.get("q").toString());
    }

    /**
     * Return the type of error of this message if it's an error,
     * null otherwise.
     */
    public Pair<ErrorType, String> error() {
        if (type() != Type.error) {
            return null;
        }
        List<BValue> error = this.message.get("e").toList();
        ErrorType type = null;
        switch (error.get(0).toInt()) {
        case 201:
            type = ErrorType.GenericError;
            break;
        case 202:
            type = ErrorType.ServerError;
            break;
        case 203:
            type = ErrorType.ProtocolError;
            break;
        case 204:
            type = ErrorType.UnknownMethod;
            break;
        default:
            type = ErrorType.invalid;
            break;
        }
        return new Pair<ErrorType, String>(type, error.get(1).toString());
    }

    /**
     * Set the specified key to the specified value in the
     * argument map of the message or do nothing if the message
     * subtype(query, error or response) has not been set. 
     */
    private void setArgument(String key, BValue value) {
        Map<String, BValue> argMap = argumentMap();
        if (argMap == null) {
            return;
        }
        argMap.put(key, value);
    }

    /**
     * Return the value of the key in this message argument map
     * if it exists or null otherwise.
     */
    private BValue getArgument(String key) {
        Map<String, BValue> argMap = argumentMap();
        if (argMap == null) {
            return null;
        }
        return argMap.get(key);
    }

    /**
     * Create the argument map if it doesn't exist and return it
     */
    private Map<String, BValue> argumentMap() {
        String key = null;
        Type type = this.type();
        if (type == Type.query) {
            key = "a";
        } else if (type == Type.response) {
            key = "r";
        } else { // error
            return null;
        }
        return this.message.get(key).toMap();
    }

    /**
     * Return the value of this message "id" key.
     * Used in all messages.
     */
    public byte[] nodeId() {
        return getArgument("id").toByteArray();
    }

    /**
     * Return te value of this message "token" key.
     * Used in get_peers responses and announce_peer queries.
     */
    public int token() {
        return getArgument("token").toInt();
    }

    /**
     * Return the value of this message "target" key.
     * Used in find_node queries, it contains the id of the node the querier is
     * looking for.
     */
    public byte[] target() {
        return getArgument("target").toByteArray();
    }

    /**
     * Return the value of this message "info_hash" key.
     * Used in get_peers to specify that we want peers associated with this torrent.
     * Used in announce_peer queries to specify the torrent we're announcing
     * that we're downloading.
     */
    public byte[] infoHash() {
        return getArgument("info_hash").toByteArray();
    }

    /**
     * Return the value of this message "port" key.
     * Used in announce_peer queries to specify the port on which our peer is downloading
     * the torrent.
     */
    public int port() {
        return getArgument("port").toInt();
    }

    /**
     * Returns the value of this message "t" key.
     * Used in every query and mirrored in response to identify the query corresponding
     * to the response.
     */
    public int transactionId() {
        return this.message.get("t").toInt();
    }

    /**
     * Returns the value of this message "values" key as a list
     * of "compact ip/port" or returns null if the argument does
     * not exist.
     * Used in get_peers response.
     */
    public List<byte[]> peers() {
        BValue arg = getArgument("values");
        if (arg == null) {
            return null;
        }
        List<BValue> argList = arg.toList();
        List<byte[]> peers = new ArrayList<byte[]>(argList.size());
        for (BValue peer : argList) {
            peers.add(peer.toByteArray());
        }
        return peers;
    }

    /**
     * Returns the value of this message "nodes" key as a list
     * of Node objects.
     * Used in find_node responses and get_peers response.
     */
    public List<Node> nodes() {
        BValue arg = getArgument("nodes");
        if (arg == null) {
            return null;
        }
        byte[] compact = arg.toByteArray();
        if (compact.length % 26 != 0) {
            return null;
        }
        int size = compact.length / 26;
        List<Node> nodes = new ArrayList<Node>(size);
        byte[] id = new byte[20];
        byte[] ip = new byte[4];
        byte[] port = new byte[2];
        for (int i = 0; i < size; i++) {
            System.arraycopy(compact, 26*i, id, 0, 20);
            System.arraycopy(compact, 26*i+20, ip, 0, 4);
            System.arraycopy(compact, 26*i+24, port, 0, 2);
            Node node = null;
            try {
                node = new Node(id, ip, port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                continue;
            }
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * Returns a new "find_node" query.
     * See http://bittorrent.org/beps/bep_0005.html#find-node
     * @param id The id of the node we're looking for.
     */
    public static KRPCMessage findNode(byte[] id) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.find_node);
        msg.setArgument("target", new BValue(id));
        return msg;
    }

    /**
     * Returns a new "get_peers" query.
     * See http://bittorrent.org/beps/bep_0005.html#get-peers
     * @param infoHash The info hash of the torrent for which we want peers
     */
    public static KRPCMessage getPeers(byte[] infoHash) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.get_peers);
        msg.setArgument("info_hash", new BValue(infoHash));
        return msg;
    }

    /**
     * Returns a new "announce_peer" query.
     * See http://bittorrent.org/beps/bep_0005.html#find-node
     * @param infoHash The info hash of the torrent we're downloading
     * @param port The port on which we're listening for peers connection
     * @param token A token from a previous "get_peers" response.
     */
    public static KRPCMessage announcePeer(byte[] infoHash, int port, int token) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.announce_peer);
        msg.setArgument("info_hash", new BValue(infoHash));
        msg.setArgument("port", new BValue(port));
        msg.setArgument("token", new BValue(token));
        return msg;
    }

    /**
     * Returns a new "ping" query.
     * See http://bittorrent.org/beps/bep_0005.html#ping
     */
    public static KRPCMessage ping() {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.ping);
        return msg;
    }

    /**
     * Returns e new empty response.
     * Used to respond to "ping" and "announce_peer" queries.
     */
    public KRPCMessage emptyResponse() {
        KRPCMessage resp = new KRPCMessage(Type.response);
        return resp;
    }

    /**
     * Returns a new "find_node" response.
     * @param  nodes A list of node for the "nodes" key of the message
     */
    public static KRPCMessage findNodeResponse(List<Node> nodes) {
        KRPCMessage resp = new KRPCMessage(Type.response);

        byte[][] nodesList = new byte[nodes.size()][26];
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            nodesList[i] = ByteArrays.compact(node.id(), node.ip(), node.port());
        }
        resp.setArgument("nodes", new BValue(ByteArrays.concat(nodesList)));
        return resp;
    }

    /**
     * Returns a new "get_peers" response with a list of peers
     * @param peerInfos A list of peers in compact ip/port format for the "values" key
     */
    public static KRPCMessage getPeersResponse(List<byte[]> peerInfos) {
        KRPCMessage resp = new KRPCMessage(Type.response);

        List<BValue> infoList = new ArrayList<BValue>();
        for (byte[] peerInfo : peerInfos) {
            infoList.add(new BValue(peerInfo));
        }
        resp.setArgument("values", new BValue(infoList));
        resp.setArgument("token", new BValue(NodeManager.instance().token()));
        return resp;
    }

    /**
     * Returns a new "get_peers" response with a list of nodes.
     * @param nodes A list of nodes in compact node info format for the "nodes" key
     */
    public static KRPCMessage getPeersNodeResponse(List<Node> nodes) {
        KRPCMessage resp = findNodeResponse(nodes);
        resp.setArgument("token", new BValue(NodeManager.instance().token()));
        return resp;
    }
}
