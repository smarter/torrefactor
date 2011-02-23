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
    private BufferedReader socketReader;
    private PrintWriter socketWriter;

    boolean isChoked = true;
    boolean isChokingUs = true;
    boolean isInteresting = false;
    boolean isInterestedInUs = false;

    public Peer(String _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        this.socket = new Socket(this.ip, this.port);
        socketReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        socketWriter = new PrintWriter(this.socket.getOutputStream(), false);
    }

    public void run() throws IOException {
        keepAlive();
        //TODO: find out how ready() behaves
        while (socketReader.ready()) {
            readMessage();
        }
    }

    private void readMessage() throws IOException {
        char[] lengthArray = new char[4];
        socketReader.read(lengthArray, 0, 4);
        int length = arrayToInt(lengthArray);
        if (length == 0) {
            keepAlive();
            return;
        }
        char[] messageArray = new char[length];
        socketReader.read(messageArray, 0, length);
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
        socketWriter.print("19BitTorrent protocol");
        socketWriter.print("\0\0\0\0\0\0\0\0");
        socketWriter.print(torrent.infoHash);
        socketWriter.print(torrent.peerManager.peerId);
    }

    public boolean isDownloading() {
        return false;
    }

    public boolean wasDisconnected() {
        return false;
    }

    private void keepAlive() {
        socketWriter.print("0");
        socketWriter.flush();
    }

    private void sendMessage(MessageType type, String payload) {
        int length = 1;
        if (payload != null) {
            length += payload.length();
        }
        socketWriter.print(length);
        socketWriter.print(type.ordinal());
        if (payload != null && !payload.isEmpty()) {
            socketWriter.print(payload);
        }
        socketWriter.flush();
    }

    private int arrayToInt(char[] intArray) {
        return (intArray[0] << 24) | (intArray[1] << 16) | (intArray[2] << 8) | intArray[3];
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
