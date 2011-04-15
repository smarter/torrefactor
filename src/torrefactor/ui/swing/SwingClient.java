package torrefactor.ui.swing;

import torrefactor.core.TorrentManager;
import torrefactor.core.Torrent;

public class SwingClient {
    private TorrentManager torrentManager;
    private MainWindow mainWindow;

    public static void main(String[] args)
    throws java.io.IOException, java.io.FileNotFoundException,
    java.net.ProtocolException, java.security.NoSuchAlgorithmException,
    torrefactor.util.InvalidBDecodeException {
        SwingClient swingClient = new SwingClient ();
    }

    public SwingClient () {
        this.torrentManager = new TorrentManager("");
        this.mainWindow = new MainWindow(this.torrentManager);
        this.mainWindow.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                public void run() {
                    destructor();
                }
            }));
    }

    public void destructor() {
        this.torrentManager.stop();
    }
}
