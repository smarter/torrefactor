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
    private JCheckBox useStupidEncryption;
    private JCheckBox forceStupidEncryption;
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
        this.contentPane.setBackground(Color.GREEN);

        layout = new BoxLayout(this.contentPane, BoxLayout.Y_AXIS);
        this.contentPane.setLayout(layout);

        JLabel label = new JLabel(
                "Changes are applied imediately but used for new "
                + "connections only.");
        this.contentPane.add(label);

        this.useStupidEncryption = new JCheckBox(
                "Use encryption",
                CONF.getPropertyBoolean("Peer.UseStupidEncryption"));
        this.useStupidEncryption.addItemListener(this);
        this.useStupidEncryption.setOpaque(true);
        this.useStupidEncryption.setBackground(Color.YELLOW);
        Dimension dima = this.useStupidEncryption.getPreferredSize();
        dima = new Dimension(Integer.MAX_VALUE, dima.height);
        this.useStupidEncryption.setMaximumSize(dima);

        this.contentPane.add(this.useStupidEncryption);

        this.forceStupidEncryption = new JCheckBox(
                "Force encryption",
                CONF.getPropertyBoolean("Peer.ForceStupidEncryption"));
        if (! CONF.getPropertyBoolean("Peer.UseStupidEncryption")) {
            this.forceStupidEncryption.setSelected(false);
            this.forceStupidEncryption.setEnabled(false);
        }
        this.forceStupidEncryption.setBackground(Color.RED);
        this.forceStupidEncryption.addItemListener(this);
        this.contentPane.add(this.forceStupidEncryption);

        this.basePathContainer = new JPanel ();
        this.basePathContainer.setBackground(Color.CYAN);
        layout = new BoxLayout(this.basePathContainer, BoxLayout.X_AXIS);
        this.basePathContainer.setLayout(layout);
        this.basePathLabel = new JLabel("Download directory:");
        this.basePathLabel.setOpaque(true);
        this.basePathLabel.setBackground(Color.YELLOW);
        LOG.debug("Yellow label alignement: "
                  + this.basePathLabel.getAlignmentX());

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
        this.contentPaneBottom.setBackground(Color.MAGENTA);

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

        if (source == this.useStupidEncryption) {
            CONF.setPropertyBoolean("Peer.UseStupidEncryption", selected);
            if (selected) {
                this.forceStupidEncryption.setEnabled(true);
            } else {
                this.forceStupidEncryption.setEnabled(false);
                this.forceStupidEncryption.setSelected(false);
            }
        } else if (source == this.forceStupidEncryption) {
            CONF.setPropertyBoolean("Peer.ForceStupidEncryption", selected);
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
