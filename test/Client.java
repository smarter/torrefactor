package test;

import torrefactor.core.Torrent;

public class Client {
    public static void main(String[] args)
    throws java.io.IOException, java.io.FileNotFoundException, java.net.ProtocolException,
           java.security.NoSuchAlgorithmException, torrefactor.util.InvalidBencodeException {
        if (args.length < 2) {
            System.out.println("Usage: java test.Client <input.torrent> <output>");
            System.exit(1);
        }
        Torrent torrent = new Torrent(args[0]);
        torrent.createFile(args[1]);
        torrent.start();
    }
}
