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

import torrefactor.util.Config;
import torrefactor.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class displays a dialog with the options of torrefactor.
 */
public class ConfigDialog implements ItemListener {
    private static final Config CONF = Config.getConfig();
    private static final Logger LOG = new Logger();
    private JDialog dialog;
    private Frame parent;
    private Container contentPane;
    private Container contentPaneBottom;
    private JCheckBox dht;
    private JLabel basePathLabel;
    private JTextField basePathField;
    private JButton basePathButton;
    private Container basePathContainer;

    /**
     * Create a new config dialog with parent as parent frame.
     */
    public ConfigDialog (Frame parent) {
        LayoutManager layout;
        this.parent = parent;
        this.dialog = new JDialog(parent, "Configuration", true);
        this.contentPane = this.dialog.getContentPane();

        layout = new BoxLayout(this.contentPane, BoxLayout.Y_AXIS);
        this.contentPane.setLayout(layout);

        JLabel label = new JLabel(
                "Changes are applied imediately but used for new "
                + "connections only.");
        this.contentPane.add(label);

        this.dht = new JCheckBox(
                "Enable DHT",
                CONF.getPropertyBoolean("DHT"));
        this.dht.addItemListener(this);
        this.contentPane.add(this.dht);

        this.basePathContainer = new JPanel ();
        layout = new BoxLayout(this.basePathContainer, BoxLayout.X_AXIS);
        this.basePathContainer.setLayout(layout);
        this.basePathLabel = new JLabel("Download directory:");
        this.basePathLabel.setOpaque(true);

        Dimension dim = this.basePathLabel.getPreferredSize();
        dim = new Dimension(Integer.MAX_VALUE, dim.height);
        this.basePathLabel.setMaximumSize(dim);

        this.basePathField = new JTextField(
                CONF.getProperty("Ui.Swing.BasePath"));
        this.basePathField.setEditable(false);
        this.basePathButton = new JButton("Change…");
        this.basePathButton.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String directory = chooseDirectory();
                if (directory == null) return;
                basePathField.setText(directory);
                CONF.setProperty("Ui.Swing.BasePath", directory);
            }
        });
    //    this.basePathContainer.add(this.basePathLabel);
        this.basePathContainer.add(this.basePathField);
        this.basePathContainer.add(this.basePathButton);
        this.contentPane.add(this.basePathLabel);
        this.contentPane.add(this.basePathContainer);

        layout = new FlowLayout (FlowLayout.RIGHT);
        this.contentPaneBottom = new JPanel (layout);

        JButton closeButton = new JButton ("Close");
        closeButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent event) {
                try {
                    CONF.store();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
                dialog.dispose();
            }
        });
        this.contentPaneBottom.add (closeButton);
        this.contentPane.add (contentPaneBottom);

        this.dialog.pack ();
        this.dialog.setLocationRelativeTo(this.parent);
    }

    /**
     * Shows or hide the dialog.
     */
    public void setVisible (boolean bool) {
        this.dialog.setVisible (true);
    }
    
    /**
     * This methods implements ItemEventListener. It is intended to be called
     * by the JCheckBox-es of the dialog.
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

        if (source == this.dht) {
            CONF.setPropertyBoolean("DHT", selected);
        }
    }

    /**
     * This method open a FileChooser to choose a directory and return the path
     * of the selected directory. Returns null if nothing as been selected.
     */
    private String chooseDirectory () {
        JFileChooser chooser = new JFileChooser (
                System.getProperty("user.dir"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int returnVal = chooser.showOpenDialog(this.dialog);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String directory = chooser.getSelectedFile().getAbsolutePath();
            return directory;
        }

        return null;
    }
}
