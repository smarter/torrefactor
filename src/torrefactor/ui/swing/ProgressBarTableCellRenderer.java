package torrefactor.ui.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import torrefactor.util.Logger;


public class ProgressBarTableCellRenderer extends JProgressBar 
    implements TableCellRenderer {
    private Logger LOG = new Logger();

    public Component getTableCellRendererComponent 
    (JTable table, Object value, boolean isSelected, boolean hasFocus, int
     row, int col) {
        
        if (!(value instanceof BoundedRangeModel)) {
            LOG.error("Was asked to display something which is not a"
                      + " BoundedRangeModel, ignoring it.");
            return this;
        }

        this.setModel((BoundedRangeModel) value);

        return this;
     }

}
