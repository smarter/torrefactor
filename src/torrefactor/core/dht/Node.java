/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

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
     * Set this node token, normally the value comes from
     * the "token" key in a "get_peers" response.
     */
    public void setToken(int token) {
        this.token = token;
    }

    /**
     * Set a node status to be bad. From BEP5: "Nodes become bad when they fail
     * to respond to multiple queries in a row.
     */
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
