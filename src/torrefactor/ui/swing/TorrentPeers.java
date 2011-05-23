package torrefactor.ui.swing;

import torrefactor.core.Torrent;
import torrefactor.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.text.*;

/**
 * This container shows the peers for a given torrent in a JTable.
 */
class TorrentPeers extends Box {
    private final Logger LOG = new Logger();
    private Torrent torrent;
    private JLabel torrentName = new JLabel();
    private JTable table;
    private TorrentPeersTableModel model;
    private JScrollPane scrollPane;

    public TorrentPeers () {
        super(BoxLayout.Y_AXIS);

        this.model = new TorrentPeersTableModel();
        this.table = new JTable(this.model);
        this.table.setRowSelectionAllowed(true);
        
        this.scrollPane = new JScrollPane(this.table);
        // Sets a small preferred size because the default use something like
        // half of my screen.
        this.scrollPane.setPreferredSize(new Dimension(0,100));
        add(this.torrentName);
        add(this.scrollPane);
        setVisible(true);
    }

    /**
     * Displays the peers for the given torrent.
     */
    public void setTorrent (Torrent torrent) {
        this.torrent = torrent;
        this.torrentName.setText(torrent.FILE_NAME);
        this.model.setTorrent(torrent);
    }

    /**
     * Update the data and make the table redraw.
     */
    public void update () {
        this.model.updatePeers();
    }

    /**
     * Returns the name of this container. (This sets the label of the tab if
     * this container is added to a JTabbedPane.)
     */
    public String getName() {
        return "Peers";
    }

}
