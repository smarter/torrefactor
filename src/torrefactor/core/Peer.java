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
public class Peer implements Runnable {
    private static Logger LOG = new Logger();
    public enum MessageType {
        choke, unchoke, interested, not_interested, have, bitfield,
        request, piece, cancel, port,
        sendRSAKey, sendSymmetricKey,
        keepalive, invalid
    } // keepalive and invalid MUST be the last two elements of the enum

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
    private byte[] reservedBytes;

    static final int CONNECTION_TRIES = 5;

    // In milliseconds
    static final int CONNECT_TIMEOUT =  1000;
    static final int PEER_TIMEOUT =  2*60*1000;
    static final int SLEEP_DELAY = 10;

    // In bytes
    static final int SEND_BUFFER_SIZE = (1 << 18);
    static final int RECEIVE_BUFFER_SIZE = (1 << 18);

    public Peer (InetAddress _ip, int _port, Torrent _torrent)
    throws UnknownHostException, IOException {
        this.outQueue = new LinkedList<DataBlockInfo>();
        this.id = null;
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        this.bitfield = new byte[this.torrent.pieceManager.bitfield.length];
        Arrays.fill(this.bitfield, (byte) 0);
        LOG.setHeader("Peer" + this.ip + ':' + this.port);
    }

    public void run() {
        int tries = 0;
        while (this.socket == null && (! this.isStopped)) {
            try {
                LOG.debug("Connecting: " + this.ip.toString()
                                + ':' + this.port);
                this.socket = new Socket();
                this.socket.setSendBufferSize(SEND_BUFFER_SIZE);
                this.socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                this.socket.setSoTimeout(PEER_TIMEOUT);
                InetSocketAddress address = new InetSocketAddress(
                                                        this.ip, this.port);
                this.socket.connect(address, CONNECT_TIMEOUT);

                // FIXME: Why do we need Buffered streams?
                socketInput = new DataInputStream(
                                  new BufferedInputStream(
                                      this.socket.getInputStream()));
                LOG.debug("Connected: " + this.ip.toString()
                                + ':' + this.port);
                socketOutput = new DataOutputStream(
                                   new BufferedOutputStream(
                                       this.socket.getOutputStream()));
                if (!handshake()) {
                    invalidate();
                    return;
                }
                sendMessage(MessageType.bitfield, null,
                            this.torrent.pieceManager.bitfield);
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
                    LOG.error(e);
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
                LOG.error(e);
                invalidate();
                return;
            }
            try {
                Thread.currentThread().sleep(SLEEP_DELAY);
            } catch (InterruptedException e) {
                LOG.debug("Got InterruptedException while sleeping.");
                Thread.currentThread().interrupt();
                invalidate();
                return;
            }
        }
        if ( (this.socket != null) && (! this.socket.isClosed())) {
            try {
                this.socket.close();
            } catch (IOException e) {
                LOG.error ("While closing socket of "
                                 + this.ip + " " + this.port + ":");
                LOG.error (e.getMessage());
            }
        }
    }
    
    /**
     * Reads the header of the message and return the MessageType and the
     * length of the message.
     */
    private Pair<MessageType, Integer> getMessage() throws IOException {
        int length = socketInput.readInt();
        LOG.debug("getMessage: length = " + length);
        if (length == 0) {
            return new Pair<MessageType, Integer>(MessageType.keepalive,
                                                  length);
        }
        int typeByte = socketInput.read();
        length--;
        int lastTypeByte = MessageType.values()[MessageType.values().length-3]
                                                                    .ordinal();
        if (typeByte < 0 || typeByte > lastTypeByte) {
            LOG.warning("Got unknown message " + typeByte
                              + " with length: " + length + " "
                              + arrayToString(this.id));
            return new Pair<MessageType, Integer>(MessageType.invalid, length);
        }
        MessageType type = MessageType.values()[typeByte];
        LOG.debug("getMessage: type = " + type.toString());
        
        return new Pair<MessageType, Integer>(type, length);
    }

    private void readMessage() throws IOException {
        Pair<MessageType, Integer> pair = getMessage();
        MessageType type = pair.first();
        int length = pair.second();

        readMessage(type, length);
    }

