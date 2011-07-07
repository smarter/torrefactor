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

package torrefactor.ui.qt;

import com.trolltech.qt.QVariant;
import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.*;

import torrefactor.ui.qt.*;

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
