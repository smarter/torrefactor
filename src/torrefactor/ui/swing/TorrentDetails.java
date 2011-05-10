package torrefactor.ui.swing;

import torrefactor.core.Torrent;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.text.*;

class TorrentDetails extends Container {
    private Torrent torrent;
    private Container containerA = new Container();
    private Container containerB = new Container();
    private JLabel nameLabel = new JLabel("<html><b>Name:");
    private JLabel name = new JLabel();
    private JLabel authorLabel = new JLabel("<html><b>author:");
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

    public TorrentDetails () {
        super();
        LayoutManager layout = new GridLayout (7, 1, 3, 3);
        this.containerA.setLayout(layout);
        this.containerB.setLayout(layout);
        layout = new FlowLayout ();
        setLayout(layout);
        add(this.containerA);
        add(this.containerB);
        this.nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.nameLabel);
        this.containerB.add(this.name);
        this.authorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.authorLabel);
        this.containerB.add(this.author);
        this.creationDateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.creationDateLabel);
        this.containerB.add(this.creationDate);
        this.sizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.sizeLabel);
        this.containerB.add(this.size);
        this.pathLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.pathLabel);
        this.containerB.add(this.path);
        this.piecesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.piecesLabel);
        this.containerB.add(this.pieces);
        this.pieceSizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.containerA.add(this.pieceSizeLabel);
        this.containerB.add(this.pieceSize);
    }

    public void setTorrent (Torrent torrent) {
        this.torrent = torrent;
        updateData();
    }

    public void updateData() {
        this.name.setText(torrent.FILE_NAME);
        this.author.setText(torrent.getAuthor());
        this.creationDate.setText(
                DateFormat.getInstance()
                .format(new Date(torrent.getCreationDate())));
        this.size.setText(Long.toString(torrent.getSize()));
        this.path.setText(torrent.getBasePath());
        this.pieces.setText(Integer.toString(torrent.getNumPiece()));
        this.pieceSize.setText(Integer.toString(torrent.getPieceSize()));
    }

    public String getName() {
        return "Details";
    }

}
