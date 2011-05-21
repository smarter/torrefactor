package torrefactor.core;

import torrefactor.core.*;
import torrefactor.core.dht.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a Bittorrent peer.
 */
public class Peer implements Runnable, PeerConnectionListener  {
    private static Logger LOG = new Logger();
    private static Config CONF = Config.getConfig();

    static final int PEER_TIMEOUT =  2*60*1000; // in ms
    static final int SLEEP_DELAY = 10; // in ms

    /** The bitfield of this peer*/
    byte[] bitfield;

    /** The peer id of this peer*/
    byte[] id;

    /** 
     * This LinkedList contains message which should be send as soon as
     * possible to the peer.
     * */
    private LinkedList<Message> msgOutQueue;

    /**
     * This LinkedList contains the requests made by the peer.
     */
    private LinkedList<DataBlockInfo> peerRequestQueue;

    /**
     * This Linkedlist contains our request to send to the peer.
     */
    private LinkedList<DataBlockInfo> ownRequestQueue;

    private volatile boolean isValid = true;
    private volatile boolean isStopped = false;

    private byte[] reservedBytes;
    private InetAddress address;
    private int port;
    private Torrent torrent;
    private PeerConnection connection;

    private AtomicLong downloaded = new AtomicLong(0);
    private AtomicLong uploaded = new AtomicLong(0);
    private SpeedMeter downloadedSpeed = new SpeedMeter(0);
    private SpeedMeter uploadedSpeed = new SpeedMeter(0);

    private byte[] reserved;
    private volatile boolean isChoked = true;
    private volatile boolean isChokingUs = true;
    private volatile boolean isInteresting = false;
    private volatile boolean isInterestedInUs = false;



    /**
     * Create a new peer.
     *
     * @param address    the InetAddress of the peer
     * @param port        the port where the peer is listening
     * @param torrent    the torrent to use with this connection
     */
    public Peer (InetAddress address, int port, Torrent torrent) {
        this.msgOutQueue = new LinkedList<Message>();
        this.peerRequestQueue = new LinkedList<DataBlockInfo>();
        this.ownRequestQueue = new LinkedList<DataBlockInfo>();
        this.address = address;
        this.port = port;
        this.torrent = torrent;
        this.bitfield = new byte[this.torrent.pieceManager.bitfield.length];
        Arrays.fill(this.bitfield, (byte) 0);
        LOG.setHeader(this.toString());

        this.connection = new PeerConnection (address, port, this);
    }


    /**
     * The main loop of the peer.
     * The main loop is responsible to send and receive the messages to/from
     * the peer. It exit when the connection is closed.
     */
    public void run() {
        try {
            this.connection.connect();
            
            byte reserved[] = { 0, 0, 0, 0, 0, 0, 0, 0};
            //reserved[7] = 1; //DHT support
            if (CONF.getPropertyBoolean("Peer.UseStupidEncryption")) {
                reserved[7] |= (1 << 4);
            }

            boolean handshake = this.connection.handshake (
                    this.torrent.infoHash,
                    reserved,
                    this.torrent.peerManager.peerId); 
            if (!handshake) {
                invalidate();
                return;
            }
            LOG.debug("Handshake done");
            
            Message bitfield = new BitfieldMessage(
                    this.torrent.pieceManager.bitfield);
            this.connection.send(bitfield);
        } catch (Exception e) {
            invalidate();
            e.printStackTrace();
        }

            //TODO: remove, when peerManager has an algorithm to handle this
            setChoked(false);
            setInteresting(true);

        try {
            long time = System.currentTimeMillis();
            while (this.connection.isConnected() && this.isValid) {

                // Wait a bit for a message but not to much. (It doesn't wait
                // more than necessary
                Message r = null;
                int count = 0;
                while (r == null && count < 10) {
                    r = this.connection.receive();
                    try {
                        Thread.currentThread().sleep(SLEEP_DELAY);
                    } catch (InterruptedException e) {
                        LOG.debug("Got InterruptedException while sleeping.");
                        Thread.currentThread().interrupt();
                        invalidate();
                    }
                    count++;
                }

                // Send all the messages frow msgOutQueue
                while (this.msgOutQueue.size() > 0) {
                    Message msg = this.msgOutQueue.poll();
                    this.connection.send(msg);
                }

                // Send one of our request
                if (this.ownRequestQueue.size() > 0) {
                    DataBlockInfo info = this.ownRequestQueue.poll();
                    Message msg = new RequestMessage(info);
                    this.connection.send(msg);
                }

                // Respond to a request of the peer
                if (this.peerRequestQueue.size() > 0) {
                    DataBlockInfo info = this.peerRequestQueue.poll();
                    sendBlock(info);
                }

                // Send keep-alive according to Bittorrent specificiations
                if (System.currentTimeMillis() - time > PEER_TIMEOUT / 2) {
                    keepAlive();
                    time = System.currentTimeMillis();
                }
                
            }

        } catch (Exception e) {
            e.printStackTrace();
            invalidate();
        }

        if (this.isValid) invalidate();
    }

