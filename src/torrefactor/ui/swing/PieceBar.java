package torrefactor.ui.swing;

import java.util.*;
import java.awt.*;
import javax.swing.*;

import torrefactor.core.*;


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
        int pieceNumber = this.pieceManager.pieceNumber();
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
        for (int i = 0; i < bitfield.length; i++) {
            for (int j = 0; j < 8; j++) {
                if ((bitfield[i] >>> j & 1) == 1) {
                    g2.fillRect(8*i+j, 0, 1, height);
                }
            }
        }
    }
}
