package torrefactor.gui;

import torrefactor.core.Torrent;
import torrefactor.gui.*;
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