    private void readMessage(MessageType type, int length)
    throws IOException {
        LOG.debug("Got message " + type.toString() + " "
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
                LOG.debug("Wrong bitfield length, got: " + length
                                + " expected: " + this.bitfield.length);
            }
            socketInput.readFully(this.bitfield);
            break;
        }
        case request: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            int blockLength = socketInput.readInt();
            LOG.debug("Request, index: " + index
                            + " offset: " + offset
                            + " blockLength : " + blockLength);
            byte[] block = this.torrent.pieceManager.getBlock(index, offset,
                                                              blockLength);
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
            if (queuedInfo != null
                && requested < this.torrent.peerManager.BLOCKS_PER_REQUEST){
                LOG.debug("Got block, sending new request");
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
        case sendRSAKey: {
            LOG.warning("Got unexpected sendRSAKey message");
            break;
        }
        case sendSymmetricKey: {
            LOG.warning("Got unexpected sendSymmetricKey message");
            break;
        }
        default:
            break;
        }
    }

    private boolean handshake() throws IOException {
        LOG.debug("Handshake start: "
                        + this.ip.toString() + ':' + this.port);

        // 19BitTorrent protocol
        socketOutput.writeByte(19);
        byte header[] = "BitTorrent protocol".getBytes();
        socketOutput.write(header);
        socketOutput.flush();

        // Reserved bytes
        byte reserved[] = { 0, 0, 0, 0, 0, 0, 0, 0};
        //reserved[7] = 1; //DHT support
        reserved[7] |= (1 << 4); // Stupid "encryption" protocol
        socketOutput.write(reserved);

        socketOutput.write(torrent.infoHash);
        socketOutput.write(torrent.peerManager.peerId);
        socketOutput.flush();

        int inLength = socketInput.read();
        if (inLength == -1) {
            return false;
        }

        // 19BitTorrent protocol
        byte[] inHeader = new byte[inLength];
        socketInput.readFully(inHeader);
        if (!Arrays.equals(header, inHeader)) {
            LOG.debug("Unsupported protocol header: "
                            + new String(inHeader));
            return false;
        }

        // Reserved bytes
        byte[] inReserved = new byte[reserved.length];
        socketInput.readFully(inReserved);
        this.reservedBytes = inReserved;

        byte[] inInfoHash = new byte[20];
        socketInput.readFully(inInfoHash);
        if (!Arrays.equals(torrent.infoHash, inInfoHash)) {
            LOG.warning("Wrong info_hash");
            return false;
        }

        this.id = new byte[20];
        socketInput.readFully(this.id);

        LOG.debug("Handshake done: " + this.ip.toString() + ':'
                        + this.port + " id: " + arrayToString(this.id));

        // Enable StupidEncryption if possible
        stupidEncryptionSetup();

        return true;
    }

    /**
     * Setups the StupidEncryption.
     * Returns true if the encryption streams where successfully created, false
     * otherwise.
     */ 
    private boolean stupidEncryptionSetup () {
        if ((this.reservedBytes[7] & (1 << 4)) == 0) {
            // Peer does not support stupid encryption
            return false;
        }

        int RSA_KEY_BITLENGTH = 1024;
        int XOR_LENGTH = 128;

        Rsa rsa = new Rsa (RSA_KEY_BITLENGTH);
        Pair<DataInputStream, DataOutputStream> oldStreams = null;
        SecureRandom srandom = new SecureRandom();
        byte[] key = new byte[XOR_LENGTH];
        srandom.nextBytes(key);

        try {
            stupidEncryptionSendRSAKey(rsa, rsa.getModulo().length-1);
            int mysteriousN = stupidEncryptionReceiveRSAKey(rsa);
            oldStreams = stupidEncryptionEnableRSAStreams(rsa);
            stupidEncryptionSendSymmetricKey(key);
            key = stupidEncryptionReceiveSymmetricKey(mysteriousN);
            stupidEncryptionDisableRSAStreams(oldStreams);
            stupidEncryptionEnableSymmetricStreams(key, mysteriousN);
        } catch (Exception e) {
            if (oldStreams != null) {
                try {
                    stupidEncryptionDisableRSAStreams(oldStreams);
                } catch (Exception f) {}
            }
            LOG.error(e);
            e.printStackTrace();
            LOG.debug("Failed to enable StupidEncryption, continuing with the"
                      + " standard protocol.");
            return false;
        }

        LOG.info("Stupid encryption enabled.");
        return true;
    }

    private void stupidEncryptionSendRSAKey(Rsa rsa, int mysteriousN)
    throws IOException {
        byte[] key = rsa.getPublicKey();
        byte[] modulo = rsa.getModulo();
        byte[] data = new byte[4 + 4 + 4 + key.length + modulo.length];
        byte[] array;

        // 4 bytes - Mysterious N
        array = ByteArrays.fromInt(modulo.length-1);
        System.arraycopy(array, 0, data, 0, 4);

        // 4 bytes - length of key in byte
        array = ByteArrays.fromInt(key.length);
        System.arraycopy(array, 0, data, 4, 4);

        // key
        System.arraycopy(key, 0, data, 8, key.length);

        // 4 bytes - length of modulo in bytes
        array = ByteArrays.fromInt(modulo.length);
        System.arraycopy(array, 0, data, 8 + key.length, 4);

        // modulo
        System.arraycopy(modulo, 0, data, 12 + key.length, modulo.length);

        sendMessage(MessageType.sendRSAKey, null, data);
        LOG.debug("RSA key sent.");
    }

    private int stupidEncryptionReceiveRSAKey (Rsa rsa)
    throws IOException {
        Pair<MessageType, Integer> header = getMessage();
        MessageType messageType = header.first();
        int messageLength = header.second();

        if (messageType != MessageType.sendRSAKey) {
            LOG.warning("Unexpected MessageType: got " + messageType
                        + " while expecting " + MessageType.sendRSAKey);
        }

        int mysteriousN = this.socketInput.readInt();    // in bits
        LOG.debug("mysteriousN: " + mysteriousN);

        int keyLength = this.socketInput.readInt();    // in bytes
        LOG.debug("keyLength: " + keyLength);
        byte[] key = new byte[keyLength];
        socketInput.readFully(key);

        int moduloLength = this.socketInput.readInt(); // in bytes
        LOG.debug("moduloLength: " + moduloLength);
        byte[] modulo = new byte[moduloLength];
        socketInput.readFully(modulo);

        if (! ByteArrays.isPositiveBigInteger(key)) {
            LOG.warning("Received non positive RSA key: \n"
                              + ByteArrays.toHexString(key));
        }
        if (! ByteArrays.isPositiveBigInteger(modulo)) {
            LOG.warning("Received non positive modulo: \n"
                              + ByteArrays.toHexString(key));
        }

        rsa.setEncryptKey(key, modulo);
        LOG.debug("RSA key received.");

        return mysteriousN;
    }

    private void stupidEncryptionSendSymmetricKey (byte[] key)
    throws IOException {
        sendMessage(MessageType.sendSymmetricKey, null, key);
        LOG.debug("XOR key sent.");
    }

    private byte[] stupidEncryptionReceiveSymmetricKey (int length)
    throws IOException {
        LOG.debug("ReceiveSymmetricKey: now calling getMessage()");
        Pair<MessageType, Integer> header = getMessage();
        LOG.debug("ReceiveSymmetricKey: getMessage() returned");
        MessageType messageType = header.first();
        int messageLength = header.second();

        if (messageType != MessageType.sendSymmetricKey) {
            LOG.warning("Unexpected MessageType: got " + messageType
                        + " while expecting " + MessageType.sendSymmetricKey);
        }

        int keyLength = messageLength;
        LOG.debug("XOR key length is " + keyLength);
        byte[] key = new byte[keyLength];
        socketInput.readFully(key);

        LOG.debug("XOR key received.");
        return key;
    }

    private Pair <DataInputStream, DataOutputStream> 
    stupidEncryptionEnableRSAStreams (Rsa rsa) {
        Pair<DataInputStream, DataOutputStream> oldStreams;
        oldStreams = new Pair<DataInputStream, DataOutputStream>
                         (this.socketInput, this.socketOutput);

        this.socketInput = new DataInputStream(
                                new RsaInputStream(this.socketInput, rsa));
        this.socketOutput = new DataOutputStream(
                                new RsaOutputStream(this.socketOutput, rsa));

        LOG.debug("Now using Rsa streams.");
        return oldStreams;
    }

    private void stupidEncryptionEnableSymmetricStreams (byte[] key, int len) {
        this.socketInput = new DataInputStream(
                              new XorInputStream(this.socketInput, key, len));
        this.socketOutput = new DataOutputStream(
                              new XorOutputStream(this.socketOutput, key, len));
        LOG.debug("Now using XOR encryption.");
    }

    private void stupidEncryptionDisableRSAStreams 
        (Pair<DataInputStream, DataOutputStream> oldStreams) {
        this.socketInput = oldStreams.first();
        this.socketOutput = oldStreams.second();
        LOG.debug("Rsa streams are disabled");
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
            LOG.debug("## " + arrayToString(this.id) + " invalidated");
        } else {
            LOG.debug("## " + this.ip.toString() + " invalidated");
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
        LOG.debug("No piece: " + arrayToString(this.id));
        return -1;
    }

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = 7 - index % 8;
        return (((this.bitfield[byteIndex] >>> offset) & 1) == 1);
    }

    private synchronized void keepAlive() throws IOException {
        LOG.debug("KeepAlive :" + arrayToString(this.id));
        socketOutput.writeInt(0);
        socketOutput.flush();
    }

    private synchronized void sendMessage(MessageType type, int[] params, byte[] data) throws IOException {
        LOG.debug("Sending " + type.toString()
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
        LOG.debug("Queued!");
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
        LOG.debug("pieceIndex: " + info.pieceIndex()
                        + " offset: " + info.offset() + " ");
        sendMessage(MessageType.request, params, null);
    }

    public void sendBlock(int index, int offset, byte[] block)
    throws IOException {
        int[] params = { index, offset };
        sendMessage(MessageType.piece, params, block);
    }

    public void stop() {
        LOG.debug("Stopping peer " + this.ip + " " + this.port);
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

