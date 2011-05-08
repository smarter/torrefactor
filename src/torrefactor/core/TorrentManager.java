package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.util.*;
import java.security.*;

/**
 * This class manages a list of Torrent.
 * When it is instancied, it will attempt to read its state from a config
 * file. It will write to this file when it's stop()-ed.
 * The config file name is System.getProperty("user.home")/.torrefactor/config.bin
 */
public class TorrentManager {
    private static TorrentManager instance;
    private static Logger LOG = new Logger();
    private List<Torrent> torrentList;
    private transient List<Torrent> readOnlyList;
    private File configFile;

    private TorrentManager() {
        File home = new File(System.getProperty("user.home"));
        File configDir = new File(home, ".torrefactor");
        configDir.mkdir();
        configFile = new File(configDir, "config.bin");
        if (restoreConfig()) {
            return;
        }
        this.torrentList = new ArrayList<Torrent>();
        this.readOnlyList = Collections.unmodifiableList(this.torrentList);
    }

    public static synchronized TorrentManager instance() {
        if (instance == null) {
            instance = new TorrentManager();
        }
        return instance;
    }

    public List<Torrent> torrentList() {
        return this.readOnlyList;
    }

    public synchronized Torrent addTorrent(String fileName, String basePath)
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

    /**
     * Stop all currently running torrents.
     * Write config to the config file specified
     * in the constructor.
     */
    public synchronized void stop() {
        for (Torrent torrent: torrentList) {
            torrent.stop();
        }
        saveConfig();
    }

    // FIXME: Serialization can easily break with code changes
    // Especially since we don't define serialVersionUID
    @SuppressWarnings("unchecked") // config.bin should contains an ArrayList<Torrent>
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
