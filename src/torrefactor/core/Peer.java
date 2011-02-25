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
    private BufferedInputStream socketInput;
    private PrintWriter socketOutput;

    boolean isChoked = true;
    boolean isChokingUs = true;
    boolean isInteresting = false;
    boolean isInterestedInUs = false;

    public Peer(String _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        this.socket = new Socket(this.ip, this.port);
        socketInput = new BufferedInputStream(this.socket.getInputStream());
        socketOutput = new PrintWriter(this.socket.getOutputStream(), false);
    }

    public void run() throws IOException {
        keepAlive();
        while (socketInput.available() != 0) {
            readMessage();
        }
    }

    private void readMessage() throws IOException {
        byte[] lengthArray = new byte[4];
        socketInput.read(lengthArray, 0, 4);
        int length = arrayToInt(lengthArray);
        if (length == 0) {
            //TODO: handle peer keepalive
            return;
        }
        byte[] messageArray = new byte[length];
        socketInput.read(messageArray, 0, length);
        String message = new String(messageArray);
        MessageType type = MessageType.values()[charToInt(message.charAt(0))];
        readMessage(type, message.substring(1));
    }

    private void readMessage(MessageType type, String payload) {
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
        //Not implemented
        case have:
        case bitfield:
        case request:
        case piece:
        case cancel:
        default: {
            break;
        }
        }
    }

    public void handshake() {
        socketOutput.print("19BitTorrent protocol");
        socketOutput.print("\0\0\0\0\0\0\0\0");
        socketOutput.print(torrent.infoHash);
        socketOutput.print(torrent.peerManager.peerId);
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

    private int arrayToInt(byte[] intArray) {
        return (intArray[0] & 0xFF << 24) | (intArray[1] & 0xFF << 16) | (intArray[2] & 0xFF << 8) | intArray[3] & 0xFF;
    }

    private int charToInt(char digit) {
        return digit - '0';
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
