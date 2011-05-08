package torrefactor.core.dht;

import torrefactor.core.dht.*;
import torrefactor.util.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class KRPCMessage {
    public enum Type {
        query, response, error
    };

    public enum QueryType {
        ping, find_node, get_peers, announce_peer
    };

    public enum ErrorType {
        GenericError, ServerError, ProtocolError, UnknownMethod, invalid
    };

    private Map<String, BValue> message;

    public KRPCMessage(byte[] _message) throws InvalidBDecodeException {
        ByteArrayInputStream stream = new ByteArrayInputStream(_message);
        try {
            this.message = BDecode.decodeDict(stream);
        } catch (IOException e) {
            //can't happen with a ByteArrayInputStream according to the documentation
            e.printStackTrace();
        }
    }

    public KRPCMessage(Type type) {
        this.message = new HashMap<String, BValue>();
        setType(type);
        setArgument("id", new BValue(NodeManager.instance().id));
    }

    public void setTransactionId(int id) {
        this.message.put("t", new BValue(id));
    }

    public byte[] toByteArray() {
        return BEncode.encode(new BValue(this.message));
    }

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

    private void setQuery(QueryType type) {
        this.message.put("y", new BValue("q"));
        this.message.put("q", new BValue(type.toString()));
    }

    public Type type() {
        if (this.message.get("y").toString().equals("y")) {
            return Type.query;
        } else if (this.message.get("r").toString().equals("r")) {
            return Type.response;
        } else {
            return Type.error;
        }
    }

    public QueryType queryType() {
        if (type() != Type.query) {
            return null;
        }
        return QueryType.valueOf(this.message.get("q").toString());
    }

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

    private void setArgument(String key, BValue value) {
        Map<String, BValue> argMap = argumentMap();
        if (argMap == null) {
            argMap = new HashMap<String, BValue>();
            this.message.put(key, new BValue(argMap));
        }
        argMap.put(key, value);
    }

    private BValue getArgument(String key) {
        Map<String, BValue> argMap = argumentMap();
        if (argMap == null) {
            return null;
        }
        return argMap.get(key);
    }

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

    public byte[] nodeId() {
        return getArgument("id").toByteArray();
    }

    public int token() {
        return getArgument("token").toInt();
    }

    public byte[] target() {
        return getArgument("target").toByteArray();
    }

    public byte[] infoHash() {
        return getArgument("info_hash").toByteArray();
    }

    public int port() {
        return getArgument("port").toInt();
    }

    public int transactionId() {
        return this.message.get("t").toInt();
    }

    /**
     * Returns the "peers" argument of the message as a list
     * of "compact ip/port" or returns null if the argument does
     * not exist.
     */
    public List<byte[]> peers() {
        BValue arg = getArgument("peers");
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

    public static KRPCMessage findNode(byte[] id) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.find_node);
        msg.setArgument("target", new BValue(id));
        return msg;
    }

    public static KRPCMessage getPeers(byte[] infoHash) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.get_peers);
        msg.setArgument("info_hash", new BValue(infoHash));
        return msg;
    }

    public static KRPCMessage announcePeer(byte[] infoHash, int port, int token) {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.announce_peer);
        msg.setArgument("info_hash", new BValue(infoHash));
        msg.setArgument("port", new BValue(port));
        msg.setArgument("token", new BValue(token));
        return msg;
    }

    public static KRPCMessage ping() {
        KRPCMessage msg = new KRPCMessage(Type.query);
        msg.setQuery(QueryType.ping);
        return msg;
    }

    public KRPCMessage emptyResponse() {
        KRPCMessage resp = new KRPCMessage(Type.response);
        return resp;
    }

    public static KRPCMessage findNodeResponse(List<Node> nodes) {
        KRPCMessage resp = new KRPCMessage(Type.response);

        StringBuilder infoSb = new StringBuilder(nodes.size() * 26 + 1);
        for (Node node : nodes) {
            infoSb.append(ByteArrays.compact(node.id(), node.ip(), node.port()));
        }
        resp.setArgument("nodes", new BValue(infoSb.toString()));
        return resp;
    }

    /**
     * Response to a get_peers message with a list of peers in compact format
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
     * Response to a get_peers message with a list of nodes
     */
    public static KRPCMessage getPeersNodeResponse(List<Node> nodes) {
        KRPCMessage resp = findNodeResponse(nodes);
        resp.setArgument("token", new BValue(NodeManager.instance().token()));
        return resp;
    }
}
