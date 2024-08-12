package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CsvViewer extends JFrame {
    public JTable table;
    public ReorderableTableModel tableModel;
    public CsvDataHandler dataHandler;
    public boolean isNormalized = false;
    public boolean isHeatmapEnabled = false;
    public boolean isClassColorEnabled = false;
    public boolean areDifferenceColumnsVisible = false;
    public Color cellTextColor = Color.BLACK;
    public JTextArea statsTextArea;
    public JButton toggleButton;
    public JButton toggleEasyCasesButton;
    public JButton toggleStatsButton;
    public JButton toggleStatsOnButton;
    public JButton toggleStatsOffButton;
    public JButton toggleTrigonometricButton;
    public Map<String, Color> classColors = new HashMap<>();
    public Map<String, Shape> classShapes = new HashMap<>();
    public JLabel selectedRowsLabel;
    public JPanel bottomPanel;
    public JPanel statsPanel;
    public JScrollPane statsScrollPane;
    public JScrollPane tableScrollPane;
    public JSplitPane splitPane;
    private List<Integer> hiddenRows = new ArrayList<>();
    public JSlider thresholdSlider;
    public JLabel thresholdLabel;
    private List<String[]> originalData;
    private List<String> originalColumnNames;

    public CsvViewer() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dataHandler = new CsvDataHandler();
        tableModel = new ReorderableTableModel();
        table = TableSetup.createTable(tableModel);
        table.addMouseListener(new TableMouseListener(this));

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

        thresholdSlider = new JSlider(0, 100, 5);
        thresholdSlider.setMajorTickSpacing(20);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setToolTipText("Adjust threshold percentage");

        thresholdLabel = new JLabel("5%");

        thresholdSlider.addChangeListener(e -> {
            int thresholdValue = thresholdSlider.getValue();
            thresholdLabel.setText(thresholdValue + "%");
            calculateAndDisplayPureRegions(thresholdValue);
        });

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(thresholdSlider, BorderLayout.CENTER);
        sliderPanel.add(thresholdLabel, BorderLayout.EAST);

        bottomPanel.add(sliderPanel, BorderLayout.EAST);
        bottomPanel.add(selectedRowsLabel, BorderLayout.CENTER);

        statsPanel = new JPanel(new BorderLayout());
        statsPanel.add(statsScrollPane, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

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

    public boolean hasHiddenRows() {
        return !hiddenRows.isEmpty();
    }

    public void toggleTrigonometricColumns() {
        if (!isNormalized) {
            int choice = JOptionPane.showConfirmDialog(this, 
                "Data is not normalized. Normalization is required to insert trigonometric columns. Would you like to normalize the data now?", 
                "Normalization Required", 
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                dataHandler.normalizeData();
                updateTableData(dataHandler.getNormalizedData());
                isNormalized = true;
                toggleButton.setIcon(UIHelper.loadIcon("icons/denormalize.png", 40, 40));
                toggleButton.setToolTipText("Default");
            } else {
                return;
            }
        }
        
        if (areDifferenceColumnsVisible) {
            removeTrigonometricColumns(); // If columns are visible, remove them
        } else {
            // Ask user to choose the mode for trigonometric columns
            String[] options = {"Forward Difference", "Backward Difference", "Direct"};
            String mode = (String) JOptionPane.showInputDialog(this, 
                "Choose the mode for trigonometric columns:", 
                "Trigonometric Columns Mode", 
                JOptionPane.PLAIN_MESSAGE, 
                null, 
                options, 
                options[0]);
    
            if (mode != null) { // User selected a mode, add columns based on the mode
                addTrigonometricColumns(mode);
            }
        }
    
        // Toggle the state of whether the difference columns are visible
        areDifferenceColumnsVisible = !areDifferenceColumnsVisible;
    }
    
    private void addTrigonometricColumns(String mode) {
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
    
        // Add new columns for trigonometric values with descriptive names
        for (int i = 0; i < numCols; i++) {
            int col1 = columnModel.getColumn(i).getModelIndex();
            int col2 = -1;
            String description = "";
    
            if (col1 == classColumnIndex) continue; // Skip the class column
    
            switch (mode) {
                case "Direct":
                    description = tableModel.getColumnName(col1);
                    break;
                case "Forward Difference":
                    col2 = (i + 1) % numCols;
                    if (col2 == classColumnIndex) col2 = (col2 + 1) % numCols;
                    description = tableModel.getColumnName(col2) + " - " + tableModel.getColumnName(col1);
                    break;
                case "Backward Difference":
                    col2 = (i - 1 + numCols) % numCols;
                    if (col2 == classColumnIndex) col2 = (col2 - 1 + numCols) % numCols;
                    description = tableModel.getColumnName(col1) + " - " + tableModel.getColumnName(col2);
                    break;
            }
    
            tableModel.addColumn("Arccos " + description);
            tableModel.addColumn("Arcsin " + description);
            tableModel.addColumn("Arctan " + description);
        }
    
        // Calculate and populate the new columns with trigonometric values
        for (int row = 0; row < numRows; row++) {
            int newColIndex = numCols; // Start adding at the new columns
    
            for (int i = 0; i < numCols; i++) {
                int col1 = columnModel.getColumn(i).getModelIndex();
                int col2;
    
                if (col1 == classColumnIndex) continue; // Skip the class column
    
                try {
                    double value1 = Double.parseDouble(tableModel.getValueAt(row, col1).toString());
                    double value2 = 0;
    
                    switch (mode) {
                        case "Direct":
                            value2 = value1;
                            break;
    
                        case "Forward Difference":
                            col2 = (i + 1) % numCols;
                            if (col2 == classColumnIndex) col2 = (col2 + 1) % numCols;
                            value2 = Double.parseDouble(tableModel.getValueAt(row, col2).toString());
                            value1 = value2 - value1;
                            break;
    
                        case "Backward Difference":
                            col2 = (i - 1 + numCols) % numCols;
                            if (col2 == classColumnIndex) col2 = (col2 - 1 + numCols) % numCols;
                            value2 = Double.parseDouble(tableModel.getValueAt(row, col2).toString());
                            value1 = value1 - value2;
                            break;
                    }
    
                    // Calculate trigonometric values
                    double arccosValue = Math.acos(value1);
                    double arcsinValue = Math.asin(value1);
                    double arctanValue = Math.atan(value1);
    
                    // Set the calculated values in the respective columns
                    tableModel.setValueAt(arccosValue, row, newColIndex++);
                    tableModel.setValueAt(arcsinValue, row, newColIndex++);
                    tableModel.setValueAt(arctanValue, row, newColIndex++);
                } catch (NumberFormatException | NullPointerException e) {
                    // If there's an issue parsing the numbers, set the values as empty strings
                    tableModel.setValueAt("", row, newColIndex++);
                    tableModel.setValueAt("", row, newColIndex++);
                    tableModel.setValueAt("", row, newColIndex++);
                }
            }
        }
    
        dataHandler.updateStats(tableModel, statsTextArea);
        updateSelectedRowsLabel();
    }    

    public void showRuleOverlayPlot() {
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        String[] columnNames = new String[columnCount];
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = columnModel.getColumn(i).getHeaderValue().toString();
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        List<String[]> data = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (!hiddenRows.contains(row)) {
                String[] rowData = new String[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    Object value = tableModel.getValueAt(row, col);
                    rowData[col] = value != null ? value.toString() : "";
                }
                data.add(rowData);
            }
        }
    
        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);
    
        List<PureRegion> pureRegions = PureRegion.calculatePureRegions(tableModel, thresholdSlider.getValue(), getClassColumnIndex());

        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, classColors, getClassColumnIndex(), columnOrder, selectedRows);
        plot.setPureRegionsOverlay(pureRegions);
        plot.setVisible(true);
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

            dataHandler.updateStats(tableModel, statsTextArea);
            updateSelectedRowsLabel();
        }
    }

    public double calculateAndDisplayPureRegions(int thresholdPercentage) {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            noDataLoadedError();
            return 0;
        }

        List<PureRegion> pureRegions = PureRegion.calculatePureRegions(tableModel, thresholdPercentage, classColumnIndex);

        int totalRows = tableModel.getRowCount();
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

        displayPureRegions(pureRegions);
        
        return remainingCoverage;
    }

    public void toggleEasyCases() {
        if (hiddenRows.isEmpty()) {
            hideEasyCases();
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/uneasy.png", 40, 40));
            toggleEasyCasesButton.setToolTipText("Show All Cases");
        } else {
            showEasyCases();
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/easy.png", 40, 40));
            toggleEasyCasesButton.setToolTipText("Show Easy Cases");
        }
    }

    public void hideEasyCases() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            noDataLoadedError();
            return;
        }

        int currentThreshold = thresholdSlider.getValue();
        List<PureRegion> pureRegions = PureRegion.calculatePureRegions(tableModel, currentThreshold, classColumnIndex);
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
        updateSelectedRowsLabel();
    }

    public int calculateRemainingCases(int threshold) {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            noDataLoadedError();
            return 0;
        }

        List<PureRegion> pureRegions = PureRegion.calculatePureRegions(tableModel, threshold, classColumnIndex);
        Set<Integer> hiddenRows = new HashSet<>();

        for (PureRegion region : pureRegions) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
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

        return tableModel.getRowCount() - hiddenRows.size();
    }

    public void setSliderToMaxCoverage() {
        int bestThreshold = 0;
        int minRemainingCases = Integer.MAX_VALUE;

        for (int threshold = 0; threshold <= 100; threshold++) {
            int remainingCases = calculateRemainingCases(threshold);
            if (remainingCases <= minRemainingCases) {
                minRemainingCases = remainingCases;
                bestThreshold = threshold;
            }
        }

        thresholdSlider.setValue(bestThreshold);
        calculateAndDisplayPureRegions(bestThreshold);
    }

    public void toggleStatsVisibility(boolean hideStats) {
        if (hideStats) {
            splitPane.setBottomComponent(null);
            splitPane.setDividerSize(0);
            switchToggleStatsButton(toggleStatsOffButton);
        } else {
            splitPane.setBottomComponent(statsPanel);
            splitPane.setDividerSize(10);
            splitPane.setDividerLocation(0.8);
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

            clearTableAndState();

            dataHandler.loadCsvData(filePath, tableModel, statsTextArea);

            isNormalized = false;
            isHeatmapEnabled = false;
            isClassColorEnabled = false;
            generateClassColors();
            generateClassShapes();
            updateSelectedRowsLabel();

            toggleButton.setIcon(UIHelper.loadIcon("icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");

            setSliderToMaxCoverage();

            statsTextArea.setCaretPosition(0);
        }
    }

    private void clearTableAndState() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        classColors.clear();
        classShapes.clear();
        dataHandler.clearData();
        originalData = null;
        originalColumnNames = null;
        areDifferenceColumnsVisible = false;
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

        currentCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        statsTextArea.setCaretPosition(currentCaretPosition);
    }

    public void updateTableData(List<String[]> data) {
        int currentCaretPosition = statsTextArea.getCaretPosition();

        tableModel.setRowCount(0);
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

        int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length() - 1);
        statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));

        calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }

    public void generateClassColors() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            return;
        }
        Map<String, Integer> classMap = new HashMap<>();
        int colorIndex = 0;
        List<String> classNames = new ArrayList<>();
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
            ShapeUtils.createStar(4, 6, 3),
            ShapeUtils.createStar(5, 6, 3),
            ShapeUtils.createStar(6, 6, 3),
            ShapeUtils.createStar(7, 6, 3),
            ShapeUtils.createStar(8, 6, 3)
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

        int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));
        calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }

    private void displayPureRegions(List<PureRegion> pureRegions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Single-Attribute Pure Regions:\n");
        for (int i = pureRegions.size() - 1; i >= 0; i--) {
            PureRegion region = pureRegions.get(i);
            sb.append(String.format("Attribute: %s, Pure Region: %.2f <= %s < %.2f, Class: %s, Count: %d (%.2f%% of class, %.2f%% of dataset)\n",
                    region.attributeName, region.start, region.attributeName, region.end,
                    region.currentClass, region.regionCount, region.percentageOfClass, region.percentageOfDataset));
        }
        dataHandler.updateStats(tableModel, statsTextArea);
        statsTextArea.append(sb.toString());
    }

    public void applyDefaultRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (hasFocus) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder());
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
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder());
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
                        g2.fill(shape);
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

        JPanel shapePickerPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        shapePickerPanel.setBorder(BorderFactory.createTitledBorder("Pick Shape"));

        Shape[] availableShapes = {
            new Ellipse2D.Double(-5, -5, 10, 10),
            new Rectangle2D.Double(-5, -5, 10, 10),
            new Polygon(new int[]{-5, 5, 0}, new int[]{-5, -5, 5}, 3),
            ShapeUtils.createStar(4, 10, 5),
            ShapeUtils.createStar(5, 10, 5),
            ShapeUtils.createStar(6, 10, 5),
            ShapeUtils.createStar(7, 10, 5),
            ShapeUtils.createStar(8, 10, 5)
        };

        ButtonGroup shapeButtonGroup = new ButtonGroup();
        JRadioButton[] shapeButtons = new JRadioButton[availableShapes.length];

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
                    g2.scale(1.5, 1.5);
                    g2.fill(shape);
                    g2.translate(-15, -15);
                }
            };
            shapeButtons[i].setPreferredSize(new Dimension(40, 40));
            shapeButtonGroup.add(shapeButtons[i]);
            shapePickerPanel.add(shapeButtons[i]);

            shapeButtons[i].addActionListener(e -> {
                String selectedClass = (String) classComboBox.getSelectedItem();
                tempClassShapes.put(selectedClass, shape);
            });
        }

        mainPanel.add(shapePickerPanel, BorderLayout.CENTER);

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
            Window window = SwingUtilities.getWindowAncestor(okButton);
            if (window != null) {
                window.dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(cancelButton);
            if (window != null) {
                window.dispose();
            }
        });

        buttonPanel.add(setColorButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        classComboBox.addActionListener(e -> updateShapeSelection.run());
        updateShapeSelection.run();

        JDialog dialog = new JDialog(this, "Select Class, Color & Shape", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setSize(dialog.getWidth(), dialog.getHeight() + 75);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void insertRow() {
        int currentCaretPosition = statsTextArea.getCaretPosition();

        int numColumns = tableModel.getColumnCount();
        String[] emptyRow = new String[numColumns];

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

            List<Integer> rowsToDelete = new ArrayList<>();
            for (int row : selectedRows) {
                rowsToDelete.add(table.convertRowIndexToModel(row));
            }
            rowsToDelete.sort(Collections.reverseOrder());

            for (int rowIndex : rowsToDelete) {
                tableModel.removeRow(rowIndex);
            }

            dataHandler.updateStats(tableModel, statsTextArea);
            updateSelectedRowsLabel();

            int newCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
            statsTextArea.setCaretPosition(Math.max(newCaretPosition, 0));
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
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    writer.write(tableModel.getColumnName(col));
                    if (col < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

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

        List<String[]> data = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (!hiddenRows.contains(row)) {
                String[] rowData = new String[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    Object value = tableModel.getValueAt(row, col);
                    rowData[col] = value != null ? value.toString() : "";
                }
                data.add(rowData);
            }
        }

        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);

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
                if (!hiddenRows.contains(row)) {
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
            if (!hiddenRows.contains(row)) {
                classLabels.add((String) tableModel.getValueAt(row, getClassColumnIndex()));
            }
        }

        int numPlots = (attributeNames.size() + 1) / 2;
        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);

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

        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (!hiddenRows.contains(row)) {
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
            if (!hiddenRows.contains(row)) {
                classLabels.add((String) tableModel.getValueAt(row, getClassColumnIndex()));
            }
        }

        List<Integer> selectedRows = getSelectedRowsIndices();
        selectedRows.removeIf(hiddenRows::contains);

        StaticCircularCoordinatesPlot plot = new StaticCircularCoordinatesPlot(data, attributeNames, classColors, classShapes, classLabels, selectedRows);
        plot.setVisible(true);
    }

    public void showRuleTesterDialog() {
        RuleTesterDialog ruleTesterDialog = new RuleTesterDialog(this, tableModel);
        ruleTesterDialog.setVisible(true);
    }

    public void updateSelectedRowsLabel() {
        int selectedRowCount = table.getSelectedRowCount();
        int totalVisibleRowCount = table.getRowCount();
        int totalRowCount = tableModel.getRowCount();

        double visiblePercentage = (totalVisibleRowCount / (double) totalRowCount) * 100.0;

        selectedRowsLabel.setText(String.format("Selected rows: %d / Total visible cases: %d / Total cases: %d = %.2f%% of dataset",
                selectedRowCount, totalVisibleRowCount, totalRowCount, visiblePercentage));
    }

    public List<Integer> getSelectedRowsIndices() {
        int[] selectedRows = table.getSelectedRows();
        List<Integer> selectedIndices = new ArrayList<>();
        for (int row : selectedRows) {
            selectedIndices.add(table.convertRowIndexToModel(row));
        }
        return selectedIndices;
    }
}
