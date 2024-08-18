package src.managers;

import src.CsvViewer;
import src.table.ReorderableTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
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
    }

    public void deleteColumn(int viewColumnIndex) {
        int modelColumnIndex = csvViewer.getTable().convertColumnIndexToModel(viewColumnIndex);
        int classColumnIndex = csvViewer.getClassColumnIndex();

        if (modelColumnIndex == classColumnIndex) {
            JOptionPane.showMessageDialog(csvViewer, "Cannot delete the class column.", "Deletion Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (csvViewer.getStateManager().getOriginalColumnNames() != null && modelColumnIndex < csvViewer.getStateManager().getOriginalColumnNames().size()) {
            csvViewer.getStateManager().getOriginalColumnNames().remove(modelColumnIndex);
        }

        TableColumnModel columnModel = csvViewer.getTable().getColumnModel();
        columnModel.removeColumn(columnModel.getColumn(viewColumnIndex));

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = modelColumnIndex; col < tableModel.getColumnCount() - 1; col++) {
                tableModel.setValueAt(tableModel.getValueAt(row, col + 1), row, col);
            }
        }

        tableModel.setColumnCount(tableModel.getColumnCount() - 1);
        tableModel.setColumnIdentifiers(csvViewer.getStateManager().getOriginalColumnNames().toArray());

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
}
