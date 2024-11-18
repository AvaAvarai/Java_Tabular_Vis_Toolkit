package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvDataHandler {
    private List<String[]> originalData = new ArrayList<>();
    private List<String[]> normalizedData = new ArrayList<>();
    private boolean isNormalized = false;
    private String normalizationType = "minmax"; // Default to min-max

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

    public void normalizeOrDenormalizeData(JTable table, JTextArea statsTextArea) {
        if (!isNormalized) {
            normalizeData(table, statsTextArea);
        } else {
            denormalizeData(table, statsTextArea);
        }
    }
    
    public boolean isDataNormalized() {
        return isNormalized;
    }

    private void normalizeData(JTable table, JTextArea statsTextArea) {
        if (originalData.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No data to normalize", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingWorker<List<String[]>, Void> worker = new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                int numColumns = originalData.get(0).length;
                double[] minValues = new double[numColumns];
                double[] maxValues = new double[numColumns];
                double[] means = new double[numColumns];
                double[] stdDevs = new double[numColumns];
                boolean[] isNumerical = new boolean[numColumns];
                int[] counts = new int[numColumns];

                // Initialize arrays
                for (int i = 0; i < numColumns; i++) {
                    minValues[i] = Double.MAX_VALUE;
                    maxValues[i] = Double.MIN_VALUE;
                    means[i] = 0.0;
                    stdDevs[i] = 0.0;
                    isNumerical[i] = true;
                    counts[i] = 0;
                }

                // First pass - calculate min, max and means
                for (String[] row : originalData) {
                    for (int j = 0; j < numColumns; j++) {
                        try {
                            if (row[j] != null && !row[j].trim().isEmpty()) {
                                double value = Double.parseDouble(row[j]);
                                minValues[j] = Math.min(minValues[j], value);
                                maxValues[j] = Math.max(maxValues[j], value);
                                means[j] += value;
                                counts[j]++;
                            }
                        } catch (NumberFormatException e) {
                            isNumerical[j] = false;
                        }
                    }
                }

                // Calculate means
                for (int j = 0; j < numColumns; j++) {
                    if (isNumerical[j] && counts[j] > 0) {
                        means[j] /= counts[j];
                    }
                }

                // Second pass - calculate standard deviations for z-score
                if (normalizationType.equals("zscore")) {
                    for (String[] row : originalData) {
                        for (int j = 0; j < numColumns; j++) {
                            if (isNumerical[j]) {
                                try {
                                    if (row[j] != null && !row[j].trim().isEmpty()) {
                                        double value = Double.parseDouble(row[j]);
                                        stdDevs[j] += Math.pow(value - means[j], 2);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    // Finalize standard deviations
                    for (int j = 0; j < numColumns; j++) {
                        if (isNumerical[j] && counts[j] > 1) {
                            stdDevs[j] = Math.sqrt(stdDevs[j] / (counts[j] - 1));
                        }
                    }
                }

                // Create normalized data
                List<String[]> normalizedData = new ArrayList<>();
                DecimalFormat df = new DecimalFormat("#.####");
                
                for (String[] row : originalData) {
                    String[] normalizedRow = new String[numColumns];
                    for (int j = 0; j < numColumns; j++) {
                        if (isNumerical[j]) {
                            try {
                                if (row[j] != null && !row[j].trim().isEmpty()) {
                                    double value = Double.parseDouble(row[j]);
                                    double normalizedValue;
                                    if (normalizationType.equals("minmax")) {
                                        double range = maxValues[j] - minValues[j];
                                        normalizedValue = range != 0 ? (value - minValues[j]) / range : 0;
                                    } else { // zscore
                                        normalizedValue = stdDevs[j] != 0 ? (value - means[j]) / stdDevs[j] : 0;
                                    }
                                    normalizedRow[j] = df.format(normalizedValue);
                                } else {
                                    normalizedRow[j] = row[j];
                                }
                            } catch (NumberFormatException e) {
                                normalizedRow[j] = row[j];
                            }
                        } else {
                            normalizedRow[j] = row[j];
                        }
                    }
                    normalizedData.add(normalizedRow);
                }
                return normalizedData;
            }

            @Override
            protected void done() {
                try {
                    normalizedData = get();
                    isNormalized = true;
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    model.setRowCount(0);
                    for (String[] row : normalizedData) {
                        model.addRow(row);
                    }
                    updateStats((DefaultTableModel) table.getModel(), statsTextArea);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error during normalization: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
    
    private void denormalizeData(JTable table, JTextArea statsTextArea) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                return null; // Nothing to compute in the background
            }

            @Override
            protected void done() {
                isNormalized = false;
                updateTableWithOriginalData(table);
                DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
                updateStats(tableModel, statsTextArea);
            }
        };

        worker.execute();
    }

    private void updateTableWithNormalizedData(JTable table) {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setRowCount(0); // Clear existing data in the table

        // Populate the table with normalized data
        for (String[] row : normalizedData) {
            tableModel.addRow(row);
        }

        table.repaint(); // Refresh the table display
    }

    private void updateTableWithOriginalData(JTable table) {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setRowCount(0); // Clear existing data in the table

        // Restore the original data
        for (String[] row : originalData) {
            tableModel.addRow(row);
        }

        table.repaint(); // Refresh the table display
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
}