    /**
     * Sends a block of data to the peer.
     *
     * @param info    The DataBlockInfo identifying the block to send to the
     *                peer
     * @throws IOException if PeerConnetion.send(Message) throws it
     */
    private void sendBlock(DataBlockInfo info)
    throws IOException {
        byte[] block = null;
        try {
            block = this.torrent.pieceManager.getBlock(
                    info.pieceIndex, info.offset, info.length);
        } catch (Exception e) {
            LOG.error("Exception while getting block:");
            e.printStackTrace();
        }

        if (block == null) {
            LOG.debug("Block is null " +  info);
        }
        Message msg = new PieceMessage(
                info.pieceIndex, info.offset, block);
        this.connection.send(msg);

        this.uploaded.addAndGet(block.length);
        this.torrent.incrementUploaded(block.length);
    }

    /**
     * Send a keep-alive to the peer.
     *
     * @throws IOException if PeerConnection.send(Message) throws it
     */
    private void keepAlive() throws IOException {
        Message msg = new KeepAliveMessage();
        this.msgOutQueue.offer(msg);
    }
    
    /**
     * Set the choked status of this peer.
     *
     * @param b true to choke the peer, false to unchoke it
     */
    public void setChoked(boolean b) {
        if (this.isChoked != b) {
            this.isChoked = b;
        }
        Message msg;
        if (b) {
            msg = new ChokeMessage();
        } else {
            msg = new UnchokeMessage();
        }
        this.msgOutQueue.offer(msg);
    }

    /**
     * Set the interesting status of this peer.
     *
     * @param b true if this peer is interesting
     */
    public void setInteresting(boolean b) {
        this.isInteresting = b;
        Message msg;
        if (b) {
            msg = new InterestedMessage();
        } else {
            msg = new NotInterestedMessage();
        }
        this.msgOutQueue.offer(msg);
    }

    /**
     * Returns whether requests can be made to this peer.
     *
     * @return true if a request can be made to this peer
     */
    public boolean canRequest() {
        boolean b = this.isValid && this.connection.isConnected() &&
            !this.isChokingUs && this.ownRequestQueue.size() <
            this.torrent.peerManager.MAX_QUEUED_REQUESTS;
        return b;
    }

    public boolean isChoked() {
        return this.isChoked;
    }

    public boolean isChokingUs() {
        return this.isChokingUs;
    }

    public boolean isInteresting() {
        return this.isInteresting;
    }

    public boolean isInterestedInUs() {
        return this.isInterestedInUs;
    }

    public long downloaded() {
        return this.downloaded.longValue();
    }

    public double downloadSpeed() {
        return this.downloadedSpeed.getSpeed(this.downloaded.longValue());
    }

    public long uploaded() {
        return this.uploaded.longValue();
    }

    public double uploadedSpeed() {
        return this.uploadedSpeed.getSpeed(this.uploaded.longValue());
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void invalidate() {
        LOG.warning("Invalidate");
        this.isValid = false;
        try {
            this.connection.close();
        } catch (IOException e) {
            LOG.error ("Exception while closing connection:");
            LOG.error (e.getMessage());
        }
    }

    public boolean isConnected() {
        return this.isValid && this.connection.isConnected();
    }

    @Deprecated
    public int firstPiece() {
        if (this.bitfield == null) return -1;
        for (int i = 0; i < this.bitfield.length; i++) {
            if (this.bitfield[i] == 0) continue;
            int offset = 7;
            while (this.bitfield[i] >>> offset == 0) {
                offset--;
            }
            return 8*i + 7 - offset;
        }
        LOG.debug("No piece: " + arrayToString(this.id));
        return -1;
    }

    /**
     * Check whether this peer has a particular piece or not.
     *
     * @param index the index of the piece
     * @return true if this piece has the piece at the given index
     */
    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = 7 - index % 8;
        return (((this.bitfield[byteIndex] >>> offset) & 1) == 1);
    }

