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
