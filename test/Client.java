package test;

import torrefactor.core.TorrentManager;
import torrefactor.core.Torrent;
import torrefactor.util.Config;

public class Client {
    public static TorrentManager torrentManager;

    public static void main(String[] args)
    throws java.io.IOException, java.io.FileNotFoundException,
    java.net.ProtocolException, java.security.NoSuchAlgorithmException,
    torrefactor.util.InvalidBDecodeException {
        if (args.length < 2) {
            System.out.println(
                    "Usage: java test.Client <input.torrent> <output directory>");
            System.exit(1);
        }
        new Config();
        Torrent torrent = TorrentManager.instance().addTorrent(args[0], args[1]);
        torrent.start();
        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                public void run() {
                    destructor();
                }
            }));
    }

    public static void destructor() {
        torrentManager.stop();
    }
}
