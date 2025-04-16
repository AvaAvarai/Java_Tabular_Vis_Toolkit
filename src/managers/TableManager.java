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
            final int column = i;
            // Only apply numeric comparator to numeric columns
            if (isNumericColumn(column)) {
                sorter.setComparator(i, new NumericStringComparator());
            }
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

        // Store current selection
        int[] selectedRows = csvViewer.getTable().getSelectedRows();
        
        // Save column order mapping
        TableColumnModel columnModel = csvViewer.getTable().getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] viewToModelMap = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            viewToModelMap[i] = csvViewer.getTable().convertColumnIndexToModel(i);
        }

        // Remove column from view
        columnModel.removeColumn(columnModel.getColumn(viewColumnIndex));
        
        // Remove data from model by creating a new dataset without the deleted column
        int newColumnCount = tableModel.getColumnCount() - 1;
        Object[][] newData = new Object[tableModel.getRowCount()][newColumnCount];
        String[] newColumnNames = new String[newColumnCount];
        
        // Copy column names (skipping the deleted one)
        int newColIndex = 0;
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != modelColumnIndex) {
                newColumnNames[newColIndex++] = tableModel.getColumnName(col);
            }
        }
        
        // Copy data (skipping the deleted column)
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            newColIndex = 0;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                if (col != modelColumnIndex) {
                    newData[row][newColIndex++] = tableModel.getValueAt(row, col);
                }
            }
        }
        
        // Update the model with new data
        tableModel.setColumnCount(0); // Clear existing columns
        tableModel.setRowCount(0);    // Clear existing rows
        
        // Add new columns
        for (String columnName : newColumnNames) {
            tableModel.addColumn(columnName);
        }
        
        // Add new rows
        for (Object[] rowData : newData) {
            tableModel.addRow(rowData);
        }
        
        // Restore column order (accounting for the removed column)
        // Create a mapping that preserves original order but skips the deleted column
        for (int i = 0; i < viewToModelMap.length; i++) {
            // Skip the deleted column
            if (i == viewColumnIndex) continue;
            
            int targetViewIndex = i < viewColumnIndex ? i : i - 1;
            int originalModelIndex = viewToModelMap[i];
            
            // Adjust model index if it's after the deleted column
            if (originalModelIndex > modelColumnIndex) {
                originalModelIndex--;
            }
            
            // Move column to preserve order
            int currentViewIndex = csvViewer.getTable().convertColumnIndexToView(originalModelIndex);
            if (currentViewIndex != targetViewIndex) {
                csvViewer.getTable().moveColumn(currentViewIndex, targetViewIndex);
            }
        }

        // Restore selection
        ListSelectionModel selectionModel = csvViewer.getTable().getSelectionModel();
        selectionModel.clearSelection();
        for (int row : selectedRows) {
            if (row < csvViewer.getTable().getRowCount()) {
                selectionModel.addSelectionInterval(row, row);
            }
        }

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

    private boolean isNumericColumn(int column) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object value = tableModel.getValueAt(row, column);
            if (value != null && !value.toString().trim().isEmpty()) {
                try {
                    Double.parseDouble(value.toString().trim());
                    return true;  // If we can parse any value as a number, consider it a numeric column
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
