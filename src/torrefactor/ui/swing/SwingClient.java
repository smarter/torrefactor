/* 
 *  This file is part of the Torrefactor project
 *  Copyright 2011 Guillaume Martres <smarter@ubuntu.com>
 *  Copyright 2011 Florian Vessaz <florian.vessaz@gmail.com> 
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *      2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

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

        new SwingClient();
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
