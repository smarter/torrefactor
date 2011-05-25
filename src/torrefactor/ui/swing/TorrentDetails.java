package torrefactor.ui.swing;

import torrefactor.core.Torrent;
import torrefactor.util.HumanReadable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.text.*;

/**
 * This container shows details about a particular torrent.
 */
class TorrentDetails extends Box {
    private Torrent torrent;
    private Container columnA = Box.createVerticalBox();
    private Container columnB = Box.createVerticalBox();
    private JLabel nameLabel = new JLabel("<html><b>Name:");
    private JLabel name = new JLabel();
    private JLabel authorLabel = new JLabel("<html><b>Author:");
    private JLabel author = new JLabel();
    private JLabel creationDateLabel = new JLabel("<html><b>Creation date:");
    private JLabel creationDate = new JLabel();
    private JLabel sizeLabel = new JLabel("<html><b>Size:");
    private JLabel size = new JLabel();
    private JLabel pathLabel = new JLabel("<html><b>Data path:");
    private JLabel path = new JLabel();
    private JLabel piecesLabel = new JLabel("<html><b>Pieces:");
    private JLabel pieces = new JLabel();
    private JLabel pieceSizeLabel = new JLabel("<html><b>Piece size:");
    private JLabel pieceSize = new JLabel();
    private JLabel pieceBarLabel = new JLabel("<html><b>State:");
    private PieceBar pieceBar = new PieceBar();
    
    //We use unbreakable spaces so empty labels have a non-zero height. 
    static private final String NBSP = " ";

    public TorrentDetails () {
        super(BoxLayout.X_AXIS);
        add(this.columnA);
        add(Box.createHorizontalStrut(5));
        add(this.columnB);

        //FIXME: we should choose the longer of all the label dynamically
        Dimension min = this.creationDateLabel.getPreferredSize();

        this.columnB.setMinimumSize(new Dimension(100, 100));
        //this.columnB.setMaximumSize(new Dimension(100, 100));

        this.nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.nameLabel.setMinimumSize(min);
        this.nameLabel.setMaximumSize(min);
        this.columnA.add(this.nameLabel);

        this.columnB.add(this.name);

        this.authorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.authorLabel.setMinimumSize(min);
        this.authorLabel.setMaximumSize(min);
        this.columnA.add(this.authorLabel);

        this.columnB.add(this.author);

        this.creationDateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.creationDateLabel.setMinimumSize(min);
        this.creationDateLabel.setMaximumSize(min);
        this.columnA.add(this.creationDateLabel);

        this.columnB.add(this.creationDate);

        this.sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.sizeLabel.setMinimumSize(min);
        this.sizeLabel.setMaximumSize(min);
        this.columnA.add(this.sizeLabel);

        this.columnB.add(this.size);

        this.pathLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.pathLabel.setMinimumSize(min);
        this.pathLabel.setMaximumSize(min);
        this.columnA.add(this.pathLabel);

        this.columnB.add(this.path);

        this.piecesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.piecesLabel.setMinimumSize(min);
        this.piecesLabel.setMaximumSize(min);
        this.columnA.add(this.piecesLabel);

        this.columnB.add(this.pieces);

        this.pieceSizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.pieceSizeLabel.setMinimumSize(min);
        this.pieceSizeLabel.setMaximumSize(min);
        this.columnA.add(this.pieceSizeLabel);

        this.columnB.add(this.pieceSize);

        Dimension pieceBarDim = new Dimension((int) min.getWidth(), (int) this.pieceBar.getMinimumSize().getHeight());
        this.pieceBarLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.pieceBarLabel.setMinimumSize(pieceBarDim);
        this.pieceBarLabel.setMaximumSize(pieceBarDim);
        this.columnA.add(this.pieceBarLabel);

        this.pieceBar.setVisible(false);
        this.columnB.add(this.pieceBar);
    }

    /**
     * Set the torrent whose details are displayed.
     */
    public void setTorrent (Torrent torrent) {
        this.torrent = torrent;
        updateData();
    }

    /**
     * Updates all the components to show the information of this.torrent.
     */
    public void updateData() {
        if (this.torrent == null) {
            emptyLabels();
            return;
        }
        this.name.setText(torrent.FILE_NAME);
        String author = torrent.getAuthor();
        if (author.isEmpty()) {
            this.author.setText(NBSP);
        } else {
            this.author.setText(author);
        }
        String date = DateFormat.getInstance().format(
                new Date(torrent.getCreationDate() * 1000));
        if (date.isEmpty()) {
            this.creationDate.setText(NBSP);
        } else {
            this.creationDate.setText(date);
        }
        this.size.setText(HumanReadable.fromLong(torrent.getSize()));
        this.path.setText(torrent.getBasePath());
        this.pieces.setText("" + torrent.pieceManager.piecesDownloaded()
                            + '/' + torrent.pieceManager.piecesNumber());
        this.pieceSize.setText(HumanReadable.fromLong(torrent.getPieceSize()));
        this.pieceBar.setPieceManager(torrent.pieceManager);
        this.pieceBar.setVisible(true);
        repaint();
    }

    private void emptyLabels () {
        this.name.setText(NBSP);
        this.author.setText(NBSP);
        this.creationDate.setText(NBSP);
        this.size.setText(NBSP);
        this.path.setText(NBSP);
        this.pieces.setText(NBSP);
        this.pieceSize.setText(NBSP);
    }

    /**
     * Retuns the name of this container. (This sets the label of the tab if
     * this container is added to a JTabbedPane.)
     */
    public String getName() {
        return "Details";
    }

}
