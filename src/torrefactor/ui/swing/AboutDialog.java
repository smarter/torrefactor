package torrefactor.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AboutDialog {
    private JDialog dialog;
    private Frame parent;
    private Container contentPane;
    private JLabel programLabel;
    private JLabel descriptionLabel;
    private Container contentPaneBottom;
    private String name;
    private String version;

    public AboutDialog (Frame parent) {
        LayoutManager layout;
        this.parent = parent;
        this.dialog = new JDialog (parent, "About", true);

        // HACK: Swing/awt mess things up when I put some containers inside the
        // default contentPane. Thus I use JPanel as contentPane.
        layout = new GridLayout (0, 1);
        this.dialog.setContentPane (new JPanel (layout));

        this.contentPane = this.dialog.getContentPane ();

        this.programLabel = new JLabel ();
        this.programLabel.setHorizontalAlignment (JLabel.CENTER);
        //this.programLabel.setFont(new Font("sans",Font.BOLD,12e));
        Font currentFont = programLabel.getFont();
        this.programLabel.setFont(new Font(currentFont.getFontName(),
                                           Font.BOLD,
                                           22));
        this.descriptionLabel = new JLabel ();
        this.descriptionLabel.setHorizontalAlignment (JLabel.CENTER);
        this.descriptionLabel.setFont(new Font(currentFont.getFontName(),
                                               Font.PLAIN,
                                               currentFont.getSize()));

        layout = new FlowLayout (FlowLayout.RIGHT);
        this.contentPaneBottom = new JPanel (layout);

        JButton closeButton = new JButton ("Close");
        closeButton.addActionListener(new ActionListener () {
            public void actionPerformed (ActionEvent event) {
                dialog.dispose();
            }
        });
        this.contentPaneBottom.add (closeButton);

        this.contentPane.add (this.programLabel);
        this.contentPane.add (this.descriptionLabel);
        this.contentPane.add (contentPaneBottom);
        this.dialog.pack ();
    }

    // Call pack after calling this method to resize the dialog
    public void setProgramName (String name) {
        this.name = name;
        this.programLabel.setText (this.name + " " + this.version);
    }

    // Call pack after calling this method to resize the dialog
    public void setVersion (String version) {
        this.version = version;
        this.programLabel.setText (this.name + " " + this.version);
    }

    // Call pack after calling this method to resize the dialog
    public void setDescription (String description) {
        this.descriptionLabel.setText (convertNewLine(description));
    }

    public void setVisible (boolean bool) {
        this.dialog.setVisible (true);
    }
    
    public void pack () {
        this.dialog.pack ();
    }

    static private String convertNewLine (String string) {
        return "<html>" + string.replaceAll("\n", "<br>");
    }
}
