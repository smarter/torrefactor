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


/**
 * This class handles the main window of the Swing interface.
 */
public class MainWindow implements ActionListener {
    private static Logger LOG = new Logger();
    private static Config CONF = Config.getConfig();

    private JFrame mainFrame;
    private JMenuBar menuBar;
    private JPopupMenu contextMenu;
    private JSplitPane splitPane;
    private JScrollPane torrentPane;
    private TorrentTableModel torrentModel;
    private JTable torrentTable;
    private JTabbedPane tabbedPane;
    private TorrentDetails torrentDetails;
    private TorrentPeers torrentPeers;
    private Timer tableTimer;

    /**
     * Create a new MainWindow. This is not a singleton because we have no use
     * of it. This class is not meant to be instanciated more than
     * once neverless.
     */
    public MainWindow () {
        this.mainFrame = new JFrame ("Torrefactor");
        this.mainFrame.setLocationByPlatform(true);
        this.menuBar = new JMenuBar ();
        this.buildMenu ();
        this.buildTorrentTable();

        // HACK: Use BorderLayout as LayoutManager for the mainFrame since I
        // didn't manage to make swing display the menubar correctly with a
        // decent layout.
        LayoutManager layout = new BorderLayout ();
        this.mainFrame.setContentPane (new JPanel (layout));
        Container contentPane = this.mainFrame.getContentPane ();

        this.tabbedPane = new JTabbedPane ();
        this.torrentDetails = new TorrentDetails ();
        this.torrentPeers = new TorrentPeers ();
        this.tabbedPane.add (torrentDetails);
        this.tabbedPane.add (torrentPeers);

        // Selection event onTorrentSelected
        ListSelectionListener listener = new ListSelectionListener () {
            public void valueChanged(ListSelectionEvent event) {
                int index = torrentTable.getSelectedRow();
                if (index == -1) {
                    //TODO: we should clear the info tabs when there is no
                    //      selection
                    return;
                }
                Torrent torrent = torrentModel.getTorrentAt(index);
                onTorrentSelected(torrent);
            }
        };
        this.torrentTable.getSelectionModel()
            .addListSelectionListener(listener);

        // Mouse event for context menu on torrent table
        MouseListener ml = new MouseListener () {
            public void mouseClicked (MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    Point p = torrentTable.getMousePosition();
                    int row = torrentTable.rowAtPoint(p);
                    ListSelectionModel model = torrentTable.getSelectionModel();
                    model.setSelectionInterval(row, row);
                    contextMenu.show(torrentTable, p.x, p.y);
                }
            }
            public void mouseExited (MouseEvent event) {}
            public void mouseEntered (MouseEvent event) {}
            public void mouseReleased (MouseEvent event) {}
            public void mousePressed (MouseEvent event) {}
        };
        this.torrentTable.addMouseListener(ml);

        this.splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                this.torrentPane,
                this.tabbedPane);
        this.splitPane.setContinuousLayout(true);
        this.splitPane.setResizeWeight(1);

        contentPane.add (this.menuBar, BorderLayout.PAGE_START);
        contentPane.add (this.splitPane, BorderLayout.CENTER);

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // FIXME: swing is so stupid it couldn't pack the JTable  inside the
        // JSscrollPane without wasting an enormous amount of space.
        mainFrame.pack();

        this.tableTimer = new Timer (1000, this);
        this.tableTimer.setActionCommand ("UpdateInfos");
        this.tableTimer.start();
    }

    /**
     * This method handles the initial construction of the menus.
     */
    private void buildMenu () {
        JMenu menuFile = new JMenu ("File");
        JMenuItem itemOpen = new JMenuItem ("Open…");
        JMenuItem itemQuit = new JMenuItem ("Quit");
        JMenuItem itemStart = new JMenuItem("Start Downloading");
        JMenuItem itemStart2 = new JMenuItem("Start");
        //itemStart.setEnabled(false);
        JMenuItem itemStop = new JMenuItem("Stop Downloading");
        JMenuItem itemStop2 = new JMenuItem("Stop");
        //itemStop.setEnabled(false);
        JMenuItem itemOptions = new JMenuItem("Options…");
        menuFile.add (itemOpen);
        menuFile.addSeparator();
        menuFile.add (itemStart);
        menuFile.add (itemStop);
        menuFile.addSeparator();
        menuFile.add (itemOptions);
        menuFile.addSeparator();
        menuFile.add (itemQuit);
        itemOpen.setActionCommand ("OpenTorrent");
        itemQuit.setActionCommand ("QuitClient");
        itemStart.setActionCommand ("StartDownload");
        itemStart2.setActionCommand ("StartDownload");
        itemStop.setActionCommand ("StopDownload");
        itemStop2.setActionCommand ("StopDownload");
        itemOptions.setActionCommand ("Options…");
        itemOpen.addActionListener (this);
        itemQuit.addActionListener (this);
        itemStart.addActionListener (this);
        itemStart2.addActionListener (this);
        itemStop.addActionListener (this);
        itemStop2.addActionListener (this);
        itemOptions.addActionListener (this);

        contextMenu = new JPopupMenu();
        contextMenu.add(itemStart2);
        contextMenu.add(itemStop2);

        this.menuBar.add (menuFile);
    }

    /**
     * This method handles the initial construction of the torrent table.
     */
    private void buildTorrentTable () {
        this.torrentModel = new TorrentTableModel();
        this.torrentTable = new JTable(this.torrentModel);
        this.torrentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.torrentTable.setRowSelectionAllowed(true);
        this.torrentPane = new JScrollPane(torrentTable);

        this.torrentTable.getColumnModel().getColumn(2)
            .setCellRenderer(new ProgressBarTableCellRenderer());
    }

    /**
     * Set the visibility of the window.
     *
     * @param bool  if true show the window
     */
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
        } else if (action.equals("UpdateInfos")) {
            this.torrentModel.fireTableRowsUpdated(
                                0, this.torrentModel.getRowCount() - 1);
            this.torrentTable.repaint();
            this.torrentDetails.repaint();
            this.torrentPeers.update();
        } else if (action.equals("Options…")) {
            ConfigDialog d = new ConfigDialog(this.mainFrame);
            d.setVisible(true);
        } else {
            LOG.error(this, "Unknown action command \"" + action + "\"");
        }
    }

    /**
     * This is must be called when the application is supposed to quit
     * gracefully.
     */
    public void quit () {
            this.tableTimer.stop ();
            stopAll ();
            this.mainFrame.dispose ();
    }

    /**
     * Stops all torrents.
     */
    public void stopAll () {
       this.torrentModel.stop();
    }

    /**
     * Show a dialog to open a new torrent.
     */
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


    /**
     * Is called when the selection on the JTable changes.
     */
    public void onTorrentSelected (Torrent torrent) {
        this.torrentDetails.setTorrent(torrent);
        this.torrentPeers.setTorrent(torrent);
    }

    /**
     * Is called to start a new torrent.
     *
     * @param path  path to the torrent file
     */
    public void openTorrent (String path) {
        try {
            String basePath = CONF.getProperty("Ui.Swing.BasePath");
            Torrent torrent = this.torrentModel.addTorrent(
                    path, basePath);
            LOG.debug("Add torrent: " + path + " basePath: " + basePath);
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

