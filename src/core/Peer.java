package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.net.*;

public class Peer {
    private String ip;
    private int port;
    private Torrent torrent;
    private Socket socket;
    private BufferedReader socketReader;
    private FileWriter socketWriter;

    boolean isChoked = true;
    boolean isChokingUs = true;
    boolean isInteresting = false;
    boolean isInterestedInUs = false;

    public Peer(String _ip, int _port, Torrent _torrent) throws UnknownHostException, IOException {
        this.ip = _ip;
        this.port = _port;
        this.torrent = _torrent;
        this.socket = new Socket(this.ip, this.port);
    }

    public void run() throws IOException {
        String input;
        char[] length = new char[4];
        socketReader.read(length, 0, 4);
        while ((input = socketReader.readLine()) != null) {
            
        }
    }

    public boolean isDownloading() {
        return false;
    }

    private long decodeInt(char[] intArray) {
        return (intArray[0] << 24) | (intArray[1] << 16) | (intArray[2] << 8) | intArray[3] & 0xFFFFFFFFL;
    }
}
