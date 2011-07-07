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
