package torrefactor.ui.qt;

import torrefactor.ui.qt.MainWindow;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

public class Main {
    public static void main(String args[]) {
        QApplication.initialize(args);
        MainWindow w = new MainWindow();
        w.show();
        QApplication.exec();
    }
}