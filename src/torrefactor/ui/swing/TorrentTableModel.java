package torrefactor.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.AbstractTableModel;

import java.util.*;
// javax.swing collides with java.util; explicitly import what we want
import java.util.List;

import torrefactor.core.*;

public class TorrentTableModel extends AbstractTableModel {
    private List<Torrent> torrents;
    public String[] columnNames = {"Torrent", "Percentage", "Uploaded",
                                   "Downloaded"};
    public enum Column { NAME, PERCENT, UPLOADED, DOWNLOADED };
    public Column[] columns;

    public TorrentTableModel (List<Torrent> _torrents) {
        setTorrents (_torrents);
        this.columns = new Column[] {Column.NAME, Column.PERCENT,
                                     Column.UPLOADED, Column.DOWNLOADED};
    }

    public void setTorrents (List<Torrent> torrents) {
        this.torrents = torrents;
        fireTableDataChanged ();
    }

    public void setColumns (Column[] columns) {
        this.columns = columns;
        fireTableStructureChanged ();
    }

    public String getColumnName(int col) {
        int i = columns[col].ordinal();
        return this.columnNames[i];
    }

    public int getRowCount() {
        return this.torrents.size();
    }

    public int getColumnCount() {
        return this.columns.length;
    }

    public Object getValueAt(int row, int col) {
        Torrent torrent = this.torrents.get(row);
        Column column = this.columns[col];

        Object data;
        if (column == Column.NAME) {
            data = torrent.FILE_NAME;
        } else if (column == Column.PERCENT) {
            data = new Float (torrent.progress());
        } else if (column == Column.UPLOADED) {
            data = humanReadable (torrent.uploaded());
        } else if (column == Column.DOWNLOADED) {
            data = humanReadable (torrent.downloaded());
        } else {
            data = "No such column.";
        }
        return data;
    }

    // FIXME: return a human readable representation of the long
    private String humanReadable (long l) {
        long t;
        t = l / (8L * 1024L * 1024L * 1024L);
        if ( t > 0 ) {
            return new String (t + "GB");
        }
        t = l / (8L * 1024L * 1024L);
        if ( t > 0 ) {
            return new String (t + "MB");
        }
        t = l / (8L * 1024L);
        if (t > 0) {
            return new String (t + "KB");
        }
        t = l / 8L;
        if (t > 0) {
            return new String (t + "B");
        }
        return new String (t + "b");
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

}
