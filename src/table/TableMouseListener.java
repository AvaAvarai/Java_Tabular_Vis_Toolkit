package src.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.JTableHeader;

import src.CsvViewer;

public class TableMouseListener extends MouseAdapter {
    private CsvViewer csvViewer;

    public TableMouseListener(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTable table = csvViewer.table;
        JTableHeader header = table.getTableHeader();
        int column = header.columnAtPoint(e.getPoint());

        if (column != -1 && e.getClickCount() == 2) {
            int confirm = JOptionPane.showConfirmDialog(
                    csvViewer,
                    "Are you sure you want to delete this column?",
                    "Delete Column",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                csvViewer.deleteColumn(column);
            }
        }
    }
}
