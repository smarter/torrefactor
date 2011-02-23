import com.trolltech.qt.QVariant;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

public class MainWindow extends QMainWindow {
    private QWidget centralWidget;
    private QListView torrentView;
    private QVBoxLayout layout;

    private QAction openAction;
    private QAction startAction;
    private QAction stopAction;

    private QToolBar torrentBar;

    private QMenu torrentMenu;

    //private List<Torrent> torrentList = new List<Torrent>;

    public MainWindow() {
        centralWidget = new QWidget(this);
        layout = new QVBoxLayout(centralWidget);
        setCentralWidget(centralWidget);
        torrentView = new QListView(centralWidget);
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

    public void addTorrent() {
        String fileName = QFileDialog.getOpenFileName(this,
               tr("Open Torrent"), QDir.currentPath(),
               new QFileDialog.Filter(tr("Torrent file (*.torrent)")));
        addTorrent(fileName);
    }

    public void addTorrent(String fileName) {
        System.out.println(fileName);
        //torrent = new Torrent(fileName);
        //String savePath = QFileDialog.getSaveFileName(this,
        //       tr("Save File"), QDir.curentPath());
        //torrent.createFile(savePath);
    }

    public void startDownload() {
        System.out.println(torrentView.currentIndex().row());
    }

    public void stopDownload() {
    }
}
