package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CsvViewer extends JFrame {

    // Inner class to hold pure region data
    class PureRegion {
        String attributeName;
        double start;
        double end;
        String currentClass;
        int regionCount;
        double percentageOfClass;
        double percentageOfDataset;

        public PureRegion(String attributeName, double start, double end, String currentClass,
                          int regionCount, double percentageOfClass, double percentageOfDataset) {
            this.attributeName = attributeName;
            this.start = start;
            this.end = end;
            this.currentClass = currentClass;
            this.regionCount = regionCount;
            this.percentageOfClass = percentageOfClass;
            this.percentageOfDataset = percentageOfDataset;
        }
    }

    public JTable table;
    public ReorderableTableModel tableModel;
    public CsvDataHandler dataHandler;
    public boolean isNormalized = false;
    public boolean isHeatmapEnabled = false;
    public boolean isClassColorEnabled = false; // Flag for class column coloring
    public Color cellTextColor = Color.BLACK; // Default cell text color
    public JTextArea statsTextArea;
    public JButton toggleButton;
    public JButton toggleEasyCasesButton;
    public JButton toggleStatsButton;
    public JButton toggleStatsOnButton;
    public JButton toggleStatsOffButton;
    public Map<String, Color> classColors = new HashMap<>(); // Store class colors
    public Map<String, Shape> classShapes = new HashMap<>(); // Store class shapes
    public JLabel selectedRowsLabel; // Label to display the number of selected rows
    public JPanel bottomPanel; // Panel for the selected rows label
    public JPanel statsPanel; // Panel for stats visibility toggling
    public JScrollPane statsScrollPane; // Scroll pane for stats text area
    public JScrollPane tableScrollPane; // Scroll pane for the table
    public JSplitPane splitPane; // Split pane for table and stats
    private List<Integer> hiddenRows = new ArrayList<>(); // Store indices of hidden rows
    public JSlider thresholdSlider;

    public CsvViewer() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dataHandler = new CsvDataHandler();
        tableModel = new ReorderableTableModel();
        table = TableSetup.createTable(tableModel);
        table.addMouseListener(new TableMouseListener(this));
        
        // Add a KeyAdapter to handle Ctrl+C for copying cell content
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    if (table.getSelectedRows().length == 1) 
                        copySelectedCell();
                    else {
                        StringBuilder sb = new StringBuilder();
                        for (int row : table.getSelectedRows()) {
                            for (int col : table.getSelectedColumns()) {
                                sb.append(table.getValueAt(row, col)).append("\t");
                            }
                            sb.append("\n");
                        }
                        StringSelection stringSelection = new StringSelection(sb.toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                    }    
                }
            }
        });

        JPanel buttonPanel = ButtonPanel.createButtonPanel(this);
        add(buttonPanel, BorderLayout.NORTH);

        statsTextArea = UIHelper.createTextArea(3, 0);
        statsScrollPane = new JScrollPane(statsTextArea);

        tableScrollPane = new JScrollPane(table);

        selectedRowsLabel = new JLabel("Selected rows: 0");
        bottomPanel = new JPanel(new BorderLayout());

        // Initialize the slider
        thresholdSlider = new JSlider(0, 100, 10); // 0-100% with initial value 10%
        thresholdSlider.setMajorTickSpacing(20);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setToolTipText("Adjust threshold percentage");

        // Add listener to update the filtering logic
        thresholdSlider.addChangeListener(e -> {
            int thresholdValue = thresholdSlider.getValue();
            calculateAndDisplayPureRegions(thresholdValue);
        });

        // Adding slider to the bottom panel
        bottomPanel.add(thresholdSlider, BorderLayout.EAST);
        bottomPanel.add(selectedRowsLabel, BorderLayout.CENTER);

        statsPanel = new JPanel(new BorderLayout());
        statsPanel.add(statsScrollPane, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8); // 80% of space for table, 20% for stats initially
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH); // Always visible at the bottom

        generateClassShapes();
    }

    private void copySelectedCell() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row != -1 && col != -1) {
            Object cellValue = table.getValueAt(row, col);
            StringSelection stringSelection = new StringSelection(cellValue != null ? cellValue.toString() : "");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        }
    }

    public double calculateAndDisplayPureRegions(int thresholdPercentage) {
        int classColumnIndex = getClassColumnIndex(); // Find the class column index
        if (classColumnIndex == -1) {
            noDataLoadedError();
            return 0.0;
        }
    
        // Clear previous pure regions info
        String previousText = statsTextArea.getText();
        String newText = previousText.replaceAll("(?s)Single-Attribute Pure Regions:.*", "");
    
        List<PureRegion> pureRegions = new ArrayList<>();
        int numColumns = tableModel.getColumnCount();
        int totalRows = tableModel.getRowCount(); // Total number of rows in the dataset
    
        // Get the class counts for the entire dataset
        Map<String, Integer> classCounts = new HashMap<>();
        for (int row = 0; row < totalRows; row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
        }
    
        // Loop through columns to find pure regions
        for (int col = 0; col < numColumns; col++) {
            if (col == classColumnIndex) continue; // Skip the class column
    
            String attributeName = tableModel.getColumnName(col);
            List<Double> values = new ArrayList<>();
            Map<Double, List<Integer>> valueToRowIndicesMap = new HashMap<>();
    
            // Collect values and their corresponding row indices
            for (int row = 0; row < totalRows; row++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    values.add(value);
                    valueToRowIndicesMap.computeIfAbsent(value, k -> new ArrayList<>()).add(row);
                } catch (NumberFormatException e) {
                    // Skip non-numerical values
                }
            }
    
            if (values.isEmpty()) continue; // Skip if there are no numerical values
    
            Collections.sort(values);
    
            // Process windows by growing them to maximize their size while maintaining purity
            for (int start = 0; start < values.size(); start++) {
                String currentClass = null;
                Set<Integer> rowsInWindow = new HashSet<>();
                boolean isPure = true;
    
                for (int end = start + 1; end <= values.size(); end++) {
                    List<Integer> rowIndices = valueToRowIndicesMap.get(values.get(end - 1));
                    for (int rowIndex : rowIndices) {
                        String className = tableModel.getValueAt(rowIndex, classColumnIndex).toString();
                        if (currentClass == null) {
                            currentClass = className;
                        } else if (!currentClass.equals(className)) {
                            isPure = false;
                            break;
                        }
                        rowsInWindow.add(rowIndex);
                    }
    
                    if (!isPure) {
                        break;
                    }
    
                    if (rowsInWindow.size() > 0) {
                        int regionCount = rowsInWindow.size();
                        double percentageOfClass = (regionCount / (double) classCounts.get(currentClass)) * 100;
                        double percentageOfDataset = (regionCount / (double) totalRows) * 100;
                        double expandedEnd = values.get(end - 1);
    
                        PureRegion region = new PureRegion(
                                attributeName, values.get(start), expandedEnd,
                                currentClass, regionCount, percentageOfClass, percentageOfDataset
                        );
                        pureRegions.add(region);
                    }
                }
            }
        }
    
        // Keep only the largest regions by topological size and number of cases
        pureRegions = filterLargestSignificantRegions(pureRegions, thresholdPercentage);
    
        // Sort pure regions by the percentage of the dataset
        Collections.sort(pureRegions, Comparator.comparingDouble(region -> -region.percentageOfDataset));
    
        // Calculate remaining coverage after hiding pure regions
        Set<Integer> hiddenRows = new HashSet<>();
        for (PureRegion region : pureRegions) {
            for (int row = 0; row < totalRows; row++) {
                String attributeName = region.attributeName;
                int attributeColumnIndex = tableModel.findColumn(attributeName);

                if (attributeColumnIndex != -1) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(row, classColumnIndex).toString();

                        if (value >= region.start && value < region.end && className.equals(region.currentClass)) {
                            hiddenRows.add(row);
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numerical values
                    }
                }
            }
        }

        double remainingCoverage = ((totalRows - hiddenRows.size()) / (double) totalRows) * 100.0;

        // Display sorted results
        StringBuilder sb = new StringBuilder();
        sb.append("Single-Attribute Pure Regions:\n");
        for (PureRegion region : pureRegions) {
            sb.append(String.format("Attribute: %s, Pure Region: %.2f <= %s < %.2f, Class: %s, Count: %d (%.2f%% of class, %.2f%% of dataset)\n",
                    region.attributeName, region.start, region.attributeName, region.end,
                    region.currentClass, region.regionCount, region.percentageOfClass, region.percentageOfDataset));
        }

        statsTextArea.setText(newText + sb.toString());

        return remainingCoverage;
    }
    
    private List<PureRegion> filterLargestSignificantRegions(List<PureRegion> pureRegions, int thresholdPercentage) {
        List<PureRegion> filteredRegions = new ArrayList<>();
    
        // Sort by region size (number of cases), then by range size
        pureRegions.sort(Comparator.comparingInt((PureRegion region) -> region.regionCount).reversed()
                .thenComparingDouble(region -> region.end - region.start).reversed());
    
        for (PureRegion regionA : pureRegions) {
            boolean isContained = false;
            for (PureRegion regionB : filteredRegions) {
                // Check if regionA is entirely contained within regionB
                if (regionA.attributeName.equals(regionB.attributeName) &&
                    regionA.currentClass.equals(regionB.currentClass) &&
                    regionA.start >= regionB.start &&
                    regionA.end <= regionB.end &&
                    regionA.regionCount <= regionB.regionCount) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                filteredRegions.add(regionA);
            }
        }
    
        // Apply the threshold percentage to filter out regions
        double minCoverage = thresholdPercentage;
        filteredRegions.removeIf(region -> region.percentageOfClass < minCoverage && region.percentageOfDataset < minCoverage);
    
        return filteredRegions;
    }

    public boolean hasHiddenRows() {
        return !hiddenRows.isEmpty();
    }

    public void setSliderToMaxCoverage() {
        int bestThreshold = 0;
        double maxCoverage = 0.0;

        for (int threshold = 0; threshold <= 100; threshold += 1) {
            double coverage = calculateAndDisplayPureRegions(threshold);
            if (coverage > maxCoverage) {
                maxCoverage = coverage;
                bestThreshold = threshold;
            }
        }

        thresholdSlider.setValue(bestThreshold); // Set the slider to the best threshold
        calculateAndDisplayPureRegions(bestThreshold); // Recalculate and display with the best threshold
    }

    public void toggleEasyCases() {
        if (hiddenRows.isEmpty()) {
            hideEasyCases();
            // This line should be updating the toggleEasyCasesButton, not toggleButton
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/uneasy.png", 40, 40)); // Switch to uneasy icon
            toggleEasyCasesButton.setToolTipText("Show All Cases");
        } else {
            showEasyCases();
            // This line should be updating the toggleEasyCasesButton, not toggleButton
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/easy.png", 40, 40)); // Switch back to easy icon
            toggleEasyCasesButton.setToolTipText("Show Easy Cases");
        }
    }
    

    public void hideEasyCases() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            noDataLoadedError();
            return;
        }

        List<PureRegion> pureRegions = calculatePureRegions();
        Set<Integer> rowsToHide = new HashSet<>();

        for (PureRegion region : pureRegions) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String attributeName = region.attributeName;
                int attributeColumnIndex = tableModel.findColumn(attributeName);
                
                if (attributeColumnIndex != -1) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(row, classColumnIndex).toString();
                        
                        if (value >= region.start && value < region.end && className.equals(region.currentClass)) {
                            rowsToHide.add(row);
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numerical values
                    }
                }
            }
        }

        hiddenRows.clear();
        hiddenRows.addAll(rowsToHide);
        applyRowFilter();
        updateSelectedRowsLabel();
    }

    public void showEasyCases() {
        hiddenRows.clear();
        applyRowFilter();
        updateSelectedRowsLabel();
    }

    private void applyRowFilter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return !hiddenRows.contains(entry.getIdentifier());
            }
        });
        table.setRowSorter(sorter);
        updateSelectedRowsLabel(); // Update the label after applying the filter
    }    

    private List<PureRegion> calculatePureRegions() {
        List<PureRegion> pureRegions = new ArrayList<>();
        int classColumnIndex = getClassColumnIndex();
        int numColumns = tableModel.getColumnCount();
        int totalRows = tableModel.getRowCount();

        Map<String, Integer> classCounts = new HashMap<>();
        for (int row = 0; row < totalRows; row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
        }

        for (int col = 0; col < numColumns; col++) {
            if (col == classColumnIndex) continue;

            String attributeName = tableModel.getColumnName(col);
            List<Double> values = new ArrayList<>();
            Map<Double, List<Integer>> valueToRowIndicesMap = new HashMap<>();

            for (int row = 0; row < totalRows; row++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    values.add(value);
                    valueToRowIndicesMap.computeIfAbsent(value, k -> new ArrayList<>()).add(row);
                } catch (NumberFormatException e) {
                    // Skip non-numerical values
                }
            }

            Collections.sort(values);

            for (int start = 0; start < values.size(); start++) {
                for (int end = start + 1; end <= values.size(); end++) {
                    Set<Integer> rowsInWindow = new HashSet<>();
                    String currentClass = null;
                    boolean isPure = true;

                    for (int i = start; i < end; i++) {
                        List<Integer> rowIndices = valueToRowIndicesMap.get(values.get(i));
                        for (int rowIndex : rowIndices) {
                            String className = tableModel.getValueAt(rowIndex, classColumnIndex).toString();
                            if (currentClass == null) {
                                currentClass = className;
                            } else if (!currentClass.equals(className)) {
                                isPure = false;
                                break;
                            }
                            rowsInWindow.add(rowIndex);
                        }
                        if (!isPure) break;
                    }

                    if (isPure && !rowsInWindow.isEmpty()) {
                        int regionCount = rowsInWindow.size();
                        double percentageOfClass = (regionCount / (double) classCounts.get(currentClass)) * 100;
                        double percentageOfDataset = (regionCount / (double) totalRows) * 100;

                        PureRegion region = new PureRegion(
                                attributeName, values.get(start), values.get(end - 1),
                                currentClass, regionCount, percentageOfClass, percentageOfDataset
                        );
                        pureRegions.add(region);
                    }
                }
            }
        }

        return pureRegions;
    }

    public void toggleStatsVisibility(boolean hideStats) {
        if (hideStats) {
            splitPane.setBottomComponent(null);
            splitPane.setDividerSize(0); // Remove the divider when stats are hidden
            switchToggleStatsButton(toggleStatsOffButton);
        } else {
            splitPane.setBottomComponent(statsPanel);
            splitPane.setDividerSize(10); // Restore the divider size
            splitPane.setDividerLocation(0.8); // Adjust the split pane layout after toggling
            switchToggleStatsButton(toggleStatsOnButton);
        }
    }

    private void switchToggleStatsButton(JButton newButton) {
        JPanel buttonPanel = (JPanel) toggleStatsButton.getParent();
        buttonPanel.remove(toggleStatsButton);
        toggleStatsButton = newButton;
        buttonPanel.add(toggleStatsButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public void noDataLoadedError() {
        JOptionPane.showMessageDialog(this, "No data loaded", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void loadCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("datasets"));
        int result = fileChooser.showOpenDialog(this);
    
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            tableModel.setRowCount(0); // Clear existing table rows
            tableModel.setColumnCount(0); // Clear existing table columns
            classColors.clear(); // Clear existing class colors
            classShapes.clear(); // Clear existing class shapes
            dataHandler.clearData(); // Clear existing data in data handler
            dataHandler.loadCsvData(filePath, tableModel, statsTextArea);
            isNormalized = false;
            isHeatmapEnabled = false; // Reset heatmap state when new CSV is loaded
            isClassColorEnabled = false; // Reset class color state when new CSV is loaded
            updateTableData(dataHandler.getOriginalData());
            generateClassColors(); // Generate class colors based on the loaded data
            generateClassShapes(); // Generate class shapes based on the loaded data
            updateSelectedRowsLabel(); // Reset the selected rows label
    
            // Reset the Normalize button to its initial state
            toggleButton.setIcon(UIHelper.loadIcon("icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");

            // Automatically set the slider to the best threshold
            setSliderToMaxCoverage(); 

            // Scroll the stats window to the top on initial load
            statsTextArea.setCaretPosition(0);
        }
    }

    public void toggleDataView() {
        int currentCaretPosition = statsTextArea.getCaretPosition();
    
        if (isNormalized) {
            updateTableData(dataHandler.getOriginalData());
            isNormalized = false;
            toggleButton.setIcon(UIHelper.loadIcon("icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");
        } else {
            dataHandler.normalizeData();
            updateTableData(dataHandler.getNormalizedData());
            isNormalized = true;
            toggleButton.setIcon(UIHelper.loadIcon("icons/denormalize.png", 40, 40));
            toggleButton.setToolTipText("Default");
        }
    
        // Ensure the caret position is within valid bounds
        currentCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        statsTextArea.setCaretPosition(currentCaretPosition);
    }    

    public void updateTableData(List<String[]> data) {
        int currentCaretPosition = statsTextArea.getCaretPosition();
    
        tableModel.setRowCount(0); // Clear existing data
        for (String[] row : data) {
            tableModel.addRow(row);
        }
        if (isHeatmapEnabled || isClassColorEnabled) {
            applyCombinedRenderer();
        } else {
            applyDefaultRenderer();
        }
        dataHandler.updateStats(tableModel, statsTextArea);
        updateSelectedRowsLabel();
    
        // Ensure the caret position is within valid bounds
        currentCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        calculateAndDisplayPureRegions(thresholdSlider.getValue());
        statsTextArea.setCaretPosition(currentCaretPosition);
    }
        
    public void highlightBlanks() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value == null || value.toString().trim().isEmpty()) {
                    c.setBackground(Color.YELLOW);
                } else {
                    c.setBackground(Color.WHITE);
                }
                c.setForeground(cellTextColor);
                return c;
            }
        });
        table.repaint();
    }

    public void toggleHeatmap() {
        int currentCaretPosition = statsTextArea.getCaretPosition();
    
        isHeatmapEnabled = !isHeatmapEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);
    
        // Ensure the caret position is within valid bounds
        int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length() - 1);
        statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));  // Ensure it's not negative
    
        calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }    

    public void generateClassColors() {
        int classColumnIndex = getClassColumnIndex(); // Find the class column index
        if (classColumnIndex == -1) {
            return;
        }
        Map<String, Integer> classMap = new HashMap<>();
        int colorIndex = 0;
        List<String> classNames = new ArrayList<>();
        // First pass: check for predefined classes and assign their colors
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = (String) tableModel.getValueAt(row, classColumnIndex);
    
            if (className.equalsIgnoreCase("malignant") || className.equalsIgnoreCase("positive")) {
                classColors.put(className, Color.RED);
                classNames.add(className);
            } else if (className.equalsIgnoreCase("benign") || className.equalsIgnoreCase("negative")) {
                classColors.put(className, Color.GREEN);
                classNames.add(className);
            }
        }

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = (String) tableModel.getValueAt(row, classColumnIndex);
            if (!classMap.containsKey(className) && !classNames.contains(className)) {
                classMap.put(className, colorIndex++);
            }
        }

        for (Map.Entry<String, Integer> entry : classMap.entrySet()) {
            int value = entry.getValue();
            Color color = new Color(Color.HSBtoRGB(value / (float) classMap.size(), 1.0f, 1.0f));
            classColors.put(entry.getKey(), color);
        }
    }

    public void generateClassShapes() {
        Shape[] availableShapes = {
            new Ellipse2D.Double(-3, -3, 6, 6),
            new Rectangle2D.Double(-3, -3, 6, 6),
            new Polygon(new int[]{-3, 3, 0}, new int[]{-3, -3, 3}, 3),
            ShapeUtils.createStar(4, 6, 3),  // 4-point star
            ShapeUtils.createStar(5, 6, 3),  // 5-point star
            ShapeUtils.createStar(6, 6, 3),  // 6-point star
            ShapeUtils.createStar(7, 6, 3),  // 7-point star
            ShapeUtils.createStar(8, 6, 3)   // 8-point star
        };
    
        int i = 0;
        for (String className : classColors.keySet()) {
            classShapes.put(className, availableShapes[i % availableShapes.length]);
            i++;
        }
    }    

    public int getClassColumnIndex() {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equalsIgnoreCase("class")) {
                return i;
            }
        }
        return -1;
    }

    public void toggleClassColors() {
        int currentCaretPosition = statsTextArea.getCaretPosition();
    
        isClassColorEnabled = !isClassColorEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);
    
        // Ensure the caret position is within valid bounds
        int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));  // Ensure it's not negative
        calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }

    public void applyDefaultRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (hasFocus) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // Red border around clicked cell
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder()); // No border when not selected
                    }
                }
                return c;
            }
        });
        table.repaint();
    }    

    public void applyCombinedRenderer() {
        int numColumns = tableModel.getColumnCount();
        double[] minValues = new double[numColumns];
        double[] maxValues = new double[numColumns];
        boolean[] isNumerical = new boolean[numColumns];
        int classColumnIndex = getClassColumnIndex();
    
        for (int i = 0; i < numColumns; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
            isNumerical[i] = true;
        }
    
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < numColumns; col++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    if (value < minValues[col]) minValues[col] = value;
                    if (value > maxValues[col]) maxValues[col] = value;
                } catch (NumberFormatException e) {
                    isNumerical[col] = false;
                }
            }
        }
    
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelColumn = table.convertColumnIndexToModel(column);
    
                if (isClassColorEnabled && modelColumn == classColumnIndex) {
                    String className = (String) value;
                    if (classColors.containsKey(className)) {
                        c.setBackground(classColors.get(className));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else if (isHeatmapEnabled && value != null && !value.toString().trim().isEmpty() && isNumerical[modelColumn]) {
                    try {
                        double val = Double.parseDouble(value.toString());
                        double normalizedValue = (val - minValues[modelColumn]) / (maxValues[modelColumn] - minValues[modelColumn]);
                        Color color = getColorForValue(normalizedValue);
                        c.setBackground(color);
                    } catch (NumberFormatException e) {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
    
                if (hasFocus) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2)); // Red border around clicked cell
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder()); // No border when not selected
                    }
                }
    
                c.setForeground(cellTextColor);
                return c;
            }
    
            private Color getColorForValue(double value) {
                int red = (int) (255 * value);
                int blue = 255 - red;
                red = Math.max(0, Math.min(255, red));
                blue = Math.max(0, Math.min(255, blue));
                return new Color(red, 0, blue);
            }
        });
        table.repaint();
    }

    public void showFontSettingsDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JLabel colorLabel = new JLabel("Font Color:");
        panel.add(colorLabel);
        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Font Color", cellTextColor);
            if (newColor != null) {
                cellTextColor = newColor;
            }
        });
        panel.add(colorButton);

        JLabel sizeLabel = new JLabel("Font Size:");
        panel.add(sizeLabel);
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1));
        sizeSpinner.setValue(statsTextArea.getFont().getSize());
        panel.add(sizeSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel, "Font Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            int fontSize = (int) sizeSpinner.getValue();
            Font newFont = statsTextArea.getFont().deriveFont((float) fontSize);
            statsTextArea.setFont(newFont);

            table.setFont(newFont);
            table.getTableHeader().setFont(newFont);

            if (isHeatmapEnabled || isClassColorEnabled) {
                applyCombinedRenderer();
            } else {
                applyDefaultRenderer();
            }

            dataHandler.updateStats(tableModel, statsTextArea);
        }
    }

    public void showColorPickerDialog() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            return;
        }
    
        Set<String> uniqueClassNames = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            uniqueClassNames.add((String) tableModel.getValueAt(i, classColumnIndex));
        }
        String[] classNames = uniqueClassNames.toArray(new String[0]);
    
        // Temporary storage for changes made in the dialog
        Map<String, Color> tempClassColors = new HashMap<>(classColors);
        Map<String, Shape> tempClassShapes = new HashMap<>(classShapes);
    
        JPanel mainPanel = new JPanel(new BorderLayout());
    
        JComboBox<String> classComboBox = new JComboBox<>(classNames);
    
        classComboBox.setRenderer(new ListCellRenderer<String>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout());
                JLabel label = new JLabel(value);
                JLabel colorSwatch = new JLabel();
                colorSwatch.setOpaque(true);
                colorSwatch.setPreferredSize(new Dimension(30, 30));
                colorSwatch.setBackground(tempClassColors.getOrDefault(value, Color.WHITE));
    
                JLabel shapeSwatch = new JLabel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setColor(Color.BLACK);
                        Shape shape = tempClassShapes.getOrDefault(value, new Ellipse2D.Double(-6, -6, 12, 12));
                        AffineTransform at = AffineTransform.getTranslateInstance(15, 15);
                        at.scale(2, 2);  // Scale the shape to make it larger
                        g2.fill(at.createTransformedShape(shape));
                    }
                };
                shapeSwatch.setPreferredSize(new Dimension(30, 30));
    
                panel.add(colorSwatch, BorderLayout.WEST);
                panel.add(shapeSwatch, BorderLayout.CENTER);
                panel.add(label, BorderLayout.EAST);
    
                if (isSelected) {
                    panel.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    panel.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                return panel;
            }
        });
    
        mainPanel.add(classComboBox, BorderLayout.NORTH);
    
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Current Class Colors & Shapes"));
    
        for (String className : classNames) {
            JPanel colorLabelPanel = new JPanel(new BorderLayout());
    
            JLabel colorBox = new JLabel();
            colorBox.setOpaque(true);
            colorBox.setBackground(tempClassColors.getOrDefault(className, Color.WHITE));
            colorBox.setPreferredSize(new Dimension(20, 20));
    
            JLabel shapeBox = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    g2.fill(tempClassShapes.getOrDefault(className, new Ellipse2D.Double(-3, -3, 6, 6)));
                }
            };
            shapeBox.setPreferredSize(new Dimension(20, 20));
    
            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
    
            colorLabelPanel.add(colorBox, BorderLayout.WEST);
            colorLabelPanel.add(shapeBox, BorderLayout.CENTER);
            colorLabelPanel.add(label, BorderLayout.EAST);
    
            legendPanel.add(colorLabelPanel);
        }
    
        mainPanel.add(legendPanel, BorderLayout.CENTER);
    
        // Shape picker panel
        JPanel shapePickerPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        shapePickerPanel.setBorder(BorderFactory.createTitledBorder("Pick Shape"));
    
        // Add shape options to the picker
        Shape[] availableShapes = {
            new Ellipse2D.Double(-5, -5, 10, 10),
            new Rectangle2D.Double(-5, -5, 10, 10),
            new Polygon(new int[]{-5, 5, 0}, new int[]{-5, -5, 5}, 3),
            ShapeUtils.createStar(4, 10, 5),  // 4-point star
            ShapeUtils.createStar(5, 10, 5),  // 5-point star
            ShapeUtils.createStar(6, 10, 5),  // 6-point star
            ShapeUtils.createStar(7, 10, 5),  // 7-point star
            ShapeUtils.createStar(8, 10, 5)   // 8-point star
        };
    
        ButtonGroup shapeButtonGroup = new ButtonGroup();
        JRadioButton[] shapeButtons = new JRadioButton[availableShapes.length];
    
        // Method to update the shape selection based on the current class
        Runnable updateShapeSelection = () -> {
            String selectedClass = (String) classComboBox.getSelectedItem();
            Shape currentShape = tempClassShapes.get(selectedClass);
            for (int i = 0; i < availableShapes.length; i++) {
                shapeButtons[i].setSelected(currentShape.getClass().equals(availableShapes[i].getClass()));
            }
        };
    
        for (int i = 0; i < availableShapes.length; i++) {
            Shape shape = availableShapes[i];
            shapeButtons[i] = new JRadioButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    g2.translate(20, 15);
                    g2.scale(1.5, 1.5);  // Scale the shape to make it larger
                    g2.fill(shape);
                    g2.translate(-15, -15);
                }
            };
            shapeButtons[i].setPreferredSize(new Dimension(40, 40));  // Increase the size of the radio button
            shapeButtonGroup.add(shapeButtons[i]);
            shapePickerPanel.add(shapeButtons[i]);
    
            shapeButtons[i].addActionListener(e -> {
                String selectedClass = (String) classComboBox.getSelectedItem();
                tempClassShapes.put(selectedClass, shape);
            });
        }
    
        mainPanel.add(shapePickerPanel, BorderLayout.CENTER);
    
        // Button panel with the new "Set Color" button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton setColorButton = new JButton("Set Color");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
    
        setColorButton.addActionListener(e -> {
            String selectedClass = (String) classComboBox.getSelectedItem();
            Color color = JColorChooser.showDialog(this, "Choose color for " + selectedClass, tempClassColors.getOrDefault(selectedClass, Color.WHITE));
            if (color != null) {
                tempClassColors.put(selectedClass, color);
                classComboBox.repaint();
            }
        });
    
        okButton.addActionListener(e -> {
            classColors.putAll(tempClassColors);
            classShapes.putAll(tempClassShapes);
            // Close the color picker dialog
            Window window = SwingUtilities.getWindowAncestor(okButton);
            if (window != null) {
                window.dispose();
            }
        });
    
        cancelButton.addActionListener(e -> {
            // Close the color picker dialog without saving
            Window window = SwingUtilities.getWindowAncestor(cancelButton);
            if (window != null) {
                window.dispose();
            }
        });
    
        buttonPanel.add(setColorButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
    
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    
        classComboBox.addActionListener(e -> {
            updateShapeSelection.run();
        });
        // Initial call to set the correct shape selection
        updateShapeSelection.run();
    
        // Show the dialog
        JDialog dialog = new JDialog(this, "Select Class, Color & Shape", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setSize(dialog.getWidth(), dialog.getHeight() + 75); // Increase the height of the dialog
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void insertRow() {
        int currentCaretPosition = statsTextArea.getCaretPosition();
    
        int numColumns = tableModel.getColumnCount();
        String[] emptyRow = new String[numColumns];
    
        // Initialize the empty row with empty strings
        for (int i = 0; i < numColumns; i++) {
            emptyRow[i] = "";
        }
    
        tableModel.addRow(emptyRow);
        dataHandler.updateStats(tableModel, statsTextArea);
    
        statsTextArea.setCaretPosition(currentCaretPosition);
    }    

    public void deleteRow() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            int currentCaretPosition = statsTextArea.getCaretPosition();
    
            // Convert selected rows to model indices and sort them in descending order
            List<Integer> rowsToDelete = new ArrayList<>();
            for (int row : selectedRows) {
                rowsToDelete.add(table.convertRowIndexToModel(row));
            }
            rowsToDelete.sort(Collections.reverseOrder());
    
            // Delete rows in descending order to avoid issues with shifting indices
            for (int rowIndex : rowsToDelete) {
                tableModel.removeRow(rowIndex);
            }
    
            dataHandler.updateStats(tableModel, statsTextArea);
            updateSelectedRowsLabel();
    
            // Ensure the caret position is within valid bounds
            int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
            statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));  // Ensure it's not negative
        } else {
            JOptionPane.showMessageDialog(this, "Please select at least one row to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }    

    public void cloneSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int currentCaretPosition = statsTextArea.getCaretPosition();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            Object[] rowData = new Object[model.getColumnCount()];
            for (int col = 0; col < model.getColumnCount(); col++) {
                rowData[col] = model.getValueAt(selectedRow, col);
            }
            model.insertRow(selectedRow + 1, rowData);
            dataHandler.updateStats(tableModel, statsTextArea);

            statsTextArea.setCaretPosition(currentCaretPosition);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to clone.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("datasets"));
        fileChooser.setDialogTitle("Save CSV File");
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                // Write header row
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    writer.write(tableModel.getColumnName(col));
                    if (col < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

                // Write data rows, excluding hidden rows
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    if (!hiddenRows.contains(row)) {
                        for (int col = 0; col < tableModel.getColumnCount(); col++) {
                            Object value = tableModel.getValueAt(row, col);
                            writer.write(value != null ? value.toString() : "");
                            if (col < tableModel.getColumnCount() - 1) {
                                writer.write(",");
                            }
                        }
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void showParallelCoordinatesPlot() {
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        String[] columnNames = new String[columnCount];
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = columnModel.getColumn(i).getHeaderValue().toString();
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        // Get the current data from the table model, excluding hidden rows
        List<String[]> data = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (!hiddenRows.contains(row)) {  // Exclude hidden rows
                String[] rowData = new String[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    Object value = tableModel.getValueAt(row, col);
                    rowData[col] = value != null ? value.toString() : "";
                }
                data.add(rowData);
            }
        }
    
        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);  // Exclude hidden rows from selectedRows
    
        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, classColors, getClassColumnIndex(), columnOrder, selectedRows);
        plot.setVisible(true);
    }    
    
    public void showShiftedPairedCoordinates() {
        List<List<Double>> data = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();
    
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (!hiddenRows.contains(row)) {  // Exclude hidden rows
                    try {
                        columnData.add(Double.parseDouble(tableModel.getValueAt(row, columnOrder[col]).toString()));
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            if (isNumeric) {
                data.add(columnData);
                attributeNames.add(tableModel.getColumnName(columnOrder[col]));
            }
        }
    
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (!hiddenRows.contains(row)) {  // Exclude hidden rows
                classLabels.add((String) tableModel.getValueAt(row, getClassColumnIndex()));
            }
        }
    
        int numPlots = (attributeNames.size() + 1) / 2;
        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);  // Exclude hidden rows from selectedRows
    
        ShiftedPairedCoordinates shiftedPairedCoordinates = new ShiftedPairedCoordinates(data, attributeNames, classColors, classShapes, classLabels, numPlots, selectedRows);
        shiftedPairedCoordinates.setVisible(true);
    }    

    public void showStaticCircularCoordinatesPlot() {
        List<List<Double>> data = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();
    
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        // Loop through columns to get numeric data
        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (!hiddenRows.contains(row)) {  // Only include visible rows
                    try {
                        columnData.add(Double.parseDouble(tableModel.getValueAt(row, columnOrder[col]).toString()));
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            if (isNumeric) {
                data.add(columnData);
                attributeNames.add(tableModel.getColumnName(columnOrder[col]));
            }
        }
    
        // Get class labels, excluding hidden rows
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (!hiddenRows.contains(row)) {  // Only include visible rows
                classLabels.add((String) tableModel.getValueAt(row, getClassColumnIndex()));
            }
        }
    
        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);  // Exclude hidden rows from selectedRows
    
        // Create and show the StaticCircularCoordinates plot
        StaticCircularCoordinatesPlot plot = new StaticCircularCoordinatesPlot(data, attributeNames, classColors, classShapes, classLabels, selectedRows);
        plot.setVisible(true);
    }    

    public void showRuleTesterDialog() {
        RuleTesterDialog ruleTesterDialog = new RuleTesterDialog(this, tableModel);
        ruleTesterDialog.setVisible(true);
    }

    public void updateSelectedRowsLabel() {
        int selectedRowCount = table.getSelectedRowCount();
        int totalVisibleRowCount = table.getRowCount(); // This gives the count of visible rows (excluding hidden rows)
        selectedRowsLabel.setText("Selected rows: " + selectedRowCount + " / Total visible cases: " + totalVisibleRowCount + " / Total cases: " + tableModel.getRowCount());
    }    

    public List<Integer> getSelectedRowsIndices() {
        int[] selectedRows = table.getSelectedRows();
        List<Integer> selectedIndices = new ArrayList<>();
        for (int row : selectedRows) {
            selectedIndices.add(table.convertRowIndexToModel(row));
        }
        return selectedIndices;
    }    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CsvViewer viewer = new CsvViewer();
            viewer.setVisible(true);
        });
    }
}
