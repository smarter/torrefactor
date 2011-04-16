package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.util.*;
import java.security.*;

public class TorrentManager {
    private static Logger LOG = new Logger();
    private List<Torrent> torrentList;
    private transient List<Torrent> readOnlyList;
    private File configFile;

    public TorrentManager(String configFileName) {
        if (configFileName.isEmpty()) {
            File home = new File(System.getProperty("user.home"));
            File configDir = new File(home, ".torrefactor");
            configDir.mkdir();
            configFile = new File(configDir, "config.bin");
        } else {
            configFile = new File(configFileName);
        }
        if (restoreConfig()) {
            return;
        }
        this.torrentList = new ArrayList<Torrent>();
        this.readOnlyList = Collections.unmodifiableList(this.torrentList);
    }

    public List<Torrent> torrentList() {
        return this.readOnlyList;
    }

    public Torrent addTorrent(String fileName, String basePath)
    throws IOException, InvalidBDecodeException, NoSuchAlgorithmException {
        for (Torrent torrent: torrentList) {
            if (fileName.equals(torrent.FILE_NAME)) {
                return torrent;
            }
        }
        Torrent torrent = new Torrent(fileName, basePath);
        boolean ok = this.torrentList.add(torrent);
        return (ok ? torrent : null);
    }

    public void stop() {
        for (Torrent torrent: torrentList) {
            torrent.stop();
        }
        saveConfig();
    }

    //FIXME: Serialization can easily break with code changes
    //Especially since we don't define serialVersionUID
    // config.bin should contains an ArrayList<Torrent>
    @SuppressWarnings("unchecked")
    private boolean restoreConfig() {
        if (!configFile.exists()) {
            return false;
        }
        try {
            LOG.info(this, "Restoring config from " + this.configFile);
            FileInputStream fis = new FileInputStream(this.configFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.torrentList = (ArrayList<Torrent>) ois.readObject();
            this.readOnlyList = Collections.unmodifiableList(this.torrentList);
        } catch (Exception e) {
            LOG.error(this,
                      "Couldn't load saved config from " + this.configFile
                      + " reason: " + e.toString());
        }

        return (this.torrentList != null);
    }

    private void saveConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(this.configFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.torrentList);
        } catch (Exception e) {
            LOG.error(this,
                      "Couldn't save config to " + this.configFile
                      + " reason: " + e.toString());
        }
    }
}
