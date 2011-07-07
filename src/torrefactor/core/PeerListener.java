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

package torrefactor.core;

import torrefactor.util.Logger;
import torrefactor.util.Config;

import java.io.*;
import java.net.*;


/**
 * This thread listen to new connection and start new Peer threads.
 */
public class PeerListener implements Runnable {
    private static final Logger LOG = new Logger();
    private static final Config CONF = Config.getConfig();
    ServerSocket serverSocket;
    int port;
    private volatile boolean listen = true;

    /**
     * Create a new PeerListener.
     *
     * @param port  the port to listen on
     */
    public PeerListener (int port) {
        this.port = port;
    }

    /**
     * The main loop of the peer listener. It creates a new Peer thread for
     * each new connections.
     */
    public void run () {
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (Exception e) {
            LOG.error("Couldn't listen on port " + this. port + ": "
                      + e.getMessage());
            e.printStackTrace();
            return;
        }

        while (this.listen) {
            try {
                Socket peerSocket = this.serverSocket.accept();

                LOG.debug("Got new peer, starting peer thread");
                Peer peer = new Peer(peerSocket);
                new Thread(peer).start();
            } catch (Exception e) {
                LOG.error("ServerSocket.accept() failed: "
                          + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Stop the peer listener after ServerSocket.accept() returns.
     * You'll have to interrupt the thread to make ServerSocket.accept()
     * returns. Sometimes it seems to be not enough in which case you'll need
     * to find another way to kill it.
     * The only thing which always worked is to make the thread of PeerListener
     * a daemon Thread so it get killed when there aren't any normal Threads
     * anymore (and then the jvm exits…).
     */
    public void stop () {
        this.listen = false;
        try {
            this.serverSocket.close();
        } catch (Exception e) {
            LOG.error("Exception while closing ServerSocket: "
                      + e.getMessage());
            e.printStackTrace();
        }
    }
}
