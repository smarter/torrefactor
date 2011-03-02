package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Torrent {
    int pieceLength;
    List<byte[]> pieceList = new ArrayList<byte[]>();
    private String name;
    private int length;
    byte[] infoHash;
    RandomAccessFile file;
    PeerManager peerManager;
    int uploaded = 0;
    int downloaded = 0;
    int left;
    String trackerURL;
    int creationDate = 0;
    String comment = "";
    String createdBy = "";
    String encoding = "";

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
        byte[] piecesArray = infoMap.get("pieces").toByteArray();
        int i = 0;
        while (i < piecesArray.length) {
            byte[] piece = new byte[20];
            for (int j = 0; j < piece.length; j++, i++) {
                piece[j] = piecesArray[i];
            }
            this.pieceList.add(piece);
        }
        this.name = new String(infoMap.get("name").toByteArray());
        this.length = infoMap.get("length").toInt();
        this.left = this.length;

        this.trackerURL = new String(fileMap.get("announce").toByteArray());
        if (fileMap.containsKey("comment")) {
            this.comment = fileMap.get("comment").toString();
        }
        if (fileMap.containsKey("creation date")) {
            //TODO: there should be a way to parse bencoded int as double
            //      in util.Bencode since the date is coded on a long.
            //this.creationDate = fileMap.get("creation date").toDouble();
            System.err.println("Ignoring 'creation date' in torrent file.");
        }
        if (fileMap.containsKey("created by")) {
            this.createdBy = fileMap.get("created by").toString();
        }
        if (fileMap.containsKey("encoding")) {
            this.encoding = fileMap.get("encoding").toString();
        }
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
        this.peerManager.start();
    }

    public void stop() {
        if (this.peerManager == null) return;

        this.peerManager.stopDownload();
    }
}
