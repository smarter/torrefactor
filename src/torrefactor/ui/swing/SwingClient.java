package torrefactor.ui.swing;

import torrefactor.core.TorrentManager;
import torrefactor.core.Torrent;
import torrefactor.core.PeerListener;
import torrefactor.util.Logger;
import torrefactor.util.Config;

import javax.swing.UIManager;
import java.io.File;

public class SwingClient {
    private static final Logger LOG = new Logger ();
    private static Config CONF;
    private MainWindow mainWindow;
    private PeerListener peerListener;

    public static void main(String[] args)
    throws java.io.IOException, java.io.FileNotFoundException,
    java.net.ProtocolException, java.security.NoSuchAlgorithmException,
    torrefactor.util.InvalidBDecodeException {
        // Explicitly enable antialiased fonts since java doesn't do it by
        // default (on Linux at least)
        System.setProperty("awt.useSystemAAFontSettings","on");

        setLookAndFeel();

        // Initialize an instance of config using the default path for the
        // config file.
        new Config();

        SwingClient swingClient = new SwingClient ();
    }

    public SwingClient () {
        CONF = Config.getConfig();
        this.mainWindow = new MainWindow();
        this.mainWindow.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                public void run() {
                    destructor();
                }
            }));

        this.peerListener = new PeerListener(CONF.getPropertyInt("ListenPort"));
        Thread t = new Thread(this.peerListener);
        t.setDaemon(true);
        t.start();
    }

    public void destructor() {
        TorrentManager.instance().stop();
    }

    /**
     * Set the look and feel to the system look and feel if possible. This must
     * be called before any other call to swing otherwise it may not work
     * correctly.
     */
    public static void setLookAndFeel () {
        String lookAndFeel = null;

        // Try to use the system look and feel 
        try {
            lookAndFeel =  UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            LOG.debug("Cannot use system look and feel " + lookAndFeel + ": "
                      + e.getMessage());
        }

        // HACK: Java doesn't detect GTK as system look and feel on some
        // configurations thus if he choosed metal, we try to use GTK.
        if (lookAndFeel.equals(
                    UIManager.getCrossPlatformLookAndFeelClassName())) {
            try {
                lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (Exception e) {
                LOG.info("Couldn't use GTK look and fell: "
                         + e.getMessage());
            }
        }

        LOG.info("Using look and feel " + UIManager.getLookAndFeel());
    }
}
