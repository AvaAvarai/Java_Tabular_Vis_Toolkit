package src.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import src.CsvViewer;

public class SlopeAndDistanceFeatures {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public SlopeAndDistanceFeatures(CsvViewer csvViewer, DefaultTableModel tableModel, JTable table) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
        this.table = table;
    }

    public void showDimensionDialog() {
        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, tableModel.getColumnCount(), 1);
        JSpinner spinner = new JSpinner(model);
        int result = JOptionPane.showConfirmDialog(csvViewer, spinner,
            "Select number of columns to group:",
            JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            int n = (Integer) spinner.getValue();
            calculateFeatures(n);
        }
    }

    private void calculateFeatures(int n) {
        List<Integer> numericColumns = new ArrayList<>();
        
        // Get numeric columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            int modelIndex = table.convertColumnIndexToModel(i);
            if (isColumnNumeric(modelIndex)) {
                numericColumns.add(modelIndex);
            }
        }

        if (numericColumns.size() < 2) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two numeric columns.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // For each row, divide each value by the next value
        for (int i = 0; i < numericColumns.size(); i++) {
            int currentCol = numericColumns.get(i);
            int nextCol = numericColumns.get((i + 1) % numericColumns.size());
            
            String columnName = csvViewer.getUniqueColumnName(
                "Slope(" + tableModel.getColumnName(currentCol) + 
                "/" + tableModel.getColumnName(nextCol) + ")");
            
            tableModel.addColumn(columnName);
            int newColIndex = tableModel.getColumnCount() - 1;
            
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    double current = Double.parseDouble(tableModel.getValueAt(row, currentCol).toString());
                    double next = Double.parseDouble(tableModel.getValueAt(row, nextCol).toString());
                    
                    double slope = next != 0 ? current / next : 0;
                    tableModel.setValueAt(String.format("%.4f", slope), row, newColIndex);
                } catch (NumberFormatException e) {
                    tableModel.setValueAt("0.0000", row, newColIndex);
                }
            }
        }
    }

    private boolean isColumnNumeric(int columnIndex) {
        try {
            Double.parseDouble(tableModel.getValueAt(0, columnIndex).toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}