package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;

public class Peer {
    public enum MessageType {
        choke, unchoke, interested, not_interested, have, bitfield,
        request, piece, cancel
    }
    private String ip;
    private int port;
    private Torrent torrent;
    private Socket socket;
    private DataInputStream socketInput;
    private PrintWriter socketOutput;

    private String id;
    private byte[] bitfield;
    boolean isChoked = true;
    boolean isChokingUs = true;
    boolean isInteresting = false;
    boolean isInterestedInUs = false;

    public Peer(String _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        int bits = torrent.pieceList.size() / 8;
        //round to nearest byte
        bits += ((torrent.pieceList.size() % 8) != 0) ? 1 : 0;
        this.bitfield = new byte[bits];
        this.socket = new Socket(this.ip, this.port);
        socketInput = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        socketOutput = new PrintWriter(this.socket.getOutputStream(), false);
    }

    public void run() throws IOException {
        keepAlive();
        while (socketInput.available() != 0) {
            readMessage();
        }
    }

    private void readMessage() throws IOException {
        int length = socketInput.readInt();
        if (length == 0) {
            //TODO: handle peer keepalive
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
        case cancel:
        default: {
            break;
        }
        }
    }

    public void handshake() throws IOException {
        String header = "19Bittorent protocol";
        String reserved = "\0\0\0\0\0\0\0\0";
        socketOutput.print(header);
        socketOutput.print(reserved);
        socketOutput.print(torrent.infoHash);
        socketOutput.print(torrent.peerManager.peerId);
        byte[] inHeader = new byte[header.length()];
        socketInput.read(inHeader);
        if (!header.equals(new String(inHeader))) {
            return;
        }
        byte[] inReserved = new byte[reserved.length()];
        socketInput.read(inReserved);
        System.out.println(new String(inReserved));
        byte[] inInfoHash = new byte[20];
        socketInput.read(inInfoHash);
        if (!torrent.infoHash.equals(new String(inInfoHash))) {
            return;
        }
        byte[] inId = new byte[20];
        socketInput.read(inId);
        this.id = new String(inId);
    }

    public boolean isDownloading() {
        return false;
    }

    public boolean wasDisconnected() {
        return false;
    }

    private void keepAlive() {
        socketOutput.print("0");
        socketOutput.flush();
    }

    private void sendMessage(MessageType type, String payload) {
        int length = 1;
        if (payload != null) {
            length += payload.length();
        }
        socketOutput.print(length);
        socketOutput.print(type.ordinal());
        if (payload != null && !payload.isEmpty()) {
            socketOutput.print(payload);
        }
        socketOutput.flush();
    }

    public void setChoked(boolean b) {
        MessageType type = b ? MessageType.choke : MessageType.unchoke;
        sendMessage(type, null);
    }

    public void setInteresting(boolean b) {
        MessageType type = b ? MessageType.interested : MessageType.not_interested;
        sendMessage(type, null);
    }
}
