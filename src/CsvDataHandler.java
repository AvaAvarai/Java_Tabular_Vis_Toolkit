package src;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvDataHandler {
    private List<String[]> originalData = new ArrayList<>();
    private List<String[]> normalizedData = new ArrayList<>();

    public void loadCsvData(String filePath, DefaultTableModel tableModel, JTextArea statsTextArea) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            originalData.clear();
            tableModel.setRowCount(0); // Clear existing data
            tableModel.setColumnCount(0); // Clear existing columns

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (isFirstLine) {
                    tableModel.setColumnIdentifiers(values);
                    isFirstLine = false;
                } else {
                    originalData.add(values);
                    tableModel.addRow(values);
                }
            }
            updateStats(tableModel, statsTextArea); // Update statistics after loading new data
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error loading CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void normalizeData() {
        if (originalData.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No data to normalize", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int numColumns = originalData.get(0).length;
        double[] minValues = new double[numColumns];
        double[] maxValues = new double[numColumns];
        boolean[] isNumerical = new boolean[numColumns];

        // Initialize min and max values, and check if columns are numerical
        for (int i = 0; i < numColumns; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
            isNumerical[i] = true;
        }

        // Calculate min and max for each column
        for (String[] row : originalData) {
            for (int j = 0; j < numColumns; j++) {
                try {
                    double value = Double.parseDouble(row[j]);
                    if (value < minValues[j]) minValues[j] = value;
                    if (value > maxValues[j]) maxValues[j] = value;
                } catch (NumberFormatException e) {
                    isNumerical[j] = false;
                }
            }
        }

        // Normalize the data
        normalizedData.clear(); // Clear previous normalized data
        for (String[] row : originalData) {
            String[] normalizedRow = new String[numColumns];
            for (int j = 0; j < numColumns; j++) {
                if (isNumerical[j]) {
                    double value = Double.parseDouble(row[j]);
                    double normalizedValue = (value - minValues[j]) / (maxValues[j] - minValues[j]);
                    normalizedRow[j] = String.valueOf(normalizedValue);
                } else {
                    normalizedRow[j] = row[j];
                }
            }
            normalizedData.add(normalizedRow);
        }
    }

    public void saveCsvData(String filePath, DefaultTableModel tableModel) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // Write column headers
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                bw.write(tableModel.getColumnName(col));
                if (col < tableModel.getColumnCount() - 1) {
                    bw.write(",");
                }
            }
            bw.newLine();

            // Write row data
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    bw.write(tableModel.getValueAt(row, col).toString());
                    if (col < tableModel.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
            JOptionPane.showMessageDialog(null, "CSV file saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isDataEmpty() {
        return originalData.isEmpty();
    }

    public List<String[]> getOriginalData() {
        return originalData;
    }

    public List<String[]> getNormalizedData() {
        return normalizedData;
    }

    public void updateStats(DefaultTableModel tableModel, JTextArea statsTextArea) {
        int caseCount = tableModel.getRowCount();
        Set<String> classSet = new HashSet<>();
        int classColumnIndex = tableModel.findColumn("class");  // TODO: make an option
        for (int row = 0; row < caseCount; row++) {
            if (classColumnIndex >= 0 && classColumnIndex < tableModel.getColumnCount()) {
                classSet.add(tableModel.getValueAt(row, classColumnIndex).toString());
            }
        }
        int classCount = classSet.size();

        StringBuilder stats = new StringBuilder();
        stats.append("Case Count: ").append(caseCount).append("\n");
        stats.append("Class Count: ").append(classCount).append("\n");

        int numColumns = tableModel.getColumnCount();
        for (int col = 0; col < numColumns; col++) {
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            boolean isNumerical = true;
            for (int row = 0; row < caseCount; row++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    if (value < minValue) minValue = value;
                    if (value > maxValue) maxValue = value;
                } catch (NumberFormatException e) {
                    isNumerical = false;
                    break;
                }
            }
            if (isNumerical) {
                stats.append(tableModel.getColumnName(col)).append(": Min=").append(minValue).append(", Max=").append(maxValue).append("\n");
            }
        }

        statsTextArea.setText(stats.toString());
    }
}
