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
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class CsvDataHandler {
    private List<String[]> originalData = new ArrayList<>();
    private List<String[]> normalizedData = new ArrayList<>();
    private boolean isNormalized = false;
    private String normalizationType = "minmax";
    private int classColumnIndex = -1; // Index of the detected class column
    private Map<Integer, double[]> originalValues = new HashMap<>();
    private Map<Integer, Double> originalMin = new HashMap<>();
    private Map<Integer, Double> originalMax = new HashMap<>();
    private Map<Integer, Double> originalMean = new HashMap<>();
    private Map<Integer, Double> originalStd = new HashMap<>();

    public void loadCsvData(String filePath, DefaultTableModel tableModel, JTextArea statsTextArea) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            // Clear existing data
            originalData.clear();
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (isFirstLine) {
                    tableModel.setColumnIdentifiers(values);
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].equalsIgnoreCase("class")) {
                            classColumnIndex = i;
                            break;
                        }
                    }
                    isFirstLine = false;
                } else {
                    originalData.add(values);
                    tableModel.addRow(values);
                }
            }
            updateStats(tableModel, statsTextArea);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error loading CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void normalizeOrDenormalizeData(JTable table, JTextArea statsTextArea) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();

        for (int col = 0; col < colCount; col++) {
            if (isColumnNumeric(col, model)) {
                double[] values = new double[rowCount];
                boolean hasNulls = false;

                // First pass - collect values and check for nulls
                for (int row = 0; row < rowCount; row++) {
                    Object value = model.getValueAt(row, col);
                    if (value == null || value.toString().trim().isEmpty()) {
                        hasNulls = true;
                        values[row] = 0.0; // Default value for nulls
                    } else {
                        try {
                            values[row] = Double.parseDouble(value.toString());
                        } catch (NumberFormatException e) {
                            values[row] = 0.0;
                        }
                    }
                }

                if (!hasNulls) {
                    // Calculate min and max
                    double min = Arrays.stream(values).min().orElse(0.0);
                    double max = Arrays.stream(values).max().orElse(1.0);
                    double mean = Arrays.stream(values).average().orElse(0.0);
                    double std = calculateStd(values, mean);

                    // Store original values for denormalization
                    originalValues.put(col, values.clone());
                    originalMin.put(col, min);
                    originalMax.put(col, max);
                    originalMean.put(col, mean);
                    originalStd.put(col, std);

                    // Normalize values
                    for (int row = 0; row < rowCount; row++) {
                        double normalizedValue;
                        if (normalizationType.equals("minmax")) {
                            normalizedValue = (values[row] - min) / (max - min);
                        } else { // zscore
                            normalizedValue = (values[row] - mean) / std;
                        }
                        model.setValueAt(String.format("%.4f", normalizedValue), row, col);
                    }
                }
            }
        }
        updateStats(model, statsTextArea);
    }

    private boolean isColumnNumeric(int col, DefaultTableModel model) {
        try {
            for (int row = 0; row < model.getRowCount(); row++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    Double.parseDouble(value.toString());
                    return true;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDataNormalized() {
        return isNormalized;
    }

    public void denormalizeData(JTable table, JTextArea statsTextArea) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();

        for (int col = 0; col < colCount; col++) {
            if (originalValues.containsKey(col)) {
                double[] values = originalValues.get(col);
                for (int row = 0; row < rowCount; row++) {
                    // Remove padding zeros when setting value back
                    String originalValue = String.valueOf(values[row]);
                    if (originalValue.endsWith(".0")) {
                        originalValue = originalValue.substring(0, originalValue.length() - 2);
                    }
                    model.setValueAt(originalValue, row, col);
                }
            }
        }
        updateStats(model, statsTextArea);
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

        StringBuilder stats = new StringBuilder();
        stats.append("Case Count: ").append(caseCount).append("\n");

        stats.append("Attribute Count: ").append(tableModel.getColumnCount() - 1).append("\n");

        int numColumns = tableModel.getColumnCount();
        int classColumnIndex = -1;

        // Find the class column index by checking various case-insensitive possibilities
        for (int col = 0; col < numColumns; col++) {
            String columnName = tableModel.getColumnName(col).toLowerCase();
            if (columnName.equals("class")) {
                classColumnIndex = col;
                break;
            }
        }

        Set<String> classSet = new HashSet<>();
        if (classColumnIndex != -1) {
            for (int row = 0; row < caseCount; row++) {
                Object classValue = tableModel.getValueAt(row, classColumnIndex);
                if (classValue != null) {
                    classSet.add(classValue.toString());
                }
            }
            int classCount = classSet.size();
            stats.append("Class Count: ").append(classCount).append("\n");
        } else {
            stats.append("Class column not found.\n");
        }

        for (int col = 0; col < numColumns; col++) {
            // Skip the "class" column
            if (col == classColumnIndex) {
                continue;
            }

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

    public void clearData() {
        originalData.clear();
        normalizedData.clear();
        isNormalized = false;
    }

    public void setNormalizationType(String type) {
        this.normalizationType = type;
    }

    private double calculateStd(double[] values, double mean) {
        double sumSquaredDiff = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.length);
    }

    public void toggleNormalization(JTable table, JTextArea statsTextArea) {
        if (isNormalized) {
            denormalizeData(table, statsTextArea);
        } else {
            normalizeOrDenormalizeData(table, statsTextArea);
        }
        isNormalized = !isNormalized;
    }
}
