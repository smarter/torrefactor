package torrefactor.ui.swing;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;
import java.security.NoSuchAlgorithmException;

import torrefactor.core.Torrent;
import torrefactor.core.TorrentManager;
import torrefactor.util.*;

// swing colide with java.util so import the one we want explicitly
import javax.swing.Timer;
import java.util.List;

public class MainWindow implements ActionListener {
    private static Logger LOG = new Logger();

    private JFrame mainFrame;
    private JMenuBar menuBar;
    private JScrollPane torrentPane;
    private TorrentTableModel torrentModel;
    private JTable torrentTable;
    private TorrentManager torrentManager;
    private String basePath = "./data";
    private Timer tableTimer;

    public MainWindow (TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.mainFrame = new JFrame ();
        this.menuBar = new JMenuBar ();
        this.buildMenu ();
        this.buildTorrentTable (this.torrentManager.torrentList ());

        // HACK: Use BorderLayout as LayoutManager for the mainFrame since I
        // didn't manage to make swing display the menubar correctly with a
        // decent layout.
        LayoutManager layout = new BorderLayout ();
        this.mainFrame.setContentPane (new JPanel (layout));
        Container contentPane = this.mainFrame.getContentPane ();

        contentPane.add (this.menuBar, BorderLayout.PAGE_START);
        contentPane.add (this.torrentPane, BorderLayout.CENTER);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // FIXME: swing is so stupid it couldn't pack the JTable  inside the
        // JSscrollPane without wasting an enormous amount of space.
        mainFrame.pack();

        this.tableTimer = new Timer (1000, this);
        this.tableTimer.setActionCommand ("UpdateTorrentTable");
        this.tableTimer.start();
    }

    private void buildMenu () {
        JMenu menuFile = new JMenu ("File");
        JMenuItem itemOpen = new JMenuItem ("Open…");
        JMenuItem itemQuit = new JMenuItem ("Quit");
        menuFile.add (itemOpen);
        menuFile.addSeparator();
        menuFile.add (itemQuit);
        itemOpen.setActionCommand ("OpenTorrent");
        itemQuit.setActionCommand ("QuitClient");
        itemOpen.addActionListener (this);
        itemQuit.addActionListener (this);
                

        this.menuBar.add (menuFile);
    }

    private void buildTorrentTable (List<Torrent> torrents) {
        this.torrentModel = new TorrentTableModel (torrents);
        this.torrentTable = new JTable (this.torrentModel);
        this.torrentPane = new JScrollPane (torrentTable);
    }

    public void updateTorrentTable (List<Torrent> torrents) {
        this.torrentModel.setTorrents (torrents);
    }

    public void updateTorrentTable () {
        this.torrentModel.fireTableDataChanged ();
    }

    public void setVisible (Boolean bool) {
        this.mainFrame.pack ();
        this.mainFrame.setVisible (bool);
    }

    public void actionPerformed (ActionEvent event) {
        String action = event.getActionCommand ();
        if (action.equals("OpenTorrent")) {
            openTorrent ();
        } else if (action.equals("QuitClient")) {
            quit ();
        } else if (action.equals("UpdateTorrentTable")) {
            this.torrentModel.fireTableDataChanged ();
            this.torrentTable.repaint ();
        } else {
            LOG.error(this, "Unknown action command \"" + action + "\"");
        }
    }

    public void quit () {
            this.tableTimer.stop ();
            stopAll ();
            this.mainFrame.dispose ();
    }

    public void stopAll () {
        torrentManager.stop ();
    }

    public void openTorrent () {
            JFileChooser chooser = new JFileChooser(
                                                System.getProperty("user.dir"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Torrent", "torrent");
            chooser.setFileFilter(filter);

            //pops up a new dialog containing the file chooser with frame as
            //parent
            int returnVal = chooser.showOpenDialog(this.mainFrame);

            if(returnVal == JFileChooser.APPROVE_OPTION) {
                openTorrent (chooser.getSelectedFile().getAbsolutePath());
            }
    }

    public void openTorrent (String path) {
        try {
            Torrent torrent = this.torrentManager.addTorrent (path,
                                                              this.basePath);
            torrent.start ();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this.mainFrame, e.getMessage ());
        } catch (InvalidBDecodeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this.mainFrame,
                    "\"" + path + "\" is not a torrent file.");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this.mainFrame, e.getMessage ());
        }
        this.torrentModel.fireTableDataChanged ();
    }
}

