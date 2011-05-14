package torrefactor.ui.swing;

import torrefactor.util.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class displays a dialog with the options of torrefactor.
 */
public class ConfigDialog implements ItemListener {
    private static final Config CONF = Config.getConfig();
    private JDialog dialog;
    private Frame parent;
    private Container contentPane;
    private Container contentPaneBottom;
    private JCheckBox useStupidEncryption;
    private JCheckBox forceStupidEncryption;

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

        this.useStupidEncryption = new JCheckBox(
                "Use encryption",
                CONF.getPropertyBoolean("Peer.UseStupidEncryption"));
        this.useStupidEncryption.addItemListener(this);
        this.contentPane.add(this.useStupidEncryption);

        this.forceStupidEncryption = new JCheckBox(
                "Force encryption",
                CONF.getPropertyBoolean("Peer.ForceStupidEncryption"));
        this.forceStupidEncryption.addItemListener(this);
        this.contentPane.add(this.forceStupidEncryption);

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
    }

    /**
     * Shows or hide the dialog.
     */
    public void setVisible (boolean bool) {
        this.dialog.setVisible (true);
    }
    
    /**
     * This methods implements ItemEventListener. It is intended to be called
     * byt the JCheckBox-es of the dialog.
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);

        if (source == this.useStupidEncryption) {
            CONF.setPropertyBoolean("Peer.UseStupidEncryption", selected);
        } else if (source == this.forceStupidEncryption) {
            CONF.setPropertyBoolean("Peer.ForceStupidEncryption", selected);
        }
    }
}
