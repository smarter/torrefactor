package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;

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
        p.handshake();
        return;
    }

    public Peer(InetAddress _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        System.out.println("New peer: " + _ip.toString() + ':' + _port);
        this.id = null;
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        int bits = torrent.pieceList.size() / 8;
        //round to nearest byte
        bits += ((torrent.pieceList.size() % 8) != 0) ? 1 : 0;
        this.bitfield = new byte[bits];
        this.socket = new Socket(this.ip, this.port);
        socketInput = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        socketOutput = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
        handshake();
    }

    public void run() {
        try {
            handshake();
        } catch (IOException e) {
            e.printStackTrace();
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void readMessage() throws IOException {
        int length = socketInput.readInt();
        if (length == 0) {
            //keepalive, do nothing
            return;
        }
        MessageType type = MessageType.values()[socketInput.readChar()];
        readMessage(type, length);
    }

    private void readMessage(MessageType type, int length)
    throws IOException {
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
            int byteIndex = index / 8 - 1;
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

    public void handshake() throws IOException {
        socketOutput.writeByte(19);
        byte header[] = (new String("Bittorent protocol")).getBytes();
        socketOutput.write(header);
        byte reserved[] = { 0, 0, 0, 0, 0, 0, 0, 0};
        socketOutput.write(reserved);
        socketOutput.write(torrent.infoHash);
        //socketOutput.write(torrent.peerManager.peerId);
        socketOutput.write((new String("00000000000000000000")).getBytes());
        socketOutput.flush();
        int inLength = socketInput.readByte() & 0xFF;
        byte[] inHeader = new byte[inLength];
        socketInput.read(inHeader);
        if (inLength != 19) {
            System.out.println("Unsupported protocol header: " + new String(inHeader));
            return;
        }
        for (int i = 0; i < inHeader.length; i++) {
            if (inHeader[i] != header[i]) {
                System.out.println("Unsupported protocol header: " + new String(inHeader));
                return;
            }
        }
        byte[] inReserved = new byte[reserved.length];
        socketInput.read(inReserved);
        System.out.println(new String(inReserved));
        byte[] inInfoHash = new byte[20];
        socketInput.read(inInfoHash);
        for (int i = 0; i < inInfoHash.length; i++) {
            if (inInfoHash[i] != torrent.infoHash[i]) {
                System.out.println("Wrong info_hash");
                return;
            }
        }
        id = new byte[20];
        socketInput.read(this.id);
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
