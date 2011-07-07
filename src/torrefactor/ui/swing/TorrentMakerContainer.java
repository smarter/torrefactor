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
