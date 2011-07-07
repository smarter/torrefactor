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
import java.io.File;

/**
 * Container containing a text field and a button to choose files or
 * directories.
 */
public class CompactFileChooser extends Container {
    StringCallback callback;
    Component parent;
    Boolean chooseDirectory = false;
    JTextField field;
    JButton button;

    CompactFileChooser () {
        LayoutManager layout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(layout);
        this.field = new JTextField("");
        this.field.setEditable(false);
        this.button = new JButton("Change…");
        this.button.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String file = chooseFile();
                if (file == null) return;
                field.setText(file);
                if (callback != null) {
                    callback.call(file);
                }
            }
        });

        add(field);
        add(button);
    }

    public void setCallback (StringCallback callback) {
        this.callback = callback;
    }

    public void setParent (Component component) {
        this.parent = component;
    }

    public File getFile () {
        return new File(this.field.getText());
    }

    /**
     * This method open a FileChooser to choose a file and return the path
     * of the selected directory. Returns null if nothing as been selected.
     */
    private String chooseFile () {
        JFileChooser chooser = new JFileChooser (
                System.getProperty("user.dir"));
        if (this.chooseDirectory) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
        }

        int returnVal = chooser.showOpenDialog(this.parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().getAbsolutePath();
            return file;
        }

        return null;
    }
}
