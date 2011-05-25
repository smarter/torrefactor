package torrefactor.ui.swing;

import torrefactor.util.HumanReadable;
import torrefactor.core.TorrentMaker;

import java.awt.*;
import javax.swing.*;
import java.io.File;

public class TorrentMakerContainer extends Container {
    CompactFileChooser inChooser = new CompactFileChooser();
    CompactFileChooser outChooser = new CompactFileChooser();
    JLabel error;

    IntWrapper[] choices = new IntWrapper[] {
        new IntWrapper(32 * 1024),
        new IntWrapper(64 * 1024),
        new IntWrapper(128 * 1024),
        new IntWrapper(256 * 1024),
        new IntWrapper(512 * 1024),
        new IntWrapper(1024 * 1024),
        new IntWrapper(2048 * 1024),
        new IntWrapper(4096 * 1024)};
    JComboBox pieceLengthCb = new JComboBox(choices);

    JTextField urlField = new JTextField();
    JTextField commentField = new JTextField();
    

    public TorrentMakerContainer () {
        LayoutManager layout = new BoxLayout(this, BoxLayout.Y_AXIS);
        this.setLayout(layout);

        this.add(new JLabel("File: "));
        add(this.inChooser);

        JLabel l;

        Container container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        l = new JLabel("Piece length: ");
        setPrefAsMaxDim(l);
        container.add(l);
        container.add(this.pieceLengthCb);
        add(container);

        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        l = new JLabel("Tracker url: ");
        setPrefAsMaxDim(l);
        container.add(l);
        container.add(this.urlField);
        add(container);

        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        l = new JLabel("Comment: ");
        setPrefAsMaxDim(l);
        container.add(l);
        container.add(this.commentField);
        add(container);

        add(new JLabel("Torrent file: "));
        add(this.outChooser);

        this.error = new JLabel();
        add(this.error);
    }

    private void setPrefAsMaxDim (Component component) {
        Dimension d = component.getPreferredSize();
        component.setMaximumSize(d);
    }

    public int getPieceLength () {
        return ((IntWrapper) this.pieceLengthCb.getSelectedItem()).i;
    }

    public File getInFile () {
        return this.inChooser.getFile();
    }

    public File getOutFile () {
        return this.outChooser.getFile();
    }

    public String getUrl () {
        return this.urlField.getText();
    }

    public String getComment () {
        return this.commentField.getText();
    }

    public boolean saveTorrent () {
        System.out.print("Writing torrent file");
        try {
            TorrentMaker.write(
                     getOutFile(),
                     getInFile(),
                     getPieceLength(),
                     getUrl(),
                     getComment());
        } catch (Exception e) {
            this.error.setText(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    class IntWrapper {
        final int i;
        
        IntWrapper (int i) {
            this.i = i;
        }

        public String toString() {
            return HumanReadable.fromLong(i);
        }
    }

}
