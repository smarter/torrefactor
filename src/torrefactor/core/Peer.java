package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.SecureRandom;

/**
 * A Bittorent peer with the ability to read and write messages to.
 */
public class Peer implements Runnable, PeerConnectionListener  {
    private static Logger LOG = new Logger();
    private static Config CONF = Config.getConfig();

    static final int PEER_TIMEOUT =  2*60*1000; // in ms
    static final int SLEEP_DELAY = 10; // in ms

    private LinkedList<Message> msgOutQueue;
    private LinkedList<DataBlockInfo> peerRequestQueue;
    private LinkedList<DataBlockInfo> ownRequestQueue;

    private volatile boolean isValid = true;
    private volatile boolean isStopped = false;

    byte[] id;
    private byte[] reservedBytes;
    private InetAddress address;
    private int port;
    private Torrent torrent;
    private PeerConnection connection;

    //FIXME we likely should use AtomicLong for downloaded and uploaded
    private long downloaded = 0;
    private long uploaded = 0;

    public byte[] bitfield;
    private byte[] reserved;
    private volatile boolean isChoked = true;
    private volatile boolean isChokingUs = true;
    private volatile boolean isInteresting = false;
    private volatile boolean isInterestedInUs = false;



    public Peer (InetAddress address, int port, Torrent torrent)
    throws UnknownHostException, IOException {
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

            //TODO: remove, for testing only
            setChoked(false);
            setInteresting(true);

        try {
            long time = System.currentTimeMillis();
            while (this.connection.isConnected() && this.isValid) {
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

                while (this.msgOutQueue.size() > 0) {
                    Message msg = this.msgOutQueue.poll();
                    this.connection.send(msg);
                }

                if (this.ownRequestQueue.size() > 0) {
                    DataBlockInfo info = this.ownRequestQueue.poll();
                    Message msg = new RequestMessage(info);
                    this.connection.send(msg);
                }

                if (this.peerRequestQueue.size() > 0) {
                    DataBlockInfo info = this.peerRequestQueue.poll();
                    sendBlock(info);
                }

                if (System.currentTimeMillis() - time > PEER_TIMEOUT / 2) {
                    keepAlive();
                    time = System.currentTimeMillis();
                }
                
            }

        } catch (Exception e) {
            e.printStackTrace();
            invalidate();
        }
    }

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
    }

    private void keepAlive() throws IOException {
        Message msg = new KeepAliveMessage();
        this.msgOutQueue.offer(msg);
    }
    
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

    public boolean isRequestQueueFull() {
        return this.ownRequestQueue.size() >= 
            this.torrent.peerManager.MAX_QUEUED_REQUESTS;
    }

    public long downloaded() {
        return this.downloaded;
    }

    public long uploaded() {
        return this.uploaded;
    }

    public long popDownloaded() {
        long poped = this.downloaded;
        this.downloaded = 0;
        return poped;
    }

    public long popUploaded() {
        long poped = this.uploaded;
        this.uploaded = 0;
        return poped;
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

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = 7 - index % 8;
        return (((this.bitfield[byteIndex] >>> offset) & 1) == 1);
    }

    public void sendRequest(DataBlockInfo info) {
        LOG.debug("Sending request: pieceIndex: " + info.pieceIndex()
                  + " offset: " + info.offset() + " ");
        this.ownRequestQueue.offer(info);
    }

    public void sendHave(int piece) {
        Message msg = new HaveMessage(piece);
        this.msgOutQueue.offer(msg);
    }

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

    private static String arrayToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (!Character.isISOControl((char) data[i])) {
                sb.append((char) data[i]);
            }
        }
        return sb.toString();
    }

    public void onChokeMessage (ChokeMessage msg) {
        this.isChokingUs = true;
    }

    public void onUnchokeMessage (UnchokeMessage  msg) {
        this.isChokingUs = false;
    }

    public void onInterestedMessage (InterestedMessage msg) {
        this.isInterestedInUs = true;
    }

    public void onNotInterestedMessage (NotInterestedMessage msg) {
        this.isInterestedInUs = false;
    }

    public void onHaveMessage (HaveMessage msg) {
        int byteIndex = msg.index / 8;
        this.bitfield[byteIndex] |= 1 << 7 - (msg.index % 8);
    }

    public void onBitfieldMessage (BitfieldMessage msg) {
        if (msg.bitfield.length != this.bitfield.length) {
            LOG.error("Wrong bitfield length, got: " + msg.bitfield.length
                      + " expected: " + this.bitfield.length);
            invalidate();
        }
        this.bitfield = msg.bitfield;
        LOG.debug("Bitfield: " + ByteArrays.toHexString(msg.bitfield));
    }

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

    public void onPieceMessage (PieceMessage msg) {
        try {
            this.torrent.pieceManager.putBlock(
                    msg.index, msg.offset, msg.block);
            downloaded += msg.block.length;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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

    public void onPortMessage (PortMessage msg) {
        // FIXME: do we have to do what is commented out there or what is in
        //        PortMessage? In any case, the right thing should be in
        //        PortMessage.

        //int lowByte = socketInput.read();
        //int highByte = socketInput.read();
        //int port = (highByte << 8) | lowByte;
        //NodeManager.add(this.address, port);

        //NodeManager.add(this.address, msg.port);
    }

    public void onUnknownMessage (UnknownMessage msg) {
        LOG.warning("Ignoring unknown message");
    }

    public void onKeepAliveMessage () {};

    public void onHandshake(byte[] peerId, byte[] reserved) {
        this.id = peerId;
        this.reserved = reserved;
    }

    public void onConnectionClosed () {
    }

}

