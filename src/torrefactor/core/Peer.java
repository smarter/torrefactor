package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Peer extends Thread {
    public enum MessageType {
        choke, unchoke, interested, not_interested, have, bitfield,
        request, piece, cancel
    }
    private byte[] id;
    private InetAddress ip;
    private int port;
    private Torrent torrent;
    private Socket socket;
    private DataInputStream socketInput;
    private DataOutputStream socketOutput;
    private int downloaded = 0;
    private int uploaded = 0;

    private byte[] bitfield;
    boolean isChoked = true;
    boolean isChokingUs = true;
    boolean isInteresting = false;
    boolean isInterestedInUs = false;

    final static int delay = 100; // milliseconds
    final static int maxTries = 20;

    public static void main(String[] args) throws Exception {
        Torrent t = new Torrent("deb.torrent");
        t.createFile("bla");
        t.start();
        Peer p = new Peer(InetAddress.getByName("localhost"), 3000, t);
        p.start();
        return;
    }

    public Peer(InetAddress _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.id = null;
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        int bits = torrent.pieceList.size() / 8;
        //round to nearest byte
        bits += ((torrent.pieceList.size() % 8) != 0) ? 1 : 0;
        this.bitfield = new byte[bits];
    }

    public void run() {
        if (this.socket == null) {
            try {
                System.out.println("Connecting: " + this.ip.toString() + ':' + this.port);
                this.socket = new Socket(this.ip, this.port);
                socketInput = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                System.out.println("Connected: " + this.ip.toString() + ':' + this.port);
                socketOutput = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                if (!handshake()) return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        int tries = maxTries;
        while (true) {
            try {
                keepAlive();
                if (socketInput.available() != 0) {
                    tries = maxTries;
                    readMessage();
                } else if (tries > 0) {
                    tries--;
                    sleep(delay);
                } else {
                    tries = maxTries;
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void readMessage() throws IOException {
        int length = socketInput.readInt();
        if (length == 0) {
            //keepalive, do nothing
            return;
        }
        int typeByte = socketInput.read();
        if (typeByte < 0 || typeByte > 9) {
            System.out.println("Unknown message type " + typeByte + ' ' + this.ip.toString() + ':' + this.port);
        }
        MessageType type = MessageType.values()[typeByte];
        readMessage(type, length);
    }

    private void readMessage(MessageType type, int length)
    throws IOException {
        System.out.println("Message " + type.toString() + ' ' + this.ip.toString() + ':' + this.port);
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
            int offsetMask = 1 << (index % 8);
            this.bitfield[byteIndex] |= offsetMask;
            break;
        }
        case bitfield: {
            socketInput.read(this.bitfield);
            break;
        }
        //TODO
        case request:
        case piece: {
            int index = socketInput.readInt();
            int offset = socketInput.readInt();
            byte[] dataArray = new byte[length - 2*4];
            socketInput.read(dataArray);
            String data = new String(dataArray);
            this.torrent.writePiece(index, offset, data);
            break;
        }
        //TODO
        case cancel:
        default:
            break;
        }
    }

    public boolean handshake() throws IOException {
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
        System.out.println("Handshake done: " + this.ip.toString() + ':' + this.port);
        return true;
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

    public boolean isDownloading() {
        return false;
    }

    public boolean wasDisconnected() {
        return false;
    }

    private void keepAlive() throws IOException {
        socketOutput.writeInt(0);
        socketOutput.flush();
    }

    private void sendMessage(MessageType type, byte[] payload) throws IOException {
        int length = 1;
        if (payload != null) {
            length += payload.length;
        }
        socketOutput.writeInt(length);
        socketOutput.writeInt(type.ordinal());
        if (payload != null) {
            socketOutput.write(payload);
        }
        socketOutput.flush();
    }

    public void setChoked(boolean b) throws IOException {
        MessageType type = b ? MessageType.choke : MessageType.unchoke;
        sendMessage(type, null);
    }

    public void setInteresting(boolean b) throws IOException {
        MessageType type = b ? MessageType.interested : MessageType.not_interested;
        sendMessage(type, null);
    }
}
