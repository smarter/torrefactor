package torrefactor.gui;

import com.trolltech.qt.QVariant;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

import torrefactor.gui.*;

public class TorrentDelegate extends QStyledItemDelegate {
    public void paint(QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        if (index.column() == TorrentModel.Column.Progress.ordinal()) {
            int progress = QVariant.toInt(index.data());
            QStyleOptionProgressBarV2 bar = new QStyleOptionProgressBarV2();
            bar.setRect(option.rect());
            bar.setMinimum(0);
            bar.setMaximum(100);
            bar.setProgress(progress);
            bar.setText(Integer.toString(progress) + "%");
            bar.setTextVisible(true);

            QApplication.style().drawControl(QStyle.ControlElement.CE_ProgressBar, bar, painter);
        } else {
            super.paint(painter, option, index);
        }
    }
}
