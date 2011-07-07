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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.AbstractTableModel;

import java.net.*;
import java.util.*;
// javax.swing collides with java.util thus explicitly import what we want
import java.util.List;

import torrefactor.core.*;
import torrefactor.util.Logger;
import torrefactor.util.HumanReadable;

/**
 * This class implements a table model to display the peers for a particular
 * torrent.
 */
public class TorrentPeersTableModel extends AbstractTableModel {
    private final Logger LOG = new Logger();
    private Torrent torrent;
    private Map<InetAddress, Peer> peerMap;
    public String[] columnNames = {"Address", "Port", "Id", "Connected",
                                   "Uploaded", "Downloaded", };
    public enum Column { HOST, PORT, ID, CONNECTED, UPLOADED, DOWNLOADED };
    private Column[] columns;

    public TorrentPeersTableModel() {
        this.columns = new Column[] { Column.HOST, Column.PORT, Column.ID,
                                      Column.CONNECTED, Column.UPLOADED, 
                                      Column.DOWNLOADED };
    }
    
    /**
     * Set the torrent for which the peers are displayed.
     */
    public void setTorrent (Torrent torrent) {
        if (torrent == null) {
            return;
        }
        this.torrent = torrent;
        updatePeers ();
    }


    /**
     * Update the data and make the table redraw.
     */
    public void updatePeers () {
        if (torrent != null) {
            this.peerMap = torrent.getPeerMap();
            fireTableDataChanged ();
        }
    }

    /**
     * Set the list of colums ot be displayed in the table.
     */
    public void setColumns (Column[] columns) {
        this.columns = columns;
        fireTableStructureChanged ();
    }

    /**
     * Returns the name of the column.
     */
    public String getColumnName (int col) {
        int i = columns[col].ordinal();
        return this.columnNames[i];
    }

    /**
     * Returns the number of rows in the model.
     */
    public int getRowCount () {
        if (this.peerMap == null) return 0;
        return this.peerMap.size ();
    }

    /**
     * Retuns the number of columns in the model.
     */
    public int getColumnCount () {
        return this.columns.length;
    }

    /**
     * Returns the peer which is displayed at the given row.
     */
    public Peer getPeerAt (int row) {
        int i = 0;
        for (Map.Entry<InetAddress, Peer> entry : this.peerMap.entrySet()) {
            if (i == row) {
                return entry.getValue();
            }
            i++;
        }
        return null;
    }

    /**
     * Returns the content of the cell at the given position.
     */
    public Object getValueAt(int row, int col) {
        Peer peer = getPeerAt(row);
        if (peer == null) return "null";
        Column column = this.columns[col];

        // TODO add something to data
        Object data;
        if (column == Column.HOST) {
            data = peer.getAddress ().toString ();
        } else if (column == Column.PORT) {
            data = Integer.valueOf(peer.getPort ());
        } else if (column == Column.ID) {
            data = peer.getIdAsString ();
        } else if (column == Column.CONNECTED) {
            data = peer.isConnected() ? "Connected" : "Disconnected";
        } else if (column == Column.UPLOADED) {
            data = HumanReadable.fromLong(peer.downloaded ());
        } else if (column == Column.DOWNLOADED) {
            data = HumanReadable.fromLong(peer.uploaded ());
        } else {
            data = "No such column.";
        }
        return data;
    }

    /**
     * Returns whether the cell at the given position is editable or not.
     */
    public boolean isCellEditable (int row, int col) {
        return false;
    }

}
