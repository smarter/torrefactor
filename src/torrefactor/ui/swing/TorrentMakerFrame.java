package torrefactor.ui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TorrentMakerFrame extends JFrame implements ActionListener{
    TorrentMakerContainer tmc;
    JButton close;
    JButton save;

    public static void main (String[] args) {
        TorrentMakerFrame tmf = new TorrentMakerFrame();
        tmf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tmf.setVisible(true);
    }

    public TorrentMakerFrame () {
        super("Torrefactor torrent creator");
        setLocationByPlatform(true);

        Container container = this.getContentPane();
        LayoutManager layout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(layout);

        this.tmc = new TorrentMakerContainer();
        container.add(tmc);

        Container bb = new Container();
        layout = new BoxLayout(bb, BoxLayout.X_AXIS);
        bb.setLayout(layout);
        
        this.close = new JButton("Close");
        this.close.addActionListener(this);
        bb.add(this.close);

        this.save = new JButton("Save");
        this.save.addActionListener(this);
        bb.add(this.save);

        add(bb);
        pack(); 
    }

    public void actionPerformed (ActionEvent event) {
        Object source = event.getSource();

        if (source == this.close) {
            this.setVisible(false);
        } else if (source == this.save) {
            if (this.tmc.saveTorrent()) {
                this.dispose();
            }
        } else {
            System.out.println("Received event of unknown source");
        }
    }
}
