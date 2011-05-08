package torrefactor.core.dht;

import java.net.*;
import java.util.*;

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

    public Node(byte[] _id, InetAddress _ip, int _port) {
        this.id = new byte[160/8];
        System.arraycopy(_id, 0, this.id, 0, 160/8);
        this.ip = _ip;
        this.port = _port;
        this.lastChanged = System.currentTimeMillis();
        this.token = 0;
        this.isBad = false;
    }

    public Node(byte[] _id, byte[] _ip, byte[] _port) throws UnknownHostException {
        this(_id, InetAddress.getByAddress(_ip),
             ((_port[0] & 0xFF) << 8) | (_port[1] & 0xFF));
    }

    public byte[] id() {
        return this.id;
    }

    public InetAddress ip() {
        return this.ip;
    }

    public int port() {
        return this.port;
    }

    public int token() {
        return this.token;
    }

    public void setToken(int _token) {
        this.token = _token;
    }

    public void setBad(boolean bad) {
        this.isBad = bad;
    }

    public Status status() {
        if (this.isBad) {
            return Status.bad;
        }
        
        if ((System.currentTimeMillis() - this.lastChanged) <= FRESH_LIMIT) {
            return Status.questionable;
        }
        return Status.good;    
    }

    public void refresh() {
        this.lastChanged = System.currentTimeMillis();
    }
}
