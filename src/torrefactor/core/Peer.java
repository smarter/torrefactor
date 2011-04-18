package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Bittorent peer with the ability to read and write messages to.
 */
public class Peer implements Runnable {
    private static Logger LOG = new Logger();
    public enum MessageType {
        choke, unchoke, interested, not_interested, have, bitfield,
        request, piece, cancel, port
    }

    private Queue<DataBlockInfo> outQueue;

    // Numbers of answers we're waiting for
    private volatile int requested;

    private volatile boolean isValid = true;
    private volatile boolean isStopped = false;
    private boolean isConnected = false;

    byte[] id;
    private InetAddress ip;
    private int port;
    private Torrent torrent;
    private Socket socket;
    private DataInputStream socketInput;
    private DataOutputStream socketOutput;
    //FIXME we likely should use AtomicLong for downloaded and uploaded
    private long downloaded = 0;
    private long uploaded = 0;

    private byte[] bitfield;
    private boolean isChoked = true;
    private boolean isChokingUs = true;
    private boolean isInteresting = false;
    private boolean isInterestedInUs = false;

    static final int CONNECTION_TRIES = 5;

    // In milliseconds
    static final int CONNECT_TIMEOUT =  1000;
    static final int PEER_TIMEOUT =  2*60*1000;
    static final int SLEEP_DELAY = 10;

    // In bytes
    static final int SEND_BUFFER_SIZE = (1 << 18);
    static final int RECEIVE_BUFFER_SIZE = (1 << 18);

    public Peer(InetAddress _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.outQueue = new LinkedList<DataBlockInfo>();
        this.id = null;
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        this.bitfield = new byte[this.torrent.pieceManager.bitfield.length];
        Arrays.fill(this.bitfield, (byte) 0);
    }

