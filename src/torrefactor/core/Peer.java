package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class Peer implements Runnable {
    public enum MessageType {
        choke, unchoke, interested, not_interested, have, bitfield,
        request, piece, cancel, port
    }

    private Queue<DataBlockInfo> outQueue;

    private volatile boolean isValid = true;
    private boolean isConnected = false;

    byte[] id;
    private InetAddress ip;
    private int port;
    private Torrent torrent;
    private Socket socket;
    private DataInputStream socketInput;
    private DataOutputStream socketOutput;
    private int downloaded = 0;
    private int uploaded = 0;

    private byte[] bitfield;
    private boolean isChoked = true;
    private boolean isChokingUs = true;
    private boolean isInteresting = false;
    private boolean isInterestedInUs = false;

    static final int CONNECTION_TRIES = 5;

    // In milliseconds
    static final int PEER_TIMEOUT =  2*60*1000;
    static final int SLEEP_DELAY = 1000;

    public static void main(String[] args) throws Exception {
        Torrent t = new Torrent("deb.torrent");
        t.createFile("bla");
        t.start();
        Peer p = new Peer(InetAddress.getByName("localhost"), 3000, t);
        new Thread(p).start();
        return;
    }

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
        while (this.socket == null) {
            try {
                System.out.println("Connecting: " + this.ip.toString() + ':' + this.port);
                this.socket = new Socket(this.ip, this.port);
                this.socket.setSoTimeout(PEER_TIMEOUT);
                socketInput = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                System.out.println("Connected: " + this.ip.toString() + ':' + this.port);
                socketOutput = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                if (!handshake()) {
                    invalidate();
                    return;
                }
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
                    e.printStackTrace();
                    invalidate();
                    return;
                }
            }
        }
        this.isConnected = true;
        long time = System.currentTimeMillis();
        while (this.isValid) {
            //System.out.println("Loop: " + arrayToString(this.id) + " " + this.isValid + " " + !this.isChokingUs);
            try {
                readMessage();
                if (System.currentTimeMillis() - time > PEER_TIMEOUT / 2) {
                    System.out.println("#################");
                    DataBlockInfo queuedInfo = outQueue.poll();
                    if (queuedInfo != null) {
                        sendRequest(queuedInfo);
                    } else {
                        keepAlive();
                    }
                    time = System.currentTimeMillis();
                }
                if (socketInput.available() != 0) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
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
    }

    private void readMessage() throws IOException {
        int length = socketInput.readInt();
        if (length == 0) {
            //System.out.println("Got keep alive: " + arrayToString(this.id));
            //keepalive, do nothing
            return;
        }
        int typeByte = socketInput.read();
        length--;
        if (typeByte < 0 || typeByte > 9) {
            System.out.println("Got unknown message " + typeByte + " with length: " + length + " " + arrayToString(this.id));
            //HACK: try to find keep alives to get back to known messages
            boolean foundKeepAlive = false;
            while (length != 0 && socketInput.available() != 0) {
                if (socketInput.read() == 0 && socketInput.read() == 0 && socketInput.read() == 0) {
                    foundKeepAlive = true;
                    System.out.println("Found possible Keep Alive, trying to recover");
                    break;
                }
                length -= 3;
            }
            if (!foundKeepAlive) {
                invalidate();
            }
            return;
        }
        MessageType type = MessageType.values()[typeByte];
        readMessage(type, length);
    }

    private void readMessage(MessageType type, int length)
    throws IOException {
        System.out.println("Got message " + type.toString() + " " + arrayToString(this.id) + " " + length);
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
                System.out.println("Wrong bitfield length, got: " + length + " expected: " + this.bitfield.length);
            }
            socketInput.read(this.bitfield);
            break;
        }
        case request: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            int blockLength = socketInput.readInt();
            System.out.println("Request, index: " + index + " offset: " + offset + " blockLength : " + blockLength);
            byte[] block = this.torrent.pieceManager.getBlock(index, offset, blockLength);
            if (block == null) return;
            sendBlock(index, offset, block);
            break;
        }
        case piece: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            byte[] block = new byte[length - 2*4];
            socketInput.read(block);
            this.torrent.pieceManager.putBlock(index, offset, block);
            DataBlockInfo queuedInfo = outQueue.poll();
            if (queuedInfo != null) {
                System.out.println("AAAAAAAAAAAAAAAAAA");
                try {Thread.currentThread().sleep(100);} catch(InterruptedException e) { e.printStackTrace(); invalidate(); return; }
                System.out.println("BBBBBBBBBBBBBBBBBB");
                sendRequest(queuedInfo);
            }
            break;
        }
        //TODO
        case cancel: {
            break;
        }
        //DHT node port, unsupported for the moment
        case port: {
            int lowByte = socketInput.read();
            int highByte = socketInput.read();
            int port = (highByte << 8) | lowByte;
            break;
        }
        default:
            break;
        }
    }

    private boolean handshake() throws IOException {
        System.out.println("Handshake start: " + this.ip.toString() + ':' + this.port);
        socketOutput.writeByte(19);
        byte header[] = (new String("BitTorrent protocol")).getBytes();
        socketOutput.write(header);
        socketOutput.flush();
        byte reserved[] = { 0, 0, 0, 0, 0, 0, 0, 0};
        socketOutput.write(reserved);
        socketOutput.write(torrent.infoHash);
        socketOutput.write(torrent.peerManager.peerId);
        socketOutput.flush();
        int inLength = socketInput.read();
        if (inLength == -1) {
            return false;
        }
        byte[] inHeader = new byte[inLength];
        socketInput.read(inHeader);
        if (!Arrays.equals(header, inHeader)) {
            System.out.println("Unsupported protocol header: " + new String(inHeader));
            return false;
        }
        byte[] inReserved = new byte[reserved.length];
        socketInput.read(inReserved);
        byte[] inInfoHash = new byte[20];
        socketInput.read(inInfoHash);
        if (!Arrays.equals(torrent.infoHash, inInfoHash)) {
            System.out.println("Wrong info_hash");
            return false;
        }
        id = new byte[20];
        socketInput.read(this.id);
        System.out.println("Handshake done: " + this.ip.toString() + ':' + this.port + " id: " + arrayToString(this.id));
        sendMessage(MessageType.bitfield, null, this.torrent.pieceManager.bitfield);
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

    public int popDownloaded() {
        int poped = this.downloaded;
        this.downloaded = 0;
        return poped;
    }

    public int popUploaded() {
        int poped = this.uploaded;
        this.uploaded = 0;
        return poped;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void invalidate() {
        if (this.id != null) {
            System.out.println("## " + arrayToString(this.id) + " invalidated");
        } else {
            System.out.println("## " + this.ip.toString() + " invalidated");
        }
        this.isValid = false;
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
        System.out.println("No piece: " + arrayToString(this.id));
        return -1;
    }

    public boolean hasPiece(int index) {
        int byteIndex = index / 8;
        int offset = 7 - index % 8;
        return (((this.bitfield[byteIndex] >>> offset) & 1) == 1);
    }

    private synchronized void keepAlive() throws IOException {
        System.out.println("KeepAlive :" + arrayToString(this.id));
        socketOutput.writeInt(0);
        socketOutput.flush();
    }

    private synchronized void sendMessage(MessageType type, int[] params, byte[] data) throws IOException {
        System.out.println("Sending " + type.toString() + " to :" + arrayToString(this.id));
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
    }

    public void queueRequest(DataBlockInfo info) throws IOException {
        System.out.println("Queued!");
        outQueue.add(info);
        if (outQueue.size() >= 10) {
            sendRequest(outQueue.poll());
        }
    }

    //TODO: make asynchronous
    public void sendRequest(DataBlockInfo info)
    throws IOException {
        if (!isConnected()) return;
        int[] params = { info.pieceIndex(), info.offset(), info.length() };
        System.out.print("pieceIndex: " + info.pieceIndex() + " offset: " + info.offset() + " ");
        sendMessage(MessageType.request, params, null);
    }

    public void sendBlock(int index, int offset, byte[] block)
    throws IOException {
        int[] params = { index, offset };
        sendMessage(MessageType.piece, params, block);
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
