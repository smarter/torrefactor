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
