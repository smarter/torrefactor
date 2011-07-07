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
