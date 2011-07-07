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

import torrefactor.util.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A DHT Bucket. Used to store a fixed amount of nodes and decide
 * whether or not an old node should be removed when a new one is added
 */
public class Bucket {
    private List<Node> nodes;
    static final int CAPACITY = 8;

    /**
     * Create a new bucket using nodes as an initial list of nodes
     */
    public Bucket(List<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * Create a new empty bucket.
     */
    public Bucket() {
        this(new ArrayList<Node>(CAPACITY + 1));
    }

    /**
     * Returns whether or not this bucket has its maximum amount of nodes
     */
    public boolean isFull() {
        return nodes.size() >= this.CAPACITY;
    }

    /**
     * Try to add a node to the bucket.
     * @return true if the node was successfully added, false otherwise
     */
    public boolean add(Node newNode) {
        if (isFull()) {
            ListIterator<Node> iter = nodes.listIterator();
            while (iter.hasNext()) {
                Node node = iter.next();
                switch (node.status()) {
                case good: {
                    continue;
                }
                case bad: {
                    iter.set(newNode);
                    return true;
                }
                case questionable: {
                    try {
                        NodeManager.instance().ping(node)
                                       .get(NodeManager.RESPONSE_TIMEOUT,
                                            TimeUnit.MILLISECONDS);
                    } catch(Exception e) {
                        iter.set(newNode);
                        return true;
                    }
                    node.refresh();
                    break;
                }
                }
            }
            return false;
        }
        nodes.add(newNode);
        return true;
    }

    /**
     * Return an immutable view of the nodes in the bucket
     */
    public List<Node> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Return the node with this particular if it exists in this bucket,
     * null otherwise.
     */
    public Node node(byte[] id) {
        for (Node node : nodes) {
            if (Arrays.equals(id, node.id())) {
                return node;
            }
        }
        return null;
    }

    /**
     * Remove all elements of this bucket whose common prefix
     * with array is less than prefixThresold and put them in a
     * new bucket that is returned.
     */
    public Bucket splitBucket(byte[] array, int prefixThresold) {
        ArrayList<Node> splitList = new ArrayList<Node>(this.CAPACITY + 1);
        Iterator<Node> iter = nodes.iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (ByteArrays.commonPrefix(node.id(), array) >= prefixThresold) {
                continue;
            }
            iter.remove();
            splitList.add(node);
        }
        return new Bucket(splitList);
    }
}
