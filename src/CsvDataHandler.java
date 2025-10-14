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
import utils.DataTypeDetector;
import utils.ColumnDataTypeInfo;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.util.Collections;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;

public class CsvDataHandler {
    private List<String[]> originalData = new ArrayList<>();
    private List<String[]> normalizedData = new ArrayList<>();
    private boolean isNormalized = false;
    private String normalizationType = "minmax";
    private Map<Integer, double[]> originalValues = new HashMap<>();
    private Map<Integer, Double> originalMin = new HashMap<>();
    private Map<Integer, Double> originalMax = new HashMap<>();
    private Map<Integer, Double> originalMean = new HashMap<>();
    private Map<Integer, Double> originalStd = new HashMap<>();
    private Set<String> selectedClasses = new HashSet<>();
    private Map<String, Set<String>> classGroups;
    private Map<Integer, ColumnDataTypeInfo> columnDataTypes = new HashMap<>();

    private static class ClassGrouping {
        JCheckBox checkBox;
        JComboBox<String> groupCombo;
        String className;
        JLabel currentGroupLabel;

        ClassGrouping(String className, boolean selected) {
            this.className = className;
            this.checkBox = new JCheckBox(className, selected);
            this.groupCombo = new JComboBox<>();
            this.currentGroupLabel = new JLabel(className);
            checkBox.setFont(checkBox.getFont().deriveFont(11f));
            groupCombo.setFont(groupCombo.getFont().deriveFont(11f));
            currentGroupLabel.setFont(currentGroupLabel.getFont().deriveFont(11f));
        }
    }

