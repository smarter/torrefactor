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
