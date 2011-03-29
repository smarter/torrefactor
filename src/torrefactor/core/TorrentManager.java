package torrefactor.core;

import torrefactor.core.*;
import torrefactor.util.*;

import java.io.*;
import java.util.*;
import java.security.*;

public class TorrentManager {
    private List<Torrent> torrentList;
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
    }

    public Torrent addTorrent(String fileName)
    throws IOException, InvalidBencodeException, NoSuchAlgorithmException {
        for (Torrent torrent: torrentList) {
            if (fileName.equals(torrent.FILE_NAME)) {
                return torrent;
            }
        }
        Torrent torrent = new Torrent(fileName);
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
            FileInputStream fis = new FileInputStream(this.configFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            torrentList = (ArrayList<Torrent>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Couldn't load saved config from " + this.configFile
                                + " reason: " + e.toString());
        }

        return (torrentList != null);
    }

    private void saveConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(this.configFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(torrentList);
        } catch (Exception e) {
            System.err.println("Couldn't save config to " + this.configFile
                               + " reason: " + e.toString());
        }
    }
}