    public void run() {
        int tries = 0;
        while (this.socket == null && (! this.isStopped)) {
            try {
                LOG.debug(this, "Connecting: " + this.ip.toString() + ':' + this.port);
                this.socket = new Socket();
                this.socket.setSendBufferSize(SEND_BUFFER_SIZE);
                this.socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                this.socket.setSoTimeout(PEER_TIMEOUT);
                InetSocketAddress address = new InetSocketAddress(this.ip, this.port);
                this.socket.connect(address, CONNECT_TIMEOUT);

                socketInput = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                LOG.debug(this, "Connected: " + this.ip.toString() + ':' + this.port);
                socketOutput = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                if (!handshake()) {
                    invalidate();
                    return;
                }
                sendMessage(MessageType.bitfield, null, this.torrent.pieceManager.bitfield);
                //TODO: remove, for testing only
                setChoked(false);
                setInteresting(true);
            } catch (Exception e) {
                tries++;
                if (this.socket != null) {
                    //this.socket.close();
                }
                this.socket = null;
                if (tries == CONNECTION_TRIES) {
                    LOG.error(this, e);
                    invalidate();
                    return;
                }
            }
        }
        this.isConnected = true;
        long time = System.currentTimeMillis();
        while (this.isValid && (! this.isStopped)) {
            try {
                readMessage();
                if (System.currentTimeMillis() - time > PEER_TIMEOUT / 2) {
                    keepAlive();
                    time = System.currentTimeMillis();
                }
                if (socketInput.available() != 0) {
                    continue;
                }
            } catch (IOException e) {
                LOG.error(this, e);
                invalidate();
                return;
            }
            try {
                Thread.currentThread().sleep(SLEEP_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                invalidate();
                return;
            }
        }
        if ( (this.socket != null) && (! this.socket.isClosed())) {
            try {
                this.socket.close ();
            } catch (IOException e) {
                LOG.error (this, "While closing socket of "
                                 + this.ip + " " + this.port + ":");
                LOG.error (this, e.getMessage());
            }
        }
    }

    private void readMessage() throws IOException {
        int length = socketInput.readInt();
        if (length == 0) {
            return;
        }
        int typeByte = socketInput.read();
        length--;
        if (typeByte < 0 || typeByte > 9) {
            LOG.debug(this, "Got unknown message " + typeByte
                            + " with length: " + length + " "
                            + arrayToString(this.id));
            return;
        }
        MessageType type = MessageType.values()[typeByte];
        readMessage(type, length);
    }

    private void readMessage(MessageType type, int length)
    throws IOException {
        LOG.debug(this, "Got message " + type.toString() + " "
                        + arrayToString(this.id) + " " + length);
        switch (type) {
        case choke: {
            this.isChokingUs = true;
            break;
        }
        case unchoke: {
            this.isChokingUs = false;
            break;
        }
        case interested: {
            this.isInterestedInUs = true;
            break;
        }
        case not_interested: {
            this.isInterestedInUs = false;
            break;
        }
        case have: {
            int index = socketInput.readInt();
            int byteIndex = index / 8;
            this.bitfield[byteIndex] |= 1 << 7 - (index % 8);
            break;
        }
        case bitfield: {
            if (length != this.bitfield.length) {
                LOG.debug(this, "Wrong bitfield length, got: " + length
                                + " expected: " + this.bitfield.length);
            }
            socketInput.readFully(this.bitfield);
            break;
        }
        case request: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            int blockLength = socketInput.readInt();
            LOG.debug(this, "Request, index: " + index
                            + " offset: " + offset
                            + " blockLength : " + blockLength);
            byte[] block = this.torrent.pieceManager.getBlock(index, offset, blockLength);
            if (block == null) return;
            sendBlock(index, offset, block);
            break;
        }
        case piece: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            byte[] block = new byte[length - 2*4];
            socketInput.readFully(block);
            downloaded += block.length;
            this.torrent.pieceManager.putBlock(index, offset, block);
            if (requested > 0) {
                requested--;
            }
            DataBlockInfo queuedInfo = outQueue.poll();
            if (queuedInfo != null && requested < this.torrent.peerManager.BLOCKS_PER_REQUEST) {
                LOG.debug(this, "Got block, sending new request");
                sendRequest(queuedInfo);
            }
            break;
        }
        //TODO
        case cancel: {
            break;
        }
        //DHT node port, see BEP 0005
        case port: {
            int lowByte = socketInput.read();
            int highByte = socketInput.read();
            int port = (highByte << 8) | lowByte;
            //NodeManager.add(this.ip, port);
            break;
        }
        default:
            break;
        }
    }

    private boolean handshake() throws IOException {
        LOG.debug(this, "Handshake start: "
                        + this.ip.toString() + ':' + this.port);
        socketOutput.writeByte(19);
        byte header[] = (new String("BitTorrent protocol")).getBytes();
        socketOutput.write(header);
        socketOutput.flush();
        byte reserved[] = { 0, 0, 0, 0, 0, 0, 0, 0};
        //reserved[7] = 1; //DHT support
        socketOutput.write(reserved);
        socketOutput.write(torrent.infoHash);
        socketOutput.write(torrent.peerManager.peerId);
        socketOutput.flush();
        int inLength = socketInput.read();
        if (inLength == -1) {
            return false;
        }
        byte[] inHeader = new byte[inLength];
        socketInput.readFully(inHeader);
        if (!Arrays.equals(header, inHeader)) {
            LOG.debug(this, "Unsupported protocol header: "
                            + new String(inHeader));
            return false;
        }
        byte[] inReserved = new byte[reserved.length];
        socketInput.readFully(inReserved);
        byte[] inInfoHash = new byte[20];
        socketInput.readFully(inInfoHash);
        if (!Arrays.equals(torrent.infoHash, inInfoHash)) {
            LOG.debug(this, "Wrong info_hash");
            return false;
        }
        id = new byte[20];
        socketInput.readFully(this.id);
        LOG.debug(this, "Handshake done: " + this.ip.toString() + ':'
                        + this.port + " id: " + arrayToString(this.id));
        return true;
    }

    public synchronized void setChoked(boolean b) throws IOException {
        if (this.isChoked != b) {
            this.isChoked = b;
        }
        MessageType type = b ? MessageType.choke : MessageType.unchoke;
        sendMessage(type, null, null);
    }

    public synchronized void setInteresting(boolean b) throws IOException {
        MessageType type = b ? MessageType.interested : MessageType.not_interested;
        sendMessage(type, null, null);
    }

    public byte[] bitfield() {
        return this.bitfield;
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

    public boolean isQueueFull() {
        return this.outQueue.size() >= this.torrent.peerManager.BLOCKS_PER_REQUEST;
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
        if (this.id != null) {
            LOG.debug(this, "## " + arrayToString(this.id) + " invalidated");
        } else {
            LOG.debug(this, "## " + this.ip.toString() + " invalidated");
        }
        this.isValid = false;
        try {
            this.socketInput.close();
            this.socketOutput.close();
            this.socket.close();
        } catch (Exception ignored) {}
    }

    public boolean isConnected() {
        return this.isValid && this.isConnected;
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
        LOG.debug(this, "No piece: " + arrayToString(this.id));
        return -1;
    }

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = 7 - index % 8;
        return (((this.bitfield[byteIndex] >>> offset) & 1) == 1);
    }

    private synchronized void keepAlive() throws IOException {
        LOG.debug(this, "KeepAlive :" + arrayToString(this.id));
        socketOutput.writeInt(0);
        socketOutput.flush();
    }

    private synchronized void sendMessage(MessageType type, int[] params, byte[] data) throws IOException {
        LOG.debug(this, "Sending " + type.toString()
                        + " to :" + arrayToString(this.id));
        int length = 1;
        if (params != null) {
            length += 4 * params.length;
        }
        if (data != null) {
            length += data.length;
        }
        socketOutput.writeInt(length);
        socketOutput.writeByte((byte) type.ordinal());
        if (params != null) {
            for (int param : params) {
                socketOutput.writeInt(param);
            }
        }
        if (data != null) {
            socketOutput.write(data);
        }
        socketOutput.flush();

        //Increment uploaded counter if we've sent some data
        if (type == MessageType.piece) {
            uploaded += data.length;
        }

    }

    public void queueRequest(DataBlockInfo info) throws IOException {
        LOG.debug(this, "Queued!");
        if (requested == 0) {
            sendRequest(info);
        } else {
            outQueue.add(info);
        }
    }

    //TODO: make asynchronous
    public void sendRequest(DataBlockInfo info)
    throws IOException {
        if (!isConnected()) return;
        int[] params = { info.pieceIndex(), info.offset(), info.length() };
        LOG.debug(this, "pieceIndex: " + info.pieceIndex()
                        + " offset: " + info.offset() + " ");
        sendMessage(MessageType.request, params, null);
    }

    public void sendBlock(int index, int offset, byte[] block)
    throws IOException {
        int[] params = { index, offset };
        sendMessage(MessageType.piece, params, block);
    }

    public void stop() {
        LOG.debug(this, "Stopping peer " + this.ip + " " + this.port);
        this.isStopped = true;
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
}

