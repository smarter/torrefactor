package torrefactor.core;
import torrefactor.core.*;

import java.io.*;
import java.util.*;

public class Torrent {
    private int pieceLength;
    private List<String> pieceList = new ArrayList<String>();
    private String name;
    private int length;
    String infoHash;
    RandomAccessFile file;
    PeerManager peerManager;
    int uploaded;
    int downloaded;
    int left;
    String trackerURL;

    Torrent(String fileName) throws UnsupportedOperationException {
        BufferedReader stream = new BufferedReader(new FileReader(fileName));
        Map<String, Bencode> fileMap = Bencode.decode(stream);

        Map<String, Bencode> infoMap = fileMap.get("info").toMap();
        if (infoMap.containsKey("files")) {
            throw new UnsupportedOperationException("Multiple files mode not supported");
        }

        this.pieceLength = infoMap.get("piece length").toInt();
        String pieces = infoMap.get("pieces").toString();
        for (int i = 0; i < pieceLength; i += 20) {
            this.pieceList.add(pieces.substring(i, i+20));
        }
        this.name = infoMap.get("name").toString();
        this.length = infoMap.get("length").toInt();
        this.left = this.length;

        this.trackerURL = fileMap.get("announce").toString();
    }

    void createFile(String fileName) {
        this.file = new RandomAccessFile(fileName, "rw");
        this.file.setLength(this.length);
    }

    void start() {
        if (this.peerManager != null) return;

        this.peerManager = new PeerManager(this);
        this.peerManager.run();
    }

    void stop() {
        if (this.peerManager == null) return;

        this.peerManager.stopDownload();
    }
}
