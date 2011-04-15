package test.ui;

import torrefactor.ui.swing.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AboutDialogTest {

    public static void main (String[] args) {
        final JFrame mainFrame = new JFrame ();
        JButton button = new JButton ("About");

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        // pack gives me a 1x1 windows which is obviously NOT enough...
        mainFrame.setSize(100,100);

        mainFrame.getContentPane().add(button);

        button.addActionListener(new ActionListener () {
            public void actionPerformed(ActionEvent event) {
                AboutDialog d = new AboutDialog (mainFrame);

                d.setProgramName ("FooBar");
                d.setVersion ("1.0");
                d.setDescription ("Description\nThis is on a new line.");
                d.pack();
                d.setVisible (true);
            }
        });

        mainFrame.setVisible(true);
    }
}