    private boolean showClassSelectionDialog(List<String> classes) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Class Grouping Selection");
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(5, 5));

        // Create panel with grid (3 columns: checkbox, combo box, and current group label)
        JPanel selectionPanel = new JPanel(new GridLayout(0, 3, 5, 2));
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        List<ClassGrouping> classGroupings = new ArrayList<>();
        Map<String, Set<String>> currentGroups = new HashMap<>();
        
        // Initially, each class gets its own group
        for (String className : classes) {
            currentGroups.put(className, new HashSet<>(Arrays.asList(className)));
        }

        // Create class groupings
        for (String className : classes) {
            ClassGrouping grouping = new ClassGrouping(className, true);
            classGroupings.add(grouping);
            
            // Add all classes as potential groups
            grouping.groupCombo.addItem(className); // Always add own class first
            classes.stream()
                  .filter(c -> !c.equals(className))
                  .forEach(c -> grouping.groupCombo.addItem(c));
            
            // Set default selection to the class's own name
            grouping.groupCombo.setSelectedItem(className);
            
            // Add item listeners
            grouping.checkBox.addItemListener(e -> {
                grouping.groupCombo.setEnabled(grouping.checkBox.isSelected());
                updateGroupings(classGroupings, currentGroups);
            });

            grouping.groupCombo.addActionListener(e -> {
                updateGroupings(classGroupings, currentGroups);
            });

            selectionPanel.add(grouping.checkBox);
            selectionPanel.add(grouping.groupCombo);
            selectionPanel.add(grouping.currentGroupLabel);
        }

        JScrollPane scrollPane = new JScrollPane(selectionPanel);
        scrollPane.setPreferredSize(new Dimension(500, Math.min(400, 50 * classes.size())));
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Select All/None buttons
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> {
            classGroupings.forEach(g -> g.checkBox.setSelected(true));
        });
        
        JButton selectNoneButton = new JButton("Select None");
        selectNoneButton.addActionListener(e -> {
            classGroupings.forEach(g -> g.checkBox.setSelected(false));
        });

        topPanel.add(selectAllButton);
        topPanel.add(selectNoneButton);
        dialog.add(topPanel, BorderLayout.NORTH);

        // Bottom panel with separator and buttons
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            selectedClasses.clear();
            Map<String, Set<String>> groupedClasses = new HashMap<>();
            
            // Collect selected classes and their groups
            for (ClassGrouping grouping : classGroupings) {
                if (grouping.checkBox.isSelected()) {
                    String group = (String) grouping.groupCombo.getSelectedItem();
                    groupedClasses.computeIfAbsent(group, k -> new HashSet<>()).add(grouping.className);
                }
            }

            // Create final class groups with concatenated names for combined groups
            Map<String, Set<String>> finalGroups = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : groupedClasses.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    if (entry.getValue().size() == 1) {
                        // Single class group - use original name
                        finalGroups.put(entry.getKey(), entry.getValue());
                    } else {
                        // Multiple classes - concatenate names
                        String newGroupName = String.join("+", entry.getValue());
                        finalGroups.put(newGroupName, entry.getValue());
                    }
                }
            }

            // Validate that we have at least one group with selected classes
            if (finalGroups.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select at least one class.",
                    "Invalid Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Store the selected classes and their groupings
            selectedClasses.clear();
            classGroups = finalGroups;
            finalGroups.values().forEach(selectedClasses::addAll);
            
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedClasses.clear();
            classGroups = null;
            dialog.dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        bottomPanel.add(separator);
        bottomPanel.add(buttonPanel);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return !selectedClasses.isEmpty();
    }

    private void updateGroupings(List<ClassGrouping> groupings, Map<String, Set<String>> currentGroups) {
        // Clear current groups
        currentGroups.clear();
        
        // Create temporary mapping of selected groupings
        Map<String, Set<String>> tempGroups = new HashMap<>();
        
        // First pass: collect all groupings
        for (ClassGrouping grouping : groupings) {
            if (grouping.checkBox.isSelected()) {
                String targetGroup = (String) grouping.groupCombo.getSelectedItem();
                tempGroups.computeIfAbsent(targetGroup, k -> new HashSet<>()).add(grouping.className);
            } else {
                // If not selected, class stays in its own group
                tempGroups.put(grouping.className, new HashSet<>(Arrays.asList(grouping.className)));
            }
        }
        
        // Second pass: create final groups and update labels
        for (Map.Entry<String, Set<String>> entry : tempGroups.entrySet()) {
            String groupName = entry.getValue().size() > 1 ? 
                String.join("+", entry.getValue()) : 
                entry.getKey();
            currentGroups.put(groupName, entry.getValue());
            
            // Update labels for all classes in this group
            for (ClassGrouping grouping : groupings) {
                if (entry.getValue().contains(grouping.className)) {
                    grouping.currentGroupLabel.setText(groupName);
                }
            }
        }
    }

    private void updateAvailableGroups(List<ClassGrouping> groupings, Set<String> availableGroups) {
        // Update combo boxes for selected checkboxes
        groupings.forEach(g -> {
            if (g.checkBox.isSelected()) {
                String selected = (String) g.groupCombo.getSelectedItem();
                g.groupCombo.removeAllItems();
                availableGroups.forEach(group -> g.groupCombo.addItem(group));
                g.groupCombo.setSelectedItem(selected);
            }
        });
    }

    public void loadCsvData(String filePath, DefaultTableModel tableModel, JTextArea statsTextArea) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            List<String> headers = null;
            List<String[]> tempData = new ArrayList<>();
            Set<String> uniqueClasses = new HashSet<>();
            int classColIndex = -1;

            // First pass to collect classes and headers
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (isFirstLine) {
                    headers = Arrays.asList(values);
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].equalsIgnoreCase("class") || values[i].equalsIgnoreCase("label")) {
                            classColIndex = i;
                            break;
                        }
                    }
                    isFirstLine = false;
                } else if (classColIndex != -1) {
                    uniqueClasses.add(values[classColIndex]);
                    tempData.add(values);
                }
            }

            // Show class selection dialog if we found classes
            if (!uniqueClasses.isEmpty()) {
                List<String> sortedClasses = new ArrayList<>(uniqueClasses);
                Collections.sort(sortedClasses);
                if (!showClassSelectionDialog(sortedClasses)) {
                    return; // User cancelled or no classes selected
                }
            }

            // Clear existing data
            originalData.clear();
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);

            // Set headers
            if (headers != null) {
                tableModel.setColumnIdentifiers(headers.toArray());
            }

            // Add filtered data with transformed class labels
            for (String[] values : tempData) {
                if (classColIndex != -1) {
                    String originalClass = values[classColIndex];
                    if (selectedClasses.contains(originalClass)) {
                        // Find the group this class belongs to
                        String newClassName = classGroups.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(originalClass))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(originalClass);
                        
                        // Create new row with transformed class label
                        String[] newValues = values.clone();
                        newValues[classColIndex] = newClassName;
                        
                        originalData.add(newValues);
                        tableModel.addRow(newValues);
                    }
                } else {
                    originalData.add(values);
                    tableModel.addRow(values);
                }
            }

            // Detect data types and transform data
            detectAndTransformDataTypes(tableModel, classColIndex);

            updateStats(tableModel, statsTextArea);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error loading CSV file: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void normalizeOrDenormalizeData(JTable table, JTextArea statsTextArea) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowCount = model.getRowCount();
        int colCount = model.getColumnCount();
        
        // Find the class column index
        int classColumnIndex = -1;
        for (int col = 0; col < colCount; col++) {
            if (model.getColumnName(col).equalsIgnoreCase("class") || model.getColumnName(col).equalsIgnoreCase("label")) {
                classColumnIndex = col;
                break;
            }
        }

        for (int col = 0; col < colCount; col++) {
            // Skip the class column
            if (col == classColumnIndex) {
                continue;
            }
            
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
                        // Ensure NaN is not returned
                        if (Double.isNaN(normalizedValue)) {
                            normalizedValue = 0.0; // Default value for NaN
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
            if (columnName.equals("class") || columnName.equals("label")) {
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
        columnDataTypes.clear();
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

    public Map<String, Set<String>> getClassGroups() {
        return classGroups;
    }
    
    /**
     * Detects data types for each column and transforms the data accordingly.
     * @param tableModel The table model containing the data
     * @param classColIndex Index of the class column (to skip it)
     */
    private void detectAndTransformDataTypes(DefaultTableModel tableModel, int classColIndex) {
        int rowCount = tableModel.getRowCount();
        int colCount = tableModel.getColumnCount();
        
        // Clear previous column data types
        columnDataTypes.clear();
        
        for (int col = 0; col < colCount; col++) {
            String columnName = tableModel.getColumnName(col);
            
            // Handle the class/label column specially
            if (col == classColIndex) {
                columnDataTypes.put(col, new ColumnDataTypeInfo(DataTypeDetector.DataType.LABEL, columnName));
                continue;
            }
            
            // Collect all values for this column
            String[] columnValues = new String[rowCount];
            for (int row = 0; row < rowCount; row++) {
                Object value = tableModel.getValueAt(row, col);
                columnValues[row] = value != null ? value.toString() : "";
            }
            
            // Detect data type
            DataTypeDetector.DataType detectedType = DataTypeDetector.detectDataType(columnValues);
            
            // Transform data based on type
            switch (detectedType) {
                case CATEGORICAL -> {
                    // Create mapping and transform to integers
                    Map<String, Integer> mapping = DataTypeDetector.createCategoricalMapping(columnValues);
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName, mapping));
                    
                    // Transform values to integers
                    for (int row = 0; row < rowCount; row++) {
                        String value = columnValues[row];
                        if (mapping.containsKey(value)) {
                            tableModel.setValueAt(mapping.get(value), row, col);
                        }
                    }
                }
                case BINARY -> {
                    // Transform to 0/1
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName));
                    for (int row = 0; row < rowCount; row++) {
                        String value = columnValues[row];
                        int binaryValue = DataTypeDetector.convertBinaryToInt(value);
                        tableModel.setValueAt(binaryValue, row, col);
                    }
                }
                case TIMESTAMP -> {
                    // Transform to milliseconds since epoch
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName));
                    for (int row = 0; row < rowCount; row++) {
                        String value = columnValues[row];
                        long timestamp = DataTypeDetector.convertTimestampToLong(value);
                        tableModel.setValueAt(timestamp, row, col);
                    }
                }
                case NOMINAL -> {
                    // Store as hash values for visualization
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName));
                    for (int row = 0; row < rowCount; row++) {
                        String value = columnValues[row];
                        if (!value.isEmpty()) {
                            int hashValue = Math.abs(value.hashCode()) % 1000;
                            tableModel.setValueAt(hashValue, row, col);
                        }
                    }
                }
                case NUMERICAL -> {
                    // Keep as is, but store type info
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName));
                }
                default -> {
                    // Unknown type - keep as is
                    columnDataTypes.put(col, new ColumnDataTypeInfo(detectedType, columnName));
                }
            }
        }
    }
    
    /**
     * Gets the data type information for a column
     * @param columnIndex Index of the column
     * @return ColumnDataTypeInfo object, or null if not found
     */
    public ColumnDataTypeInfo getColumnDataType(int columnIndex) {
        return columnDataTypes.get(columnIndex);
    }
    
    /**
     * Gets all column data type information
     * @return Map of column index to ColumnDataTypeInfo
     */
    public Map<Integer, ColumnDataTypeInfo> getAllColumnDataTypes() {
        return new HashMap<>(columnDataTypes);
    }
}
