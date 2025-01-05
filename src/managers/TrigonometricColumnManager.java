package src.managers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

public class TrigonometricColumnManager {
    private JTable table;
    private DefaultTableModel tableModel;
    private List<String[]> originalData;
    private List<String> originalColumnNames;
    private boolean areDifferenceColumnsVisible = false;

    public TrigonometricColumnManager(JTable table) {
        this.table = table;
        this.tableModel = (DefaultTableModel) table.getModel();
    }

    public void toggleTrigonometricColumns(boolean isNormalized, Runnable normalizeData, Runnable updateTableData) {
        if (!isNormalized) {
            int choice = JOptionPane.showConfirmDialog(null,
                    "Data is not normalized. Normalization is required to insert trigonometric columns. Would you like to normalize the data now?",
                    "Normalization Required",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                normalizeData.run();
                updateTableData.run();
                isNormalized = true;
            } else {
                return;
            }
        }

        if (areDifferenceColumnsVisible) {
            removeTrigonometricColumns(); // If columns are visible, remove them
        } else {
            // No menu/dialog here - just add columns with default mode
            addTrigonometricColumns("Direct", false);
        }

        areDifferenceColumnsVisible = !areDifferenceColumnsVisible;
    }

    private void addTrigonometricColumns(String mode, boolean isInverse) {
        if (!areDifferenceColumnsVisible) {
            // Save the original data and column names before adding new columns
            originalData = new ArrayList<>();
            originalColumnNames = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String[] rowData = new String[tableModel.getColumnCount()];
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    rowData[col] = tableModel.getValueAt(row, col).toString();
                }
                originalData.add(rowData);
            }
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                originalColumnNames.add(tableModel.getColumnName(col));
            }
        }

        int numRows = tableModel.getRowCount();
        TableColumnModel columnModel = table.getColumnModel();
        int numCols = columnModel.getColumnCount();
        int classColumnIndex = getClassColumnIndex(); // Ensure we are skipping the class column

        String prefix = isInverse ? "Inverse " : "";

        for (int i = 0; i < numCols; i++) {
            int col1 = columnModel.getColumn(i).getModelIndex();
            int col2 = -1;
            String description = "";

            if (col1 == classColumnIndex) continue;

            switch (mode) {
                case "Direct":
                    description = tableModel.getColumnName(col1);
                    break;
                case "Forward Differences":
                    col2 = (i + 1) % numCols;
                    if (col2 == classColumnIndex) col2 = (col2 + 1) % numCols;
                    description = tableModel.getColumnName(col2) + " - " + tableModel.getColumnName(col1);
                    break;
                case "Backward Differences":
                    col2 = (i - 1 + numCols) % numCols;
                    if (col2 == classColumnIndex) col2 = (col2 - 1 + numCols) % numCols;
                    description = tableModel.getColumnName(col1) + " - " + tableModel.getColumnName(col2);
                    break;
            }

            // Add columns for trigonometric functions
            tableModel.addColumn(prefix + "Cos " + description);
            tableModel.addColumn(prefix + "Sin " + description);
            tableModel.addColumn(prefix + "Tan " + description);
        }

        // Populate the new columns with trigonometric values
        for (int row = 0; row < numRows; row++) {
            int newColIndex = numCols;

            for (int i = 0; i < numCols; i++) {
                int col1 = columnModel.getColumn(i).getModelIndex();
                int col2;

                if (col1 == classColumnIndex) continue;

                try {
                    double value1 = Double.parseDouble(tableModel.getValueAt(row, col1).toString());
                    double value2 = 0;

                    switch (mode) {
                        case "Direct":
                            value2 = value1;
                            break;

                        case "Forward Differences":
                            col2 = (i + 1) % numCols;
                            if (col2 == classColumnIndex) col2 = (col2 + 1) % numCols;
                            value2 = Double.parseDouble(tableModel.getValueAt(row, col2).toString());
                            value1 = value2 - value1;
                            break;

                        case "Backward Differences":
                            col2 = (i - 1 + numCols) % numCols;
                            if (col2 == classColumnIndex) col2 = (col2 - 1 + numCols) % numCols;
                            value2 = Double.parseDouble(tableModel.getValueAt(row, col2).toString());
                            value1 = value1 - value2;
                            break;
                    }

                    double cosValue = Math.cos(value1);
                    double sinValue = Math.sin(value1);
                    double tanValue = Math.tan(value1);

                    if (isInverse) {
                        cosValue = Math.acos(value1);
                        sinValue = Math.asin(value1);
                        tanValue = Math.atan(value1);
                    }

                    tableModel.setValueAt(cosValue, row, newColIndex++);
                    tableModel.setValueAt(sinValue, row, newColIndex++);
                    tableModel.setValueAt(tanValue, row, newColIndex++);
                } catch (NumberFormatException | NullPointerException e) {
                    tableModel.setValueAt("", row, newColIndex++);
                    tableModel.setValueAt("", row, newColIndex++);
                    tableModel.setValueAt("", row, newColIndex++);
                }
            }
        }
    }

    private void removeTrigonometricColumns() {
        if (originalData != null && originalColumnNames != null) {
            tableModel.setColumnCount(0);
            for (String colName : originalColumnNames) {
                tableModel.addColumn(colName);
            }
            tableModel.setRowCount(0);
            for (String[] rowData : originalData) {
                tableModel.addRow(rowData);
            }
        }
    }

    private int getClassColumnIndex() {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equalsIgnoreCase("class")) {
                return i;
            }
        }
        return -1;
    }
}
