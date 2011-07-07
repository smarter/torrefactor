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

import java.util.*;
import java.awt.*;
import javax.swing.*;

import torrefactor.core.*;
import torrefactor.util.*;

/**
 * A JComponent for representing pieces which have finished downloading and which
 * are currently being downloaded in a bar.
 */
public class PieceBar extends JComponent {
    private PieceManager pieceManager;

    private static final Color IN_PROGRESS_COLOR = Color.GREEN;
    private static final Color FINISHED_COLOR = Color.BLUE;

    private static final int BORDER_THICKNESS = 1;
    private static final int MIN_WIDTH = 1;
    private static final int HEIGHT = 20;

    private Dimension minimumSize = new Dimension(MIN_WIDTH, HEIGHT);
    private Dimension maximumSize = new Dimension(Short.MAX_VALUE, HEIGHT);

    private String toolTipText = "<html>This bar displays the downloaded pieces (in blue)<br>"
        + "and the pieces whose download is in progress (in green).</html>";

    public PieceBar() {
        setMinimumSize(minimumSize);
        setPreferredSize(minimumSize);
        setMaximumSize(maximumSize);
        setBorder(BorderFactory.createLineBorder(Color.black, BORDER_THICKNESS));
        setToolTipText(toolTipText);
    }

    /**
     * Set the PieceManager used to retrieve the number of pieces,
     * length of each piece, bitfield and intervalMap needed to display
     * the bar.
     */
    public void setPieceManager(PieceManager pieceManager) {
        this.pieceManager = pieceManager;
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (this.pieceManager == null) {
            return;
        }
        int pieceNumber = this.pieceManager.piecesNumber();
        int pieceLength = this.pieceManager.pieceLength();
        byte[] bitfield = this.pieceManager.bitfield;
        IntervalMap blockMap = this.pieceManager.intervalMap;

        Graphics2D g2 = (Graphics2D) g;
        g.translate(BORDER_THICKNESS, BORDER_THICKNESS);
        int width = getWidth() - 2*BORDER_THICKNESS;
        int height = getHeight() - 2*BORDER_THICKNESS;

        double wScale = (double)width / pieceNumber;
        g2.scale(wScale,1);

        g2.setColor(IN_PROGRESS_COLOR);
        for (Map.Entry<Long, Long> block: blockMap.entrySet()) {
            int firstPiece = (int) (block.getKey().longValue() /  pieceLength);
            int lastPiece = (int) ((block.getValue().longValue() - 1) / pieceLength + 1);
            g2.fillRect(firstPiece, 0, lastPiece - firstPiece, height);
        }
        g2.setColor(FINISHED_COLOR);
        for (int i = 0; i < 8*bitfield.length; i++) {
            if (ByteArrays.isBitSet(bitfield, i)) {
                g2.fillRect(i, 0, 1, height);
            }
        }
    }
}
