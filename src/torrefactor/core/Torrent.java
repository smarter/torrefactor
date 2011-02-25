package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
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

    public Torrent(String fileName) throws UnsupportedOperationException, IOException,
    FileNotFoundException, InvalidBencodeException {
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(fileName));
        Map<String, Bencode> fileMap = Bencode.decodeDict(stream);

        Map<String, Bencode> infoMap = fileMap.get("info").toMap();
        if (infoMap.containsKey("files")) {
            throw new UnsupportedOperationException("Multiple files mode not supported");
        }

        this.pieceLength = infoMap.get("piece length").toInt();
        String pieces = infoMap.get("pieces").toString();
        for (int i = 0; i < pieces.length(); i += 20) {
            this.pieceList.add(pieces.substring(i, i+20));
        }
        this.name = infoMap.get("name").toString();
        this.length = infoMap.get("length").toInt();
        this.left = this.length;

        this.trackerURL = fileMap.get("announce").toString();
    }

    public void createFile(String fileName)
    throws FileNotFoundException, IOException {
        this.file = new RandomAccessFile(fileName, "rw");
        this.file.setLength(this.length);
    }

    public void start() throws ProtocolException, InvalidBencodeException,
                        IOException {
        if (this.peerManager != null) return;

        this.peerManager = new PeerManager(this);
        this.peerManager.run();
    }

    public void stop() {
        if (this.peerManager == null) return;

        this.peerManager.stopDownload();
    }
}
