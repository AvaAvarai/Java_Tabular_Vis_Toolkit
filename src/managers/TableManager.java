package src.managers;

import src.CsvViewer;
import src.table.ReorderableTableModel;
import src.table.NumericStringComparator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TableManager {

    private final CsvViewer csvViewer;
    private final ReorderableTableModel tableModel;

    public TableManager(CsvViewer csvViewer, ReorderableTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void updateTableData(List<String[]> data) {
        int currentCaretPosition = csvViewer.getStatsTextArea().getCaretPosition();

        tableModel.setRowCount(0);
        for (String[] row : data) {
            tableModel.addRow(row);
        }

        List<String> originalColumnNames = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            originalColumnNames.add(tableModel.getColumnName(i));
        }
        csvViewer.getStateManager().setOriginalColumnNames(originalColumnNames);

        if (csvViewer.getStateManager().isHeatmapEnabled() || csvViewer.getStateManager().isClassColorEnabled()) {
            csvViewer.getRendererManager().applyCombinedRenderer();
        } else {
            csvViewer.getRendererManager().applyDefaultRenderer();
        }
        csvViewer.getDataHandler().updateStats(tableModel, csvViewer.getStatsTextArea());
        csvViewer.updateSelectedRowsLabel();

        currentCaretPosition = Math.min(currentCaretPosition, csvViewer.getStatsTextArea().getText().length());
        csvViewer.getPureRegionManager().calculateAndDisplayPureRegions(csvViewer.getThresholdSlider().getValue());
        csvViewer.getStatsTextArea().setCaretPosition(currentCaretPosition);

        // Apply NumericStringComparator to all columns
        TableRowSorter<ReorderableTableModel> sorter = new TableRowSorter<>(tableModel);
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            sorter.setComparator(i, new NumericStringComparator());
        }
        csvViewer.getTable().setRowSorter(sorter);
    }

    public void deleteColumn(int viewColumnIndex) {
        int modelColumnIndex = csvViewer.getTable().convertColumnIndexToModel(viewColumnIndex);
        int classColumnIndex = csvViewer.getClassColumnIndex();

        if (modelColumnIndex == classColumnIndex) {
            JOptionPane.showMessageDialog(csvViewer, "Cannot delete the class column.", "Deletion Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TableColumnModel columnModel = csvViewer.getTable().getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(viewColumnIndex));

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = modelColumnIndex; col < tableModel.getColumnCount() - 1; col++) {
                tableModel.setValueAt(tableModel.getValueAt(row, col + 1), row, col);
            }
        }

        tableModel.setColumnCount(tableModel.getColumnCount() - 1);

        csvViewer.getDataHandler().updateStats(tableModel, csvViewer.getStatsTextArea());
        csvViewer.updateSelectedRowsLabel();
        csvViewer.getPureRegionManager().calculateAndDisplayPureRegions(csvViewer.getThresholdSlider().getValue());
    }

    public void highlightBlanks() {
        csvViewer.getTable().setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value == null || value.toString().trim().isEmpty()) {
                    c.setBackground(Color.YELLOW);
                } else {
                    c.setBackground(Color.WHITE);
                }
                c.setForeground(csvViewer.getStateManager().getCellTextColor());
                return c;
            }
        });
        csvViewer.getTable().repaint();
    }

    public void autoResizeColumns() {
        TableColumnModel columnModel = csvViewer.getTable().getColumnModel();
        for (int column = 0; column < csvViewer.getTable().getColumnCount(); column++) {
            // Get width of column header
            TableColumn tableColumn = columnModel.getColumn(column);
            JTableHeader header = csvViewer.getTable().getTableHeader();
            int preferredWidth = tableColumn.getMinWidth();
            
            // Get maximum width of column data
            for (int row = 0; row < csvViewer.getTable().getRowCount(); row++) {
                TableCellRenderer cellRenderer = csvViewer.getTable().getCellRenderer(row, column);
                Component c = cellRenderer.getTableCellRendererComponent(
                    csvViewer.getTable(),
                    csvViewer.getTable().getValueAt(row, column),
                    false, false, row, column);
                preferredWidth = Math.max(preferredWidth, c.getPreferredSize().width + 10);
            }
            
            // Get width of column header
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = header.getDefaultRenderer();
            }
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                csvViewer.getTable(), tableColumn.getHeaderValue(),
                false, false, 0, column);
            preferredWidth = Math.max(preferredWidth, headerComp.getPreferredSize().width + 10);
            
            // Set the width
            tableColumn.setPreferredWidth(preferredWidth);
        }
    }
}
