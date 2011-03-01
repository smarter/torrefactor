package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Torrent {
    int pieceLength;
    List<String> pieceList = new ArrayList<String>();
    private String name;
    private int length;
    byte[] infoHash;
    RandomAccessFile file;
    PeerManager peerManager;
    int uploaded = 0;
    int downloaded = 0;
    int left;
    String trackerURL;

    public Torrent(String fileName) throws UnsupportedOperationException, IOException,
    FileNotFoundException, InvalidBencodeException, NoSuchAlgorithmException {
        DigestInputStream stream = new DigestInputStream(new BufferedInputStream(new FileInputStream(fileName)), MessageDigest.getInstance("SHA1"));

        infoHash = new byte[20];
        Map<String, Bencode> fileMap = Bencode.decodeDict(stream, "info", infoHash);

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


    public String name() {
        return this.name;
    }

    public int length() {
        return this.length;
    }

    public int progress() {
        if (this.length == 0) return 0;
        return this.downloaded / this.length * 100;
    }

    public int downloaded() {
        return this.downloaded;
    }

    public int uploaded() {
        return this.uploaded;
    }


    public void writePiece(int index, int offset, String data)
    throws IOException {
        this.file.seek(this.pieceLength * index + offset);
        this.file.writeBytes(data);
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
