package torrefactor.ui.qt;

import torrefactor.core.Torrent;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

import java.util.*;

public class TorrentModel extends QAbstractTableModel {
    public enum Column {
        Name, Progress, Length, Downloaded, Uploaded
    }

    private List<Torrent> torrentList;

    public TorrentModel() {
        super();
        torrentList = new ArrayList<Torrent>();
    }

    public void addTorrent(Torrent torrent) {
        beginInsertRows(null, torrentList.size(), torrentList.size());
        torrentList.add(torrent);
        endInsertRows();
    }

    public void removeTorrent(Torrent torrent) {
        torrentList.remove(torrent);
        //TODO: signal new rows
    }

    public Object data(QModelIndex index, int role) {
        if (role != Qt.ItemDataRole.DisplayRole) {
            return null;
        }
        Torrent torrent = torrentList.get(index.row());
        switch (Column.values()[index.column()]) {
        case Name:
            return torrent.name();
        case Progress:
            return Integer.valueOf(torrent.progress());
        case Length:
            return Integer.valueOf(torrent.length());
        case Downloaded:
            return Integer.valueOf(torrent.downloaded());
        case Uploaded:
            return Integer.valueOf(torrent.uploaded());
        default:
            return new Object();
        }
    }

    public Object headerData(int section, Qt.Orientation orientation, int role) {
        if (role != Qt.ItemDataRole.DisplayRole || orientation == Qt.Orientation.Vertical) {
            return super.headerData(section, orientation, role);
        }

        switch (Column.values()[section]) {
        case Name:
            return tr("File");
        case Progress:
            return tr("Progress");
        case Length:
            return tr("Length");
        case Downloaded:
            return tr("Downloaded");
        case Uploaded:
            return tr("Uploaded");
        default:
            return new Object();
        }
    }

    public int rowCount(QModelIndex parent) {
        return torrentList.size();
    }

    public int columnCount(QModelIndex parent) {
        return 5;
    }
}
