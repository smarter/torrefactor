package torrefactor.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.AbstractTableModel;

import java.util.*;
// javax.swing collides with java.util; explicitly import what we want
import java.util.List;

import torrefactor.core.*;
import torrefactor.util.*;

import java.security.NoSuchAlgorithmException;
import java.io.IOException;

/**
 * This class implements a table model to display the torrents handled by a
 * TorrentManager.
 */
public class TorrentTableModel extends AbstractTableModel {
    public String[] columnNames = {"Torrent", "State", "Progress", "Uploaded",
                                   "Downloaded"};
    public enum Column { NAME, STATE, PERCENT, UPLOADED, DOWNLOADED };
    public Column[] columns;

    public TorrentTableModel () {
        this.columns = new Column[] {Column.NAME, Column.STATE, Column.PERCENT,
                                     Column.UPLOADED, Column.DOWNLOADED};
        fireTableDataChanged();
    }

    public Torrent getTorrentAt(int row) {
        return TorrentManager.instance().torrentList().get(row);
    }

    /**
     * Adds a new torrent to the TorrentManager and display it at the end of
     * the table.
     */
    public Torrent addTorrent(String directory, String basePath)
    throws IOException, InvalidBDecodeException, NoSuchAlgorithmException {
        Torrent t = TorrentManager.instance().addTorrent(directory, basePath);
        int rows = TorrentManager.instance().torrentList().size();
        fireTableRowsInserted(rows - 1, rows - 1);
        return t;
    }

    /**
     * Stops the TorrentManager.
     */
    public void stop() {
        TorrentManager.instance().stop();
    }

    /**
     * Set the list of columns to be displayed in the table.
     */
    public void setColumns(Column[] columns) {
        this.columns = columns;
        fireTableStructureChanged ();
    }

    /**
     * Returns the name of the column.
     */
    public String getColumnName(int col) {
        int i = columns[col].ordinal();
        return this.columnNames[i];
    }

    /**
     * Returns the number of rows in the model.
     */
    public int getRowCount() {
        return TorrentManager.instance().torrentList().size();
    }

    /**
     * Returns the number of columns in the model.
     */
    public int getColumnCount() {
        return this.columns.length;
    }

    /**
     * Returns the content of the cell at the given position.
     */
    public Object getValueAt(int row, int col) {
        Torrent torrent = TorrentManager.instance().torrentList().get(row);
        Column column = this.columns[col];

        Object data;
        if (column == Column.NAME) {
            data = torrent.FILE_NAME;
        } else if (column == Column.STATE) {
            data = torrent.getState();
        } else if (column == Column.PERCENT) {
            data = torrent.getBoundedRangeModel();
        } else if (column == Column.UPLOADED) {
            data = HumanReadable.fromLong (torrent.uploaded()) + " ("
                   + String.format("%.2f", torrent.uploadSpeed() / 1024)
                   + "KB/s)";
        } else if (column == Column.DOWNLOADED) {
            data = HumanReadable.fromLong (torrent.downloaded()) + " ("
                   + String.format("%.2f", torrent.downloadSpeed() / 1024)
                   + "KB/s)";
        } else {
            data = "No such column: " + col;
        }
        return data;
    }

    /**
     * Returns whether the cell at the given position is editable or not.
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }

}
