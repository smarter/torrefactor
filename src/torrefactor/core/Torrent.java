package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;

/**
 * A Torrent object represents a torrent currently being downloaded.
 */
public class Torrent implements Serializable {
    private static Logger LOG = new Logger();

    public final String FILE_NAME;

    private File basePath;
    private int pieceLength;
    private ArrayList<Pair<File, Long>> files;
    private byte[] pieceHash;
    private AtomicLong uploaded = new AtomicLong(0);
    private AtomicLong downloaded = new AtomicLong(0);
    private SpeedMeter uploadedSpeed = new SpeedMeter(0);
    private SpeedMeter downloadedSpeed = new SpeedMeter(0);

    public byte[] infoHash;
    transient PeerManager peerManager;
    public PieceManager pieceManager;
    long length = 0;
    List<List<String>> announceList;
    long creationDate = 0;
    String comment = "";
    String createdBy = "";
    String encoding = "";

    public Torrent(String fileName, String basePath)
    throws UnsupportedOperationException, IOException, FileNotFoundException,
    InvalidBDecodeException, NoSuchAlgorithmException {

    if (basePath == null || basePath.isEmpty()) {
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
        Map<String, BValue> fileMap = BDecode.decodeDict(stream, "info", infoHash);

        Map<String, BValue> infoMap = fileMap.get("info").toMap();
        if (infoMap.containsKey("files")) {
            List maps = infoMap.get("files").toList();
            this.files = new ArrayList<Pair<File, Long>>(maps.size());
            for (int i=0; i<maps.size(); i++) {
                Map map = ((BValue) maps.get(i)).toMap();
                File file = bencodeToFile((BValue) map.get("path"));
                file = new File(basePath, file.toString());
                long size = ((BValue) map.get("length")).toLong();
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

        this.pieceManager = new PieceManager(this.files, this.pieceLength,
                                             this.pieceHash);

        announceList = new ArrayList<List<String>>();
        if (fileMap.containsKey("announce-list")) {
            List<BValue> announces = fileMap.get("announce-list").toList();
            LOG.debug(this, "LENGTH: " + announces.size());
            for (BValue tierList : announces) {
                List<BValue> trackers = tierList.toList();
                Collections.shuffle(trackers);

                LinkedList<String> trackerList = new LinkedList<String>();
                for (int i = 0; i < trackers.size(); i++) {
                    trackerList.add(trackers.get(i).toString());
                }
                announceList.add(trackerList);
            }
        } else {
            LOG.debug(this, "Single tracker mode");
            LinkedList<String> trackerList = new LinkedList<String>();
            trackerList.add(fileMap.get("announce").toString());
            announceList.add(trackerList);
        }
        LOG.debug(this, "announceList: " + announceList);

        if (fileMap.containsKey("comment")) {
            this.comment = fileMap.get("comment").toString();
        }
        if (fileMap.containsKey("creation date")) {
            this.creationDate = fileMap.get("creation date").toLong();
            LOG.debug(this, "Creation date is: " + this.creationDate);
        }
        if (fileMap.containsKey("created by")) {
            this.createdBy = fileMap.get("created by").toString();
        }
        if (fileMap.containsKey("encoding")) {
            this.encoding = fileMap.get("encoding").toString();
        }
    }

    private static File bencodeToFile(BValue b) {
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

    public ArrayList<Pair<File, Long>> getFiles() {
        // FIXME this propably should return a copy.
        return this.files;
    }

    public long getSize () {
        return this.length;
    }

    public boolean isComplete() {
        return this.pieceManager.isComplete();
    }

    public double progress() {
        return ByteArrays.percentage(this.pieceManager.bitfield);
    }

    /**
     * Returns how much byte have been downloaded and verified.
     */
    public long done() {
        long done = ByteArrays.done(this.pieceManager.bitfield) * pieceLength;
        if (done > this.length) done = this.length;
        return done;
    }

    public long left() {
        return this.length - done();
    }

    public long downloaded() {
        return this.downloaded.longValue();
    }

    public double downloadSpeed () {
        return this.downloadedSpeed.getSpeed(this.downloaded.longValue());
    }

    public long incrementDownloaded(long value) {
        return this.downloaded.addAndGet(value);
    }

    public long uploaded() {
        return this.uploaded.longValue();
    }

    public double uploadSpeed () {
        return this.uploadedSpeed.getSpeed(this.uploaded.longValue());
    }

    public long incrementUploaded(long value) {
        return this.uploaded.addAndGet(value);
    }

    public String getAuthor () {
        return this.createdBy;
    }

    public long getCreationDate () {
        return creationDate;
    }

    public String getBasePath () {
        return basePath.getAbsolutePath();       
    }

    public int getNumPiece () {
        return (int) ((this.length-1) / this.pieceLength) + 1;
    }

    public int getPieceSize () {
        return this.pieceLength;
    }

    public void start() throws ProtocolException, InvalidBDecodeException,
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

    public Map<InetAddress, Peer> getPeerMap () {
        if (this.peerManager == null) return null;
        return this.peerManager.getPeerMap();
    }

    public BoundedRangeModel getBoundedRangeModel () {
        if (this.length <= Integer.MAX_VALUE) {
            return new DefaultBoundedRangeModel(
                    (int)(this.done()),
                    0, 0, (int)this.length);
        } else {
            int value = (int) (progress() * Integer.MAX_VALUE);
            return new DefaultBoundedRangeModel(
                    value, 0, 0, Integer.MAX_VALUE);
        }
    }

    public String getState () {
        if (this.peerManager != null) {
            PeerManager.State state = this.peerManager.state();
            if (state != null) {
                return state.toString();
            }
        }
        return "Stopped";
    }
}
