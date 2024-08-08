package src;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TableMouseListener extends MouseAdapter {
    private CsvViewer csvViewer;

    public TableMouseListener(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        csvViewer.updateSelectedRowsLabel();
    }
}