    /**
     * Send a request to the peer.
     *
     * @param info the DataBlockInfo indentifying the block to request
     */
    public void sendRequest(DataBlockInfo info) {
        LOG.debug("Sending request: pieceIndex: " + info.pieceIndex()
                  + " offset: " + info.offset() + " ");
        this.ownRequestQueue.offer(info);
    }

    /**
     * Send a have message to the peer.
     *
     * @param piece the index of the piece
     */
    public void sendHave(int piece) {
        Message msg = new HaveMessage(piece);
        this.msgOutQueue.offer(msg);
    }
    
    /**
     * Return the string representation of this peer.
     * It is of the form: "Peer/0.0.0.0:9999"
     */
    @Override
    public String toString() {
        return "Peer" + this.address + ':' + this.port;
    }

    public InetAddress getAddress () {
        return this.address;
    }

    public int getPort () {
        return this.port;
    }

    public String getIdAsString () {
        if (this.id == null) return "";
        return  arrayToString(this.id);
    }

    /**
     * Return a byte array as string. Each byte is considered as a char and
     * byte corresponding to control chars are ignored.
     *
     * @param data the byte array
     * @return the string representation
     */
    private static String arrayToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (!Character.isISOControl((char) data[i])) {
                sb.append((char) data[i]);
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onChokeMessage (ChokeMessage msg) {
        this.isChokingUs = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnchokeMessage (UnchokeMessage  msg) {
        this.isChokingUs = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInterestedMessage (InterestedMessage msg) {
        this.isInterestedInUs = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNotInterestedMessage (NotInterestedMessage msg) {
        this.isInterestedInUs = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onHaveMessage (HaveMessage msg) {
        int byteIndex = msg.index / 8;
        this.bitfield[byteIndex] |= 1 << 7 - (msg.index % 8);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBitfieldMessage (BitfieldMessage msg) {
        if (msg.bitfield.length != this.bitfield.length) {
            LOG.error("Wrong bitfield length, got: " + msg.bitfield.length
                      + " expected: " + this.bitfield.length);
            invalidate();
        }
        this.bitfield = msg.bitfield;
        LOG.debug("Bitfield: " + ByteArrays.toHexString(msg.bitfield));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestMessage (RequestMessage msg) {
        LOG.debug("Request, index: " + msg.index
                        + " offset: " + msg.offset
                        + " blockLength : " + msg.length);

        DataBlockInfo info = new DataBlockInfo(
                msg.index, msg.offset, msg.length);
        if (! this.peerRequestQueue.offer(info)) {
            LOG.debug("Ignoring request because queue is full");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPieceMessage (PieceMessage msg) {
        try {
            this.torrent.pieceManager.putBlock(
                    msg.index, msg.offset, msg.block);
            this.downloaded.addAndGet(msg.block.length);
            this.torrent.incrementDownloaded(msg.block.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancelMessage (CancelMessage msg) {
        ListIterator<DataBlockInfo> iter =
            this.peerRequestQueue.listIterator(0);
        while (iter.hasNext()) {
            DataBlockInfo info = iter.next();
            if (msg.index == info.pieceIndex && msg.offset == info.offset &&
                    msg.length == info.length) {
                iter.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPortMessage (PortMessage msg) {
        NodeManager.instance().addPeer(this.address, msg.port, this.torrent.infoHash);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUnknownMessage (UnknownMessage msg) {
        LOG.warning("Ignoring unknown message");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKeepAliveMessage () {};

    /**
     * {@inheritDoc}
     */
    @Override
    public void onHandshake(byte[] peerId, byte[] reserved) {
        this.id = peerId;
        this.reserved = reserved;
    }

}

