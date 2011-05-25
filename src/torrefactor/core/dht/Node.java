package torrefactor.core.dht;

import java.net.*;
import java.util.*;

/**
 * A DHT node in the DHT network. They should be stored in a Bucket.
 */
public class Node {
    /**
     * Described in http://bittorrent.org/beps/bep_0005.html#routing-table
     */
    public enum Status {
        good, questionable, bad
    }

    private byte[] id;
    private InetAddress ip;
    private int port;
    private int token;
    private boolean isBad;

    private long lastChanged;

    static final int FRESH_LIMIT = 15*60*1000; // in milliseconds

    /**
     * Create a new node with id, ip, port as specified
     */
    public Node(byte[] id, InetAddress ip, int port) {
        this.id = new byte[160/8];
        System.arraycopy(id, 0, this.id, 0, 160/8);
        this.ip = ip;
        this.port = port;
        this.lastChanged = System.currentTimeMillis();
        this.token = -1;
        this.isBad = false;
    }

    /**
     * Create a new node with id, ip, port as specified
     */
    public Node(byte[] id, byte[] ip, byte[] port) throws UnknownHostException {
        this(id, InetAddress.getByAddress(ip),
             ((port[0] & 0xFF) << 8) | (port[1] & 0xFF));
    }

    /**
     * Returns this node id
     */
    public byte[] id() {
        return this.id;
    }

    /**
     * Returns this node ip
     */
    public InetAddress ip() {
        return this.ip;
    }

    /**
     * Return this node peer
     */
    public int port() {
        return this.port;
    }

    /**
     * Return this node token if it was set, -1 otherwise
     */
    public int token() {
        return this.token;
    }

    /**
     * Set this node token as 
     */
    public void setToken(int token) {
        this.token = token;
    }

    public void setBad(boolean bad) {
        this.isBad = bad;
    }

    /**
     * Return this node status, as described in:
     * http://bittorrent.org/beps/bep_0005.html#routing-table
     */
    public Status status() {
        if (this.isBad) {
            return Status.bad;
        }
        
        if ((System.currentTimeMillis() - this.lastChanged) <= FRESH_LIMIT) {
            return Status.questionable;
        }
        return Status.good;    
    }

    /**
     * Refresh this node as described in:
     * http://bittorrent.org/beps/bep_0005.html#routing-table
     */
    public void refresh() {
        this.lastChanged = System.currentTimeMillis();
    }
}
