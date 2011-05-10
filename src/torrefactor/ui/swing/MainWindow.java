package torrefactor.ui.swing;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.*;
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
    private JTabbedPane tabbedPane;
    private TorrentDetails torrentDetails;
    private String basePath = "./data";
    private Timer tableTimer;

    public MainWindow (TorrentManager torrentManager) {
        this.mainFrame = new JFrame ("Torrefactor");
        this.menuBar = new JMenuBar ();
        this.buildMenu ();
        this.buildTorrentTable(torrentManager);

        // HACK: Use BorderLayout as LayoutManager for the mainFrame since I
        // didn't manage to make swing display the menubar correctly with a
        // decent layout.
        LayoutManager layout = new BorderLayout ();
        this.mainFrame.setContentPane (new JPanel (layout));
        Container contentPane = this.mainFrame.getContentPane ();

        this.tabbedPane = new JTabbedPane ();
        this.torrentDetails = new TorrentDetails ();
        this.tabbedPane.add (torrentDetails);

        // Selection event for details pane
        ListSelectionListener listener = new ListSelectionListener () {
            public void valueChanged(ListSelectionEvent event) {
                int index = event.getFirstIndex();
                Torrent torrent = torrentModel.getTorrentAt(index);
                onTorrentSelected(torrent);
            }
        };
        this.torrentTable.getSelectionModel()
            .addListSelectionListener(listener);

        contentPane.add (this.menuBar, BorderLayout.PAGE_START);
        contentPane.add (this.torrentPane, BorderLayout.CENTER);
        contentPane.add (this.tabbedPane, BorderLayout.PAGE_END);

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
        JMenuItem itemStart = new JMenuItem("Start Downloading");
        //itemStart.setEnabled(false);
        JMenuItem itemStop = new JMenuItem("Stop Downloading");
        //itemStop.setEnabled(false);
        menuFile.add (itemOpen);
        menuFile.addSeparator();
        menuFile.add (itemStart);
        menuFile.add (itemStop);
        menuFile.addSeparator();
        menuFile.add (itemQuit);
        itemOpen.setActionCommand ("OpenTorrent");
        itemQuit.setActionCommand ("QuitClient");
        itemStart.setActionCommand ("StartDownload");
        itemStop.setActionCommand ("StopDownload");
        itemOpen.addActionListener (this);
        itemQuit.addActionListener (this);
        itemStart.addActionListener (this);
        itemStop.addActionListener (this);

        this.menuBar.add (menuFile);
    }

    private void buildTorrentTable (TorrentManager torrentManager) {
        this.torrentModel = new TorrentTableModel(torrentManager);
        this.torrentTable = new JTable(this.torrentModel);
        this.torrentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.torrentTable.setRowSelectionAllowed(true);
        this.torrentPane = new JScrollPane(torrentTable);
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
        } else if (action.equals("StartDownload")) {
            if (this.torrentTable.getSelectedRow() == -1) return;
            try {
                this.torrentModel.getTorrentAt(
                                this.torrentTable.getSelectedRow()).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (action.equals("StopDownload")) {
            if (this.torrentTable.getSelectedRow() == -1) return;
            this.torrentModel.getTorrentAt(
                                this.torrentTable.getSelectedRow()).stop();
        } else if (action.equals("UpdateTorrentTable")) {
            this.torrentModel.fireTableRowsUpdated(
                                0, this.torrentModel.getRowCount() - 1);
            this.torrentTable.repaint();
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
       this.torrentModel.stop();
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


    public void onTorrentSelected (Torrent torrent) {
        this.torrentDetails.setTorrent(torrent);
    }

    public void openTorrent (String path) {
        try {
            Torrent torrent = this.torrentModel.addTorrent(path,
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
    }
}

