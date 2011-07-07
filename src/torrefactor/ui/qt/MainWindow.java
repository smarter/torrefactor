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
import torrefactor.ui.qt.*;
import torrefactor.util.*;

import com.trolltech.qt.QVariant;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

import java.io.*;

public class MainWindow extends QMainWindow {
    private Torrent torrent;

    private QWidget centralWidget;
    private QTableView torrentView;
    private TorrentModel torrentModel;
    private QVBoxLayout layout;

    private QAction openAction;
    private QAction startAction;
    private QAction stopAction;

    private QToolBar torrentBar;

    private QMenu torrentMenu;

    public MainWindow() {
        centralWidget = new QWidget(this);
        layout = new QVBoxLayout(centralWidget);
        setCentralWidget(centralWidget);
        torrentModel = new TorrentModel();
        torrentView = new QTableView(centralWidget);
        torrentView.setModel(torrentModel);
        torrentView.setItemDelegate(new TorrentDelegate());
        torrentView.setShowGrid(false);
        torrentView.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows);
        torrentView.verticalHeader().setVisible(false);
        layout.addWidget(torrentView);

        setupActions();
        setupToolbars();
        setupMenus();
    }

    public void setupActions() {
        openAction = new QAction(QIcon.fromTheme("document-open"), tr("&Add Torrent"), this);
        openAction.triggered.connect(this, "addTorrent()");
        startAction = new QAction(QIcon.fromTheme("media-playback-start"), tr("&Start Downloading"), this);
        startAction.triggered.connect(this, "startDownload()");
        stopAction = new QAction(QIcon.fromTheme("media-playback-stop"), tr("&Stop Downloading"), this);
        stopAction.triggered.connect(this, "stopDownload()");
    }

    public void setupToolbars() {
        torrentBar = addToolBar(tr(""));
        torrentBar.addAction(openAction);
        torrentBar.addAction(startAction);
        torrentBar.addAction(stopAction);
    }

    public void setupMenus() {
        torrentMenu = menuBar().addMenu(tr("&Torrent"));
        torrentMenu.addAction(openAction);
        torrentMenu.addAction(startAction);
        torrentMenu.addAction(stopAction);
    }

    public void addTorrent() throws Exception {
        String fileName = QFileDialog.getOpenFileName(this,
               tr("Open Torrent"), QDir.currentPath(),
               new QFileDialog.Filter(tr("Torrent file (*.torrent)")));
        addTorrent(fileName);
    }

    public void addTorrent(String fileName) throws Exception {
        torrent = new Torrent(fileName);
        //String savePath = QFileDialog.getSaveFileName(this,
        //       tr("Save File"), QDir.currentPath());
        //torrent.createFile(savePath);
        torrentModel.addTorrent(torrent);
    }

    public void startDownload() {
        Log.i().debug(this, torrentView.currentIndex().row());
    }

    public void stopDownload() {
    }
}
