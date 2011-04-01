package torrefactor.core;
import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Torrent implements Serializable {
    public final String FILE_NAME;
    private File basePath;
    private int pieceLength;
    private ArrayList<Pair<File, Long>> files;
    private byte[] pieceHash;
    byte[] infoHash;
    transient PeerManager peerManager;
    PieceManager pieceManager;
    long length = 0;
    long uploaded = 0;
    long downloaded = 0;
    long left;
    List<List<String>> announceList;
    int creationDate = 0;
    String comment = "";
    String createdBy = "";
    String encoding = "";

    public Torrent(String fileName, String basePath)
    throws UnsupportedOperationException, IOException, FileNotFoundException,
    InvalidBencodeException, NoSuchAlgorithmException {

    if (basePath == "" || basePath == null) {
        this.basePath = new File(".");
    } else {
        this.basePath = new File(basePath);
    }
    if (!this.basePath.isDirectory()) {
        throw new IllegalArgumentException(
                "\"" + basePath + "\" does not exist or is not a directory.");
    }

        DigestInputStream stream = new DigestInputStream(
                    new BufferedInputStream(new FileInputStream(fileName)),
                    MessageDigest.getInstance("SHA1"));

        this.FILE_NAME = fileName;

        infoHash = new byte[20];
        Map<String, Bencode> fileMap = Bencode.decodeDict(stream, "info", infoHash);

        Map<String, Bencode> infoMap = fileMap.get("info").toMap();
        if (infoMap.containsKey("files")) {
            System.err.println(infoMap.get("files"));
            List maps = infoMap.get("files").toList();
            this.files = new ArrayList<Pair<File, Long>>(maps.size());
            for (int i=0; i<maps.size(); i++) {
                Map map = ((Bencode) maps.get(i)).toMap();
                File file = bencodeToFile((Bencode) map.get("path"));
                file = new File(basePath, file.toString());
                long size = ((Bencode) map.get("length")).toLong();
                this.length += size;
                Pair<File, Long> fpair = new Pair<File, Long>(file, size);
                this.files.add(fpair);
            }
        } else {
            // Single file mode
            String path = new String(infoMap.get("name").toByteArray());
            File file = new File(basePath, path);
            long size = infoMap.get("length").toLong();
            this.length = size;
            Pair<File, Long> fpair = new Pair<File, Long>(file, size);
            this.files = new ArrayList<Pair<File, Long>>(1);
            this.files.add(fpair);
        }

        this.pieceLength = infoMap.get("piece length").toInt();
        this.pieceHash = infoMap.get("pieces").toByteArray();
        int pieces = (int)((this.length - 1)/((long) this.pieceLength + 1));

        this.left = this.length;

        this.pieceManager = new PieceManager(this.files, this.pieceLength,
                                             this.pieceHash);

        announceList = new ArrayList<List<String>>();
        if (fileMap.containsKey("announce-list")) {
            List<Bencode> announces = fileMap.get("announce-list").toList();
            System.out.println("LENGTH: " + announces.size());
            for (Bencode tierList : announces) {
                List<Bencode> trackers = tierList.toList();
                Collections.shuffle(trackers);

                LinkedList<String> trackerList = new LinkedList<String>();
                for (int i = 0; i < trackers.size(); i++) {
                    trackerList.add(new String(trackers.get(i).toByteArray()));
                }
                announceList.add(trackerList);
            }
        } else {
            System.err.println("Single tracker mode");
            LinkedList<String> trackerList = new LinkedList<String>();
            trackerList.add(new String(fileMap.get("announce").toByteArray()));
            announceList.add(trackerList);
        }
        System.out.println("announceList: " + announceList);

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

    private static File[] bencodeListToFiles(Bencode b) {
        List<Bencode> list = b.toList();
        File[] files = new File[list.size()];
        for (int i=0; i<list.size(); i++) {
            File file = bencodeToFile(list.get(i));
            files[i] = file;
        }
        return files;
    }

    private static File bencodeToFile(Bencode b) {
        List l = b.toList();
        File parent = new File(l.get(0).toString());
        File current = parent;
        int i=1;
        while (i<l.size()) {
            current = new File(parent, l.get(i).toString());
            i++;
        }
        assert (current != null);
        return current;
    }

    private static void mkparentdirs(File[] files) {
        for (int i=0; i<files.length; i++) {
            String parentPath = files[i].getParent();
            if (parentPath == null) {
                //No parent
                continue;
            }
            File parent = new File(parentPath);
            parent.mkdirs();
        }
    }

    public ArrayList<Pair<File, Long>> getFiles() {
        // FIXME this propably should return a copy.
        return this.files;
    }

    public float progress() {
        if (this.length == 0) return 0;
        // use length-left instead of downloaded since downloaded may be
        // greater than length if we downloaded some invalid pieces.
        return (float) (this.length - this.left) / (float) this.length;
    }

    public long downloaded() {
        return this.downloaded;
    }

    public long uploaded() {
        return this.uploaded;
    }

    public void start() throws ProtocolException, InvalidBencodeException,
                        IOException {
        if (this.peerManager == null) {
            this.peerManager = new PeerManager(this);
        } else if (!this.peerManager.isStopped()) {
            return;
        }
        new Thread(this.peerManager).start();
    }

    public void stop() {
        if (this.peerManager == null) return;

        this.peerManager.stop();
    }
}
