package src;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Random;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import src.managers.*;
import src.table.ReorderableTableModel;
import src.table.TableSetup;
import src.utils.ShapeUtils;
import src.utils.CovariancePairUtils;
import src.classifiers.SupportSumMachineClassifier;
import src.classifiers.KNearestNeighborsClassifier;
import src.utils.PureRegionUtils;
import utils.ColumnDataTypeInfo;

public class CsvViewer extends JFrame {
    public JTable table;
    public ReorderableTableModel tableModel;
    public CsvDataHandler dataHandler;
    public JTextArea statsTextArea;
    public JButton toggleButton;
    public JButton toggleEasyCasesButton;
    public JButton toggleStatsButton;
    public JButton toggleStatsOnButton;
    public JButton toggleStatsOffButton;
    public JButton toggleTrigonometricButton;
    public JLabel selectedRowsLabel;
    public JPanel bottomPanel;
    public JPanel statsPanel;
    public JScrollPane statsScrollPane;
    public JScrollPane tableScrollPane;
    public JSplitPane splitPane;
    public JSlider thresholdSlider;
    public JLabel thresholdLabel;
    public JPopupMenu normalizationMenu;

    private TrigonometricColumnManager trigColumnManager;
    private PureRegionManager pureRegionManager;
    private VisualizationManager visualizationManager;
    private RendererManager rendererManager;
    private DataExporter dataExporter;
    private StateManager stateManager;
    private ButtonPanelManager buttonPanelManager;
    private TableManager tableManager;
    private MainMenu mainMenu;

    public CsvViewer(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
        stateManager = new StateManager();
    
        setTitle("JTabViz");
        setSize(1200, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    
        // Add modern styling
        getRootPane().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(64, 64, 64)));
        getContentPane().setBackground(new Color(240, 240, 240));
    
        // Create main content panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setContentPane(mainPanel);
    
        dataHandler = new CsvDataHandler();
        tableModel = new ReorderableTableModel();
        table = TableSetup.createTable(tableModel);
    
        rendererManager = new RendererManager(this);
        tableManager = new TableManager(this, tableModel);
    
        CsvViewerUIHelper.setupTable(table, tableModel, this);
    
        buttonPanelManager = new ButtonPanelManager(this);
        JMenuBar buttonPanel = buttonPanelManager.createMenuBar();
        buttonPanel.setBorder((Border) new BevelBorder(BevelBorder.RAISED));
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
    
        statsTextArea = UIHelper.createTextArea(3, 0);
        statsScrollPane = CsvViewerUIHelper.createStatsScrollPane(statsTextArea);
        
        // Initialize the thresholdSlider before PureRegionManager
        thresholdSlider = new JSlider(0, 100, 5);
        thresholdSlider.setMajorTickSpacing(20);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setToolTipText("Adjust threshold percentage");

        tableScrollPane = new JScrollPane(table);
        trigColumnManager = new TrigonometricColumnManager(table, this);
        pureRegionManager = new PureRegionManager(this, tableModel, statsTextArea, thresholdSlider);
        visualizationManager = new VisualizationManager(this);
        dataExporter = new DataExporter(tableModel);
        selectedRowsLabel = new JLabel("Selected cases: 0");
    
        thresholdLabel = new JLabel("5%");
        thresholdSlider.addChangeListener(e -> {
            int thresholdValue = thresholdSlider.getValue();
            thresholdLabel.setText(thresholdValue + "%");
            pureRegionManager.calculateAndDisplayPureRegions(thresholdValue);
        });
    
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAllOwnedWindows();
                dispose();
                mainMenu.setVisible(true);
            }
        });

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    table.clearSelection();
                    updateSelectedRowsLabel(); // Update the status if you have one
                }
            }
        });
    
        bottomPanel = CsvViewerUIHelper.createBottomPanel(selectedRowsLabel, thresholdSlider, thresholdLabel);
        statsPanel = new JPanel(new BorderLayout());
        statsPanel.add(statsScrollPane, BorderLayout.CENTER);
    
        splitPane = CsvViewerUIHelper.createSplitPane(tableScrollPane, statsPanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    
        generateClassShapes();
    }    

    public void copySelectedCell() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row != -1 && col != -1) {
            Object cellValue = table.getValueAt(row, col);
            StringSelection stringSelection = new StringSelection(cellValue != null ? cellValue.toString() : "");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        }
    }

    public boolean hasHiddenRows() {
        return !stateManager.getHiddenRows().isEmpty();
    }

    public java.util.List<Integer> getHiddenRows() {
        return stateManager.getHiddenRows();
    }

    public void toggleTrigonometricColumns() {
        trigColumnManager.toggleTrigonometricColumns();
    }

    public void showStarCoordinatesPlot() {
        visualizationManager.showStarCoordinatesPlot();
    }

    public void showCollocatedPairedCoordinates() {
        visualizationManager.showCollocatedPairedCoordinates();
    }

    public void insertWeightedSumColumn() {
        if (tableModel.getColumnCount() == 0) {
            noDataLoadedError();
            return;
        }
        
        SupportSumMachineClassifier ssm = new SupportSumMachineClassifier(this, tableModel);
        ssm.insertWeightedSumColumn();
    }

    public String getUniqueColumnName(String baseName) {
        String newName = baseName;
        int counter = 1;
        while (columnExists(newName)) {
            newName = baseName + " (" + counter + ")";
            counter++;
        }
        return newName;
    }

    private boolean columnExists(String columnName) {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    public void toggleEasyCases() {
        pureRegionManager.toggleEasyCases();
    }

    public void toggleStatsVisibility(boolean hideStats) {
        if (hideStats) {
            splitPane.setBottomComponent(null);
            splitPane.setDividerSize(0);
            switchToggleStatsButton(toggleStatsOffButton);
        } else {
            splitPane.setBottomComponent(statsPanel);
            splitPane.setDividerSize(4);
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

    public String getDatasetName() {
        return stateManager.getDatasetName();
    }

    public void loadCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("datasets"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            stateManager.setDatasetName(selectedFile.getName());
            String datasetName = stateManager.getDatasetName();
            datasetName = datasetName.substring(0, datasetName.lastIndexOf('.'));
            stateManager.setDatasetName(datasetName);

            clearTableAndState();

            dataHandler.loadCsvData(filePath, tableModel, statsTextArea);

            // Store original data
            List<List<String>> originalData = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                List<String> rowData = new ArrayList<>();
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object value = tableModel.getValueAt(row, col);
                    rowData.add(value != null ? value.toString() : "");
                }
                originalData.add(rowData);
            }
            stateManager.setOriginalData(originalData);

            // Format decimal values
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    Object value = tableModel.getValueAt(row, col);
                    if (value != null && isNumeric(value.toString())) {
                        tableModel.setValueAt(formatAsDecimal(value.toString()), row, col);
                    }
                }
            }

            java.util.List<String> originalColumnNames = new ArrayList<>();
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                originalColumnNames.add(tableModel.getColumnName(i));
            }
            stateManager.setOriginalColumnNames(originalColumnNames);
            stateManager.addClassColumn(getClassColumnIndex());
            stateManager.setNormalized(false);
            stateManager.setHeatmapEnabled(false);
            stateManager.setClassColorEnabled(false);
            generateClassColors();
            generateClassShapes();
            updateSelectedRowsLabel();

            // Update the toggle button through ButtonPanelManager
            buttonPanelManager.getToggleButton().setIcon(UIHelper.loadIcon("/icons/normalize.png", 40, 40));
            buttonPanelManager.getToggleButton().setToolTipText("Normalize");
            
            tableManager.autoResizeColumns();
            statsTextArea.setCaretPosition(0);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatDecimalWithoutScientificNotation(double value) {
        int precision = stateManager.getDecimalPrecision();
        StringBuilder formatPattern = new StringBuilder("#.");
        for (int i = 0; i < precision; i++) {
            formatPattern.append("#");
        }
        DecimalFormat decimalFormat = new DecimalFormat(formatPattern.toString());
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        return decimalFormat.format(value);
    }

    private String formatAsDecimal(String str) {
        double value = Double.parseDouble(str.trim());
        return formatDecimalWithoutScientificNotation(value);
    }

    private void clearTableAndState() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        stateManager.clearState();
        dataHandler.clearData();
    }

    public void closeData() {
        clearTableAndState();
    }
    
    /**
     * Gets the data type information for a column
     * @param columnIndex Index of the column
     * @return ColumnDataTypeInfo object, or null if not found
     */
    public utils.ColumnDataTypeInfo getColumnDataType(int columnIndex) {
        return dataHandler.getColumnDataType(columnIndex);
    }
    
    /**
     * Gets all column data type information
     * @return Map of column index to ColumnDataTypeInfo
     */
    public java.util.Map<Integer, utils.ColumnDataTypeInfo> getAllColumnDataTypes() {
        return dataHandler.getAllColumnDataTypes();
    }

    public void showCovarianceSortDialog() {
        if (tableModel.getColumnCount() == 0) {
            noDataLoadedError();
            return;
        }

        String[] options = {"Covariance Sort", "Hamiltonian Path Sort"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Select sorting method:",
            "Sort Columns",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        if (choice == 0) {
            // Existing covariance sort logic
            java.util.List<String> attributes = new ArrayList<>();
            int classColumnIndex = getClassColumnIndex();

            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (i != classColumnIndex) {
                    String columnName = tableModel.getColumnName(i);
                    if (columnName.length() > 40) {
                        columnName = columnName.substring(0, 37) + "...";
                    }
                    attributes.add(columnName);
                }
            }

            String selectedAttribute = (String) JOptionPane.showInputDialog(
                this,
                "Select an attribute to sort by covariance:",
                "Covariance Column Sort", 
                JOptionPane.PLAIN_MESSAGE,
                null,
                attributes.toArray(new String[0]),
                attributes.get(0)
            );

            if (selectedAttribute != null) {
                // Get original column name if it was truncated
                String originalName = selectedAttribute;
                if (selectedAttribute.endsWith("...")) {
                    for (int i = 0; i < tableModel.getColumnCount(); i++) {
                        String fullName = tableModel.getColumnName(i);
                        if (fullName.startsWith(selectedAttribute.substring(0, 37))) {
                            originalName = fullName;
                            break;
                        }
                    }
                }
                sortColumnsByCovariance(originalName);
            }
        } else if (choice == 1) {
            sortColumnsByHamiltonianPath();
        }
    }

    public void showFrequencySortDialog() {
        if (tableModel.getColumnCount() == 0) {
            noDataLoadedError();
            return;
        }

        // Calculate frequency of empty entries for each column
        java.util.List<ColumnFrequencyPair> frequencyPairs = new ArrayList<>();
        int classColumnIndex = getClassColumnIndex();
        
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != classColumnIndex) {
                int emptyCount = 0;
                int totalCount = tableModel.getRowCount();
                
                // Count empty entries in this column (including NaN values)
                for (int row = 0; row < totalCount; row++) {
                    Object value = tableModel.getValueAt(row, i);
                    if (value == null || value.toString().trim().isEmpty()) {
                        emptyCount++;
                    } else {
                        // Check if the value is NaN
                        try {
                            double numValue = Double.parseDouble(value.toString());
                            if (Double.isNaN(numValue)) {
                                emptyCount++;
                            }
                        } catch (NumberFormatException e) {
                            // If it's not a number, it's not considered empty
                        }
                    }
                }
                
                // Calculate frequency as percentage of empty entries
                double emptyFrequency = totalCount > 0 ? (double) emptyCount / totalCount : 0.0;
                frequencyPairs.add(new ColumnFrequencyPair(i, emptyFrequency, emptyCount, totalCount));
            }
        }
        
        // Sort by frequency of empty entries in decreasing order (most empty first)
        frequencyPairs.sort((p1, p2) -> Double.compare(p2.getEmptyFrequency(), p1.getEmptyFrequency()));
        
        // Show summary information before reordering
        StringBuilder summary = new StringBuilder();
        summary.append("Columns sorted by frequency of empty entries (decreasing):\n\n");
        for (ColumnFrequencyPair pair : frequencyPairs) {
            String columnName = tableModel.getColumnName(pair.getColumnIndex());
            summary.append(String.format("%s: %.1f%% empty (%d/%d entries)\n", 
                columnName, pair.getEmptyFrequency() * 100, pair.getEmptyCount(), pair.getTotalCount()));
        }
        
        // Reorder columns based on the sorted frequency
        TableColumnModel columnModel = table.getColumnModel();
        
        // Create a list to track the new order
        java.util.List<String> newColumnOrder = new ArrayList<>();
        
        // Add class column first if it exists
        if (classColumnIndex != -1) {
            newColumnOrder.add(tableModel.getColumnName(classColumnIndex));
        }
        
        // Add columns in sorted order (most empty first)
        for (ColumnFrequencyPair pair : frequencyPairs) {
            newColumnOrder.add(tableModel.getColumnName(pair.getColumnIndex()));
        }
        
        // Reorder columns by moving them to their new positions
        for (int i = 0; i < newColumnOrder.size(); i++) {
            String columnName = newColumnOrder.get(i);
            int currentIndex = columnModel.getColumnIndex(columnName);
            if (currentIndex != i) {
                columnModel.moveColumn(currentIndex, i);
            }
        }
        
        // Update display
        table.getTableHeader().repaint();
        table.repaint();
        
        statsTextArea.append("\n" + summary.toString());
    }
    
    // Helper class to store column frequency information
    private static class ColumnFrequencyPair {
        private final int columnIndex;
        private final double emptyFrequency;
        private final int emptyCount;
        private final int totalCount;
        
        public ColumnFrequencyPair(int columnIndex, double emptyFrequency, int emptyCount, int totalCount) {
            this.columnIndex = columnIndex;
            this.emptyFrequency = emptyFrequency;
            this.emptyCount = emptyCount;
            this.totalCount = totalCount;
        }
        
        public int getColumnIndex() { return columnIndex; }
        public double getEmptyFrequency() { return emptyFrequency; }
        public int getEmptyCount() { return emptyCount; }
        public int getTotalCount() { return totalCount; }
    }

    private void sortColumnsByHamiltonianPath() {
        int classColumnIndex = getClassColumnIndex();
        int numColumns = tableModel.getColumnCount();
        List<Integer> columnIndices = new ArrayList<>();
        
        // Collect indices of all columns except the class column
        for (int i = 0; i < numColumns; i++) {
            if (i != classColumnIndex) {
                columnIndices.add(i);
            }
        }
        
        // Create adjacency matrix based on covariance
        double[][] adjacencyMatrix = new double[columnIndices.size()][columnIndices.size()];
        for (int i = 0; i < columnIndices.size(); i++) {
            for (int j = 0; j < columnIndices.size(); j++) {
                if (i != j) {
                    adjacencyMatrix[i][j] = Math.abs(calculateCovariance(
                        getColumnValues(columnIndices.get(i)),
                        getColumnValues(columnIndices.get(j))
                    ));
                }
            }
        }
        
        // Find first Hamiltonian path using nearest neighbor approach
        List<Integer> path = findHamiltonianPath(adjacencyMatrix);
        
        // Reorder columns based on the path
        TableColumnModel columnModel = table.getColumnModel();
        int currentPosition = 0;
        for (Integer pathIndex : path) {
            int columnIndex = columnIndices.get(pathIndex);
            int fromIndex = columnModel.getColumnIndex(tableModel.getColumnName(columnIndex));
            columnModel.moveColumn(fromIndex, currentPosition++);
        }
        
        table.getTableHeader().repaint();
        table.repaint();
    }

    private double[] getColumnValues(int columnIndex) {
        double[] values = new double[tableModel.getRowCount()];
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                values[i] = Double.parseDouble(tableModel.getValueAt(i, columnIndex).toString());
            } catch (NumberFormatException e) {
                values[i] = Double.NaN;
            }
        }
        return values;
    }

    private List<Integer> findHamiltonianPath(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        List<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        
        // Start with vertex 0
        path.add(0);
        visited[0] = true;
        
        // Find nearest unvisited neighbor
        while (path.size() < n) {
            int current = path.get(path.size() - 1);
            double maxCovariance = -1;
            int nextVertex = -1;
            
            for (int i = 0; i < n; i++) {
                if (!visited[i] && adjacencyMatrix[current][i] > maxCovariance) {
                    maxCovariance = adjacencyMatrix[current][i];
                    nextVertex = i;
                }
            }
            
            if (nextVertex == -1) {
                // If no unvisited neighbor found, add first unvisited vertex
                for (int i = 0; i < n; i++) {
                    if (!visited[i]) {
                        nextVertex = i;
                        break;
                    }
                }
            }
            
            path.add(nextVertex);
            visited[nextVertex] = true;
        }
        
        return path;
    }

    private void closeAllOwnedWindows() {
        for (Window window : getOwnerlessWindows()) {
            window.dispose();
        }
    }

    private void sortColumnsByCovariance(String selectedAttribute) {
        int selectedColumnIndex = tableModel.findColumn(selectedAttribute);
        int classColumnIndex = getClassColumnIndex();

        java.util.List<CovariancePairUtils> covariancePairs = new ArrayList<>();

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != selectedColumnIndex && i != classColumnIndex) {
                double covariance = calculateCovarianceBetweenColumns(selectedColumnIndex, i);
                covariancePairs.add(new CovariancePairUtils(i, covariance));
            }
        }

        covariancePairs.sort((p1, p2) -> Double.compare(p1.getCovariance(), p2.getCovariance()));

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < covariancePairs.size(); i++) {
            int fromIndex = columnModel.getColumnIndex(tableModel.getColumnName(covariancePairs.get(i).getColumnIndex()));
            columnModel.moveColumn(fromIndex, i);
        }

        table.getTableHeader().repaint();
        table.repaint();
    }

    public void sortColumnsByLDACoefficients() {
        if (tableModel.getColumnCount() == 0) {
            noDataLoadedError();
            return;
        }

        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(this, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Calculate LDA coefficients
        double[] ldaCoefficients = calculateLDACoefficients(classColumnIndex);
        if (ldaCoefficients == null) {
            return; // Error already shown
        }

        // Create list of column index and coefficient pairs
        java.util.List<LDACoefficientPair> coefficientPairs = new ArrayList<>();
        int featureIndex = 0;
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != classColumnIndex) {
                coefficientPairs.add(new LDACoefficientPair(i, ldaCoefficients[featureIndex++]));
            }
        }

        // Sort by absolute value of coefficients (descending - most important first)
        coefficientPairs.sort((p1, p2) -> Double.compare(Math.abs(p2.getCoefficient()), Math.abs(p1.getCoefficient())));

        // Reorder columns
        TableColumnModel columnModel = table.getColumnModel();
        int currentPosition = 0;
        
        // Keep class column at its position (usually first)
        if (classColumnIndex == 0) {
            currentPosition = 1;
        }
        
        for (LDACoefficientPair pair : coefficientPairs) {
            int fromIndex = columnModel.getColumnIndex(tableModel.getColumnName(pair.getColumnIndex()));
            if (fromIndex != currentPosition) {
                columnModel.moveColumn(fromIndex, currentPosition);
            }
            currentPosition++;
        }

        table.getTableHeader().repaint();
        table.repaint();
        
        // Show summary
        StringBuilder summary = new StringBuilder();
        summary.append("\nColumns sorted by LDA coefficients (absolute value, descending):\n\n");
        for (LDACoefficientPair pair : coefficientPairs) {
            String columnName = tableModel.getColumnName(pair.getColumnIndex());
            summary.append(String.format("%s: %.6f\n", columnName, pair.getCoefficient()));
        }
        statsTextArea.append(summary.toString());
    }

    // Helper class to store column index and LDA coefficient
    private static class LDACoefficientPair {
        private final int columnIndex;
        private final double coefficient;
        
        public LDACoefficientPair(int columnIndex, double coefficient) {
            this.columnIndex = columnIndex;
            this.coefficient = coefficient;
        }
        
        public int getColumnIndex() { return columnIndex; }
        public double getCoefficient() { return coefficient; }
    }

    private double[] calculateLDACoefficients(int classColumnIndex) {
        try {
            // Get unique classes and their counts
            Map<String, List<double[]>> classData = new HashMap<>();
            Set<String> uniqueClasses = new HashSet<>();
            int numFeatures = tableModel.getColumnCount() - 1;

            // Collect data by class
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String className = tableModel.getValueAt(row, classColumnIndex).toString();
                uniqueClasses.add(className);
                
                double[] features = new double[numFeatures];
                int featureIndex = 0;
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    if (col != classColumnIndex) {
                        features[featureIndex++] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    }
                }
                
                classData.computeIfAbsent(className, k -> new ArrayList<>()).add(features);
            }

            if (uniqueClasses.size() < 2) {
                JOptionPane.showMessageDialog(this, "Need at least 2 classes for LDA.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // Calculate class means
            Map<String, double[]> classMeans = new HashMap<>();
            for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
                double[] mean = new double[numFeatures];
                List<double[]> samples = entry.getValue();
                
                for (double[] sample : samples) {
                    for (int i = 0; i < numFeatures; i++) {
                        mean[i] += sample[i];
                    }
                }
                
                for (int i = 0; i < numFeatures; i++) {
                    mean[i] /= samples.size();
                }
                
                classMeans.put(entry.getKey(), mean);
            }

            // Calculate global mean
            double[] globalMean = new double[numFeatures];
            int totalSamples = 0;
            for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
                double[] classMean = classMeans.get(entry.getKey());
                int classSize = entry.getValue().size();
                totalSamples += classSize;
                
                for (int i = 0; i < numFeatures; i++) {
                    globalMean[i] += classMean[i] * classSize;
                }
            }
            for (int i = 0; i < numFeatures; i++) {
                globalMean[i] /= totalSamples;
            }

            // Calculate within-class scatter matrix
            double[][] Sw = new double[numFeatures][numFeatures];
            for (List<double[]> samples : classData.values()) {
                for (double[] sample : samples) {
                    for (int i = 0; i < numFeatures; i++) {
                        for (int j = 0; j < numFeatures; j++) {
                            Sw[i][j] += (sample[i] - globalMean[i]) * (sample[j] - globalMean[j]);
                        }
                    }
                }
            }

            // Calculate between-class scatter matrix
            double[][] Sb = new double[numFeatures][numFeatures];
            for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
                double[] classMean = classMeans.get(entry.getKey());
                int classSize = entry.getValue().size();
                
                for (int i = 0; i < numFeatures; i++) {
                    for (int j = 0; j < numFeatures; j++) {
                        Sb[i][j] += classSize * (classMean[i] - globalMean[i]) * (classMean[j] - globalMean[j]);
                    }
                }
            }

            // Add small regularization to Sw to ensure it's invertible
            double epsilon = 1e-10;
            for (int i = 0; i < numFeatures; i++) {
                Sw[i][i] += epsilon;
            }

            // Calculate Sw inverse
            double[][] SwInv = matrixInverse(Sw);
            
            // Calculate SwInv * Sb
            double[][] M = matrixMultiply(SwInv, Sb);

            // Find first eigenvector using power iteration
            double[] eigenvector = powerIterationFirst(M);

            return eigenvector;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error calculating LDA coefficients: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private double[][] matrixInverse(double[][] matrix) {
        int n = matrix.length;
        double[][] augmented = new double[n][2*n];
        
        // Create augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1.0;
        }
        
        // Gaussian elimination
        for (int i = 0; i < n; i++) {
            double pivot = augmented[i][i];
            if (Math.abs(pivot) < 1e-10) {
                // Handle near-zero pivot
                for (int k = i + 1; k < n; k++) {
                    if (Math.abs(augmented[k][i]) > 1e-10) {
                        double[] temp = augmented[i];
                        augmented[i] = augmented[k];
                        augmented[k] = temp;
                        pivot = augmented[i][i];
                        break;
                    }
                }
            }
            
            for (int j = 0; j < 2*n; j++) {
                augmented[i][j] /= pivot;
            }
            
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    double factor = augmented[k][i];
                    for (int j = 0; j < 2*n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }
        
        // Extract inverse matrix
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }
        
        return inverse;
    }

    private double[][] matrixMultiply(double[][] A, double[][] B) {
        int m = A.length;
        int n = B[0].length;
        int p = A[0].length;
        double[][] result = new double[m][n];
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        
        return result;
    }

    private double[] powerIterationFirst(double[][] matrix) {
        int n = matrix.length;
        Random rand = new Random(42); // Fixed seed for reproducibility
        
        // Initialize random vector
        double[] vector = new double[n];
        for (int i = 0; i < n; i++) {
            vector[i] = rand.nextDouble();
        }
        
        // Power iteration
        for (int iter = 0; iter < 100; iter++) {
            double[] newVector = new double[n];
            
            // Matrix-vector multiplication
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    newVector[i] += matrix[i][j] * vector[j];
                }
            }
            
            // Normalize
            double norm = 0;
            for (int i = 0; i < n; i++) {
                norm += newVector[i] * newVector[i];
            }
            norm = Math.sqrt(norm);
            
            if (norm < 1e-10) {
                break; // Vector became zero
            }
            
            for (int i = 0; i < n; i++) {
                vector[i] = newVector[i] / norm;
            }
        }
        
        return vector;
    }

    private double calculateCovarianceBetweenColumns(int col1, int col2) {
        int rowCount = tableModel.getRowCount();

        double[] values1 = new double[rowCount];
        double[] values2 = new double[rowCount];

        for (int i = 0; i < rowCount; i++) {
            try {
                values1[i] = Double.parseDouble(tableModel.getValueAt(i, col1).toString());
                values2[i] = Double.parseDouble(tableModel.getValueAt(i, col2).toString());
            } catch (NumberFormatException e) {
                values1[i] = Double.NaN;
                values2[i] = Double.NaN;
            }
        }

        return calculateCovariance(values1, values2);
    }

    public void deleteColumn(int viewColumnIndex) {
        tableManager.deleteColumn(viewColumnIndex);
    }

    public void toggleDataView() {
        int currentCaretPosition = statsTextArea.getCaretPosition();

        if (stateManager.isNormalized()) {
            tableManager.updateTableData(dataHandler.getOriginalData());
            stateManager.setNormalized(false);
            toggleButton.setIcon(UIHelper.loadIcon("/icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");
        } else {
            normalizationMenu.show(toggleButton, 0, toggleButton.getHeight());
        }

        currentCaretPosition = Math.min(currentCaretPosition, statsTextArea.getText().length());
        statsTextArea.setCaretPosition(currentCaretPosition);

        // Refresh the heatmap if it is enabled
        if (stateManager.isHeatmapEnabled()) {
            rendererManager.applyCombinedRenderer();
            // repaint
            table.repaint();
        }
    }

    public void updateTableData(java.util.List<String[]> data) {
        tableManager.updateTableData(data);
    }

    public void highlightBlanks() {
        tableManager.highlightBlanks();
    }

    public void toggleHeatmap() {
        stateManager.setHeatmapEnabled(!stateManager.isHeatmapEnabled());
        rendererManager.applyCombinedRenderer();
    }

    public void generateClassColors() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            return;
        }
        Map<String, Integer> classMap = new HashMap<>();
        int colorIndex = 0;
        java.util.List<String> classNames = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = (String) tableModel.getValueAt(row, classColumnIndex);

            if (className.equalsIgnoreCase("malignant") || className.equalsIgnoreCase("positive")) {
                stateManager.getClassColors().put(className, Color.RED);
                classNames.add(className);
            } else if (className.equalsIgnoreCase("benign") || className.equalsIgnoreCase("negative")) {
                stateManager.getClassColors().put(className, Color.GREEN);
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
            stateManager.getClassColors().put(entry.getKey(), color);
        }
    }

    public void showCovarianceMatrix() {
        int rowCount = tableModel.getRowCount();
        int colCount = tableModel.getColumnCount();
        java.util.List<double[]> numericalData = new ArrayList<>();
        java.util.List<String> columnNames = new ArrayList<>();

        for (int col = 0; colCount > col; col++) {
            boolean isNumeric = true;
            double[] columnData = new double[rowCount];

            for (int row = 0; row < rowCount; row++) {
                try {
                    columnData[row] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                } catch (NumberFormatException e) {
                    isNumeric = false;
                    break;
                }
            }

            if (isNumeric) {
                numericalData.add(columnData);
                columnNames.add(tableModel.getColumnName(col));
            }
        }

        int numAttributes = numericalData.size();
        double[][] covarianceMatrix = new double[numAttributes][numAttributes];

        double minCovariance = Double.MAX_VALUE;
        double maxCovariance = Double.MIN_VALUE;

        for (int i = 0; i < numAttributes; i++) {
            for (int j = 0; j < numAttributes; j++) {
                covarianceMatrix[i][j] = calculateCovariance(numericalData.get(i), numericalData.get(j));
                minCovariance = Math.min(minCovariance, covarianceMatrix[i][j]);
                maxCovariance = Math.max(maxCovariance, covarianceMatrix[i][j]);
            }
        }

        final double finalMinCovariance = minCovariance;
        final double finalMaxCovariance = maxCovariance;

        DefaultTableModel covarianceTableModel = new DefaultTableModel();

        covarianceTableModel.addColumn("Attributes");
        for (String columnName : columnNames) {
            covarianceTableModel.addColumn(columnName);
        }

        for (int i = 0; i < numAttributes; i++) {
            Object[] row = new Object[numAttributes + 1];
            row[0] = columnNames.get(i);
            for (int j = 0; j < numAttributes; j++) {
                row[j + 1] = String.format("%.4f", covarianceMatrix[i][j]);
            }
            covarianceTableModel.addRow(row);
        }

        JTable covarianceTable = new JTable(covarianceTableModel);
        covarianceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        if (stateManager.isHeatmapEnabled()) {
            covarianceTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (value != null && !value.toString().trim().isEmpty()) {
                        try {
                            double val = Double.parseDouble(value.toString());
                            double normalizedValue = (val - finalMinCovariance) / (finalMaxCovariance - finalMinCovariance);
                            Color color = getColorForValue(normalizedValue);
                            c.setBackground(color);
                        } catch (NumberFormatException e) {
                            c.setBackground(Color.WHITE);
                        }
                    } else {
                        c.setBackground(Color.WHITE);
                    }

                    c.setForeground(stateManager.getCellTextColor());
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
        }

        covarianceTable.setFont(table.getFont());
        covarianceTable.getTableHeader().setFont(table.getTableHeader().getFont());

        JScrollPane scrollPane = new JScrollPane(covarianceTable);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JFrame frame = new JFrame("Covariance Matrix");
        frame.add(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    private double calculateCovariance(double[] x, double[] y) {
        int n = x.length;
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);

        double covariance = 0.0;
        for (int i = 0; i < n; i++) {
            covariance += (x[i] - meanX) * (y[i] - meanY);
        }

        return covariance / (n - 1);
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
        for (String className : stateManager.getClassColors().keySet()) {
            stateManager.getClassShapes().put(className, availableShapes[i % availableShapes.length]);
            i++;
        }
    }

    public int getClassColumnIndex() {
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            String columnName = columnModel.getColumn(i).getHeaderValue().toString();
            if (columnName.equalsIgnoreCase("class") || columnName.equalsIgnoreCase("label")) {
                return columnModel.getColumn(i).getModelIndex();
            }
        }
        return -1;
    }

    public void toggleClassColors() {
        stateManager.setClassColorEnabled(!stateManager.isClassColorEnabled());
        rendererManager.applyCombinedRenderer();
    }

    public void applyDefaultRenderer() {
        rendererManager.applyDefaultRenderer();
    }

    public void applyCombinedRenderer() {
        rendererManager.applyCombinedRenderer();
    }

    public void showFontSettingsDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JLabel colorLabel = new JLabel("Font Color:");
        panel.add(colorLabel);
        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Font Color", stateManager.getCellTextColor());
            if (newColor != null) {
                stateManager.setCellTextColor(newColor);
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

            if (stateManager.isHeatmapEnabled() || stateManager.isClassColorEnabled()) {
                rendererManager.applyCombinedRenderer();
            } else {
                rendererManager.applyDefaultRenderer();
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

        ClassColorDialog dialog = new ClassColorDialog(this, stateManager.getClassColors(), stateManager.getClassShapes(), uniqueClassNames);
        dialog.setVisible(true);

        if (stateManager.isClassColorEnabled()) {
            rendererManager.applyCombinedRenderer();
        }
    }

    public void showBackgroundColorDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JLabel colorLabel = new JLabel("Plot Background Color:");
        panel.add(colorLabel);
        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Plot Background Color", stateManager.getPlotBackgroundColor());
            if (newColor != null) {
                stateManager.setPlotBackgroundColor(newColor);
            }
        });
        panel.add(colorButton);

        // Add a preview panel
        JPanel previewPanel = new JPanel();
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.setBackground(stateManager.getPlotBackgroundColor());
        previewPanel.setPreferredSize(new Dimension(100, 50));
        panel.add(previewPanel);

        // Update preview when color changes
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Plot Background Color", stateManager.getPlotBackgroundColor());
            if (newColor != null) {
                stateManager.setPlotBackgroundColor(newColor);
                previewPanel.setBackground(newColor);
                previewPanel.repaint();
            }
        });

        int result = JOptionPane.showConfirmDialog(this, panel, "Background Color Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            // The color has already been set in the button action listener
            // No additional action needed here
        }
    }

    public void showPolylineThicknessDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        JLabel thicknessLabel = new JLabel("Polyline Thickness:");
        panel.add(thicknessLabel);
        
        // Create slider for thickness
        JSlider thicknessSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, (int)(stateManager.getPolylineThickness() * 10));
        thicknessSlider.setMajorTickSpacing(5);
        thicknessSlider.setMinorTickSpacing(1);
        thicknessSlider.setPaintTicks(true);
        thicknessSlider.setPaintLabels(true);
        thicknessSlider.setLabelTable(thicknessSlider.createStandardLabels(5));
        panel.add(thicknessSlider);

        // Add current value label
        JLabel currentValueLabel = new JLabel("Current: " + String.format("%.1f", stateManager.getPolylineThickness()));
        panel.add(currentValueLabel);

        // Add preview panel
        JPanel previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                float currentThickness = thicknessSlider.getValue() / 10.0f;
                g2.setStroke(new BasicStroke(currentThickness));
                g2.setColor(Color.BLACK);
                
                int centerY = getHeight() / 2;
                g2.drawLine(20, centerY, getWidth() - 20, centerY);
            }
        };
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.setPreferredSize(new Dimension(200, 50));
        panel.add(previewPanel);

        // Update preview when slider changes
        thicknessSlider.addChangeListener(e -> {
            float newThickness = thicknessSlider.getValue() / 10.0f;
            currentValueLabel.setText("Current: " + String.format("%.1f", newThickness));
            previewPanel.repaint();
        });

        int result = JOptionPane.showConfirmDialog(this, panel, "Polyline Thickness Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            float newThickness = thicknessSlider.getValue() / 10.0f;
            stateManager.setPolylineThickness(newThickness);
        }
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

            java.util.List<Integer> rowsToDelete = new ArrayList<>();
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
        pureRegionManager.calculateAndDisplayPureRegions(thresholdSlider.getValue());
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
        dataExporter.exportCsvFile();
    }

    public void showParallelCoordinatesPlot() {
        visualizationManager.showParallelCoordinatesPlot();
    }

    public void showShiftedPairedCoordinates() {
        visualizationManager.showShiftedPairedCoordinates();
    }

    public void showCircularCoordinatesPlot() {
        visualizationManager.showCircularCoordinatesPlot();
    }

    public void showDecisionTreeVisualization() {
        visualizationManager.showDecisionTreeVisualization();
    }

    public void showRuleTesterDialog() {
        RuleTesterDialog ruleTesterDialog = new RuleTesterDialog(this, tableModel);
        ruleTesterDialog.setVisible(true);
    }

    public void updateSelectedRowsLabel() {
        int selectedRowCount = table.getSelectedRowCount();
        int totalVisibleRowCount = table.getRowCount();
        int totalRowCount = tableModel.getRowCount();

        double visiblePercentage = 0.0;
        if (totalRowCount > 0) {
            visiblePercentage = (totalVisibleRowCount / (double) totalRowCount) * 100.0;
        }

        selectedRowsLabel.setText(String.format("Selected cases: %d / Total visible cases: %d / Total cases: %d = %.2f%% of dataset",
            selectedRowCount, totalVisibleRowCount, totalRowCount, visiblePercentage));
    }

    public java.util.List<Integer> getSelectedRowsIndices() {
        int[] selectedRows = table.getSelectedRows();
        java.util.List<Integer> selectedIndices = new ArrayList<>();
        for (int row : selectedRows) {
            selectedIndices.add(table.convertRowIndexToModel(row));
        }
        return selectedIndices;
    }

    public JTable getTable() {
        return table;
    }

    public CsvDataHandler getDataHandler() {
        return dataHandler;
    }

    public void updateToggleEasyCasesButton(boolean show) {
        if (show) {
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("/icons/easy.png", 40, 40));
            toggleEasyCasesButton.setToolTipText("Show Easy Cases");
        } else {
            toggleEasyCasesButton.setIcon(UIHelper.loadIcon("/icons/uneasy.png", 40, 40));
            toggleEasyCasesButton.setToolTipText("Show All Cases");
        }
    }

    public void showConcentricCoordinatesPlot() {
        visualizationManager.showConcentricCoordinatesPlot();
    }

    public void applyRowFilter() {
        if (pureRegionManager != null) {
            pureRegionManager.applyRowFilter();
        }
    }

    public Map<String, Color> getClassColors() {
        return stateManager.getClassColors();
    }

    public Map<String, Shape> getClassShapes() {
        return stateManager.getClassShapes();
    }

    public boolean areDifferenceColumnsVisible() {
        return stateManager.areDifferenceColumnsVisible();
    }

    public boolean isHeatmapEnabled() {
        return stateManager.isHeatmapEnabled();
    }

    public Color getCellTextColor() {
        return stateManager.getCellTextColor();
    }

    public boolean isClassColorEnabled() {
        return stateManager.isClassColorEnabled();
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public RendererManager getRendererManager() {
        return rendererManager;
    }

    public PureRegionManager getPureRegionManager() {
        return pureRegionManager;
    }

    public JTextArea getStatsTextArea() {
        return statsTextArea;
    }

    public JSlider getThresholdSlider() {
        return thresholdSlider;
    }

    public void showLineCoordinatesPlot() {
        visualizationManager.showLineCoordinatesPlot();
    }

    public void insertKNNClassification(int k, String metric) {
        KNearestNeighborsClassifier knn = new KNearestNeighborsClassifier(this, tableModel);
        knn.insertKNNClassification(k, metric);
    }

    public void handleNewClass(String newClass) {
        if (!getClassColors().containsKey(newClass)) {
            // Generate a new color for this class using golden ratio for good distribution
            float hue = (getClassColors().size() * 0.618034f) % 1f;
            Color newColor = Color.getHSBColor(hue, 0.8f, 0.9f);
            getClassColors().put(newClass, newColor);
            
            // Generate a new shape for this class
            Shape[] availableShapes = {
                new Ellipse2D.Double(-3, -3, 6, 6),
                new Rectangle2D.Double(-3, -3, 6, 6),
                new Polygon(new int[]{-3, 3, 0}, new int[]{-3, -3, 3}, 3),
                new Polygon(new int[]{0, 3, 0, -3}, new int[]{-3, 0, 3, 0}, 4)
            };
            
            int shapeIndex = getClassShapes().size() % availableShapes.length;
            getClassShapes().put(newClass, availableShapes[shapeIndex]);
        }
    }

    /**
     * Selects all cases that fall within the numerical bounds defined by currently selected cases
     */
    public void selectCasesWithinBounds(boolean requireAllAttributes, List<Integer> selectedColumns) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length < 2) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least two rows to define bounds.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Find min/max bounds for selected numerical columns from the current table state
        Map<Integer, Double> minBounds = new HashMap<>();
        Map<Integer, Double> maxBounds = new HashMap<>();

        // Calculate bounds from the actual table values
        for (int col : selectedColumns) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int rowIndex : selectedRows) {
                try {
                    int modelRow = table.convertRowIndexToModel(rowIndex);
                    double value = Double.parseDouble(table.getModel().getValueAt(modelRow, col).toString());
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
            minBounds.put(col, min);
            maxBounds.put(col, max);
        }

        // Select rows based on the selection mode
        table.clearSelection();
        int matchingRows = 0;
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            if (requireAllAttributes) {
                // ALL mode: every selected attribute must be within its range
                boolean allInRange = true;
                for (int col : selectedColumns) {
                    try {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        double value = Double.parseDouble(tableModel.getValueAt(modelRow, col).toString());
                        if (value < minBounds.get(col) || value > maxBounds.get(col)) {
                            allInRange = false;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        allInRange = false;
                        break;
                    }
                }
                if (allInRange) {
                    table.addRowSelectionInterval(viewRow, viewRow);
                    matchingRows++;
                }
            } else {
                // ANY mode: at least one attribute must be within its range
                for (int col : selectedColumns) {
                    try {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        double value = Double.parseDouble(tableModel.getValueAt(modelRow, col).toString());
                        if (value >= minBounds.get(col) && value <= maxBounds.get(col)) {
                            table.addRowSelectionInterval(viewRow, viewRow);
                            matchingRows++;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        }
        updateSelectedRowsLabel();
    }

    /**
     * Keeps only the cases that fall within the numerical bounds defined by currently selected cases
     */
    public void keepOnlyCasesWithinBounds(boolean requireAllAttributes, List<Integer> selectedColumns) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length < 2) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least two rows to define bounds.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Find min/max bounds for selected numerical columns
        Map<Integer, Double> minBounds = new HashMap<>();
        Map<Integer, Double> maxBounds = new HashMap<>();

        for (int col : selectedColumns) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int rowIndex : selectedRows) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(rowIndex, col).toString());
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
            minBounds.put(col, min);
            maxBounds.put(col, max);
        }

        // Create list of rows to remove
        List<Integer> rowsToRemove = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            boolean keepRow = false;
            
            if (requireAllAttributes) {
                // ALL mode: keep if every selected attribute is within its range
                keepRow = true;
                for (int col : selectedColumns) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                        if (value < minBounds.get(col) || value > maxBounds.get(col)) {
                            keepRow = false;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        keepRow = false;
                        break;
                    }
                }
            } else {
                // ANY mode: keep if at least one attribute is within its range
                for (int col : selectedColumns) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                        if (value >= minBounds.get(col) && value <= maxBounds.get(col)) {
                            keepRow = true;
                            break;  // Found one attribute in range, no need to check others
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            if (!keepRow) {
                rowsToRemove.add(row);
            }
        }

        // Remove rows from bottom to top
        for (int i = rowsToRemove.size() - 1; i >= 0; i--) {
            tableModel.removeRow(rowsToRemove.get(i));
        }

        // Update UI
        table.clearSelection();
        updateSelectedRowsLabel();
        dataHandler.updateStats(tableModel, statsTextArea);
        pureRegionManager.calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }

    public void selectCasesWithinBounds(boolean requireAllAttributes) {
        // Get all numerical columns
        List<Integer> allNumericalColumns = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            try {
                Double.parseDouble(tableModel.getValueAt(0, i).toString());
                allNumericalColumns.add(i);
            } catch (NumberFormatException e) {
                // Skip non-numerical columns
            }
        }
        selectCasesWithinBounds(requireAllAttributes, allNumericalColumns);
    }

    public void keepOnlyCasesWithinBounds(boolean requireAllAttributes) {
        // Get all numerical columns
        List<Integer> allNumericalColumns = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            try {
                Double.parseDouble(tableModel.getValueAt(0, i).toString());
                allNumericalColumns.add(i);
            } catch (NumberFormatException e) {
                // Skip non-numerical columns
            }
        }
        keepOnlyCasesWithinBounds(requireAllAttributes, allNumericalColumns);
    }

    public void keepOnlySelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one row to keep.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view indices to model indices and sort in descending order
        List<Integer> selectedModelRows = new ArrayList<>();
        for (int viewRow : selectedRows) {
            selectedModelRows.add(table.convertRowIndexToModel(viewRow));
        }
        selectedModelRows.sort(Collections.reverseOrder());

        // Create list of rows to remove (all unselected rows)
        List<Integer> rowsToRemove = new ArrayList<>();
        for (int row = tableModel.getRowCount() - 1; row >= 0; row--) {
            if (!selectedModelRows.contains(row)) {
                rowsToRemove.add(row);
            }
        }

        // Remove unselected rows
        for (int row : rowsToRemove) {
            tableModel.removeRow(row);
        }

        // Update UI
        table.clearSelection();
        updateSelectedRowsLabel();
        dataHandler.updateStats(tableModel, statsTextArea);
        pureRegionManager.calculateAndDisplayPureRegions(thresholdSlider.getValue());
    }

    public void showMultiRowParallelCoordinatesPlot() {
        visualizationManager.showMultiRowParallelCoordinatesPlot();
    }

    public void showMultiRowShiftedPairedCoordinatesPlot() {
        visualizationManager.showMultiRowShiftedPairedCoordinatesPlot();
    }

    /**
     * Selects all cases that fall within pure regions (regions where all cases have the same class label)
     * A case is selected if at least one of its attribute values falls within a pure region
     */
    public void selectCasesInPureRegions() {
        int classColumnIndex = getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(this, 
                "No class column found. Please ensure there is a column named 'class'.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get the current threshold value
        int thresholdValue = thresholdSlider.getValue();
        
        // Get pure regions using PureRegionUtils
        java.util.List<src.utils.PureRegionUtils> pureRegions = 
            src.utils.PureRegionUtils.calculatePureRegions(tableModel, thresholdValue, classColumnIndex);
        
        if (pureRegions.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No pure regions found with current threshold (" + thresholdValue + "%).", 
                "No Pure Regions", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Clear current selection
        table.clearSelection();
        
        // To track already selected rows
        java.util.Set<Integer> selectedModelRows = new java.util.HashSet<>();
        
        // Maps to track which cases are selected by each rule
        java.util.Map<src.utils.PureRegionUtils, java.util.Set<Integer>> regionToRows = new java.util.LinkedHashMap<>();
        
        // Initialize the map for each region
        for (src.utils.PureRegionUtils region : pureRegions) {
            regionToRows.put(region, new java.util.HashSet<>());
        }
        
        // For each row, check if it falls within any pure region
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            
            // For each pure region
            for (src.utils.PureRegionUtils region : pureRegions) {
                try {
                    // Get the column index for this attribute
                    int attributeColumnIndex = tableModel.findColumn(region.getAttributeName());
                    if (attributeColumnIndex != -1) {
                        // Get the value for this column and row
                        double value = Double.parseDouble(tableModel.getValueAt(modelRow, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(modelRow, classColumnIndex).toString();
                        
                        // Check if value falls within the pure region and has the matching class
                        if (value >= region.getStart() && value <= region.getEnd() && 
                            className.equals(region.getCurrentClass())) {
                            // Add this row to the region's set of rows
                            regionToRows.get(region).add(modelRow);
                            
                            // If not already selected, select this row
                            if (!selectedModelRows.contains(modelRow)) {
                                table.addRowSelectionInterval(viewRow, viewRow);
                                selectedModelRows.add(modelRow);
                            }
                            // Don't break - we want to check all regions to count which rows they cover
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric values
                }
            }
        }
        
        // Build information about pure regions and their unique contributions
        StringBuilder sb = new StringBuilder();
        sb.append("\nSelected Pure Regions (threshold " + thresholdValue + "%):\n");
        
        java.util.Set<Integer> cumulativeSelectedRows = new java.util.HashSet<>();
        
        for (src.utils.PureRegionUtils region : pureRegions) {
            java.util.Set<Integer> regionRows = regionToRows.get(region);
            java.util.Set<Integer> uniqueRows = new java.util.HashSet<>(regionRows);
            uniqueRows.removeAll(cumulativeSelectedRows);
            
            int uniqueCount = uniqueRows.size();
            cumulativeSelectedRows.addAll(regionRows);
            
            // Format the condition using mathematical notation
            String condition;
            String attributeName = "F(x" + region.getAttributeName().replace(" ", "_") + ")";
            double start = region.getStart();
            double end = region.getEnd();
            
            if (Math.abs(start - end) < 0.0001) {
                // Single point
                condition = String.format("%s == %.4f", attributeName, start);
            } else if (start == Double.MIN_VALUE || start == 0) {
                // Only upper bound
                condition = String.format("%s <= %.4f", attributeName, end);
            } else if (end == Double.MAX_VALUE) {
                // Only lower bound
                condition = String.format("%s >= %.4f", attributeName, start);
            } else {
                // Range with both bounds
                condition = String.format("%.4f <= %s <= %.4f", start, attributeName, end);
            }
            
            // Remove trailing zeros
            condition = condition.replace(".0000", "").replaceAll("([0-9])0+([^0-9]|$)", "$1$2");
            
            sb.append(String.format("%s, Class: %s, Total: %d cases, Unique added: %d cases\n",
                    condition, region.getCurrentClass(), regionRows.size(), uniqueCount));
        }
        
        sb.append(String.format("Total selected cases: %d\n", selectedModelRows.size()));
        statsTextArea.append(sb.toString());
        
        updateSelectedRowsLabel();
    }

    /**
     * Adds a new row containing the mean values of all selected rows.
     * For numeric columns, calculates the arithmetic mean.
     * For non-numeric columns, uses the class column value if available, otherwise leaves blank.
     */
    public void addMeanCase() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one row to calculate mean.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int numColumns = tableModel.getColumnCount();
        Object[] meanRow = new Object[numColumns];
        int classColumnIndex = getClassColumnIndex();
        String className = "";

        // If we have a class column and selected rows, use the class of the first selected row
        if (classColumnIndex != -1 && selectedRows.length > 0) {
            int modelRow = table.convertRowIndexToModel(selectedRows[0]);
            className = tableModel.getValueAt(modelRow, classColumnIndex).toString();
        }

        // Calculate mean for each column
        for (int col = 0; col < numColumns; col++) {
            // Skip class column - we'll handle it separately
            if (col == classColumnIndex) {
                meanRow[col] = className + " (mean)";
                continue;
            }

            double sum = 0;
            int validValueCount = 0;
            boolean isNumeric = true;

            // Sum values for this column across all selected rows
            for (int selectedRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(selectedRow);
                Object value = tableModel.getValueAt(modelRow, col);
                if (value != null) {
                    try {
                        sum += Double.parseDouble(value.toString());
                        validValueCount++;
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }

            // Calculate mean if we have valid values
            if (isNumeric && validValueCount > 0) {
                double mean = sum / validValueCount;
                // Format the mean value to match existing decimal format
                meanRow[col] = formatDecimalWithoutScientificNotation(mean);
            } else {
                // Use empty string for non-numeric columns
                meanRow[col] = "";
            }
        }

        // Add the mean row to the table
        tableModel.addRow(meanRow);
        
        // Update the stats and select the new row
        dataHandler.updateStats(tableModel, statsTextArea);
        int newRowIndex = tableModel.getRowCount() - 1;
        table.setRowSelectionInterval(table.convertRowIndexToView(newRowIndex), table.convertRowIndexToView(newRowIndex));
        
        // Update the status label
        updateSelectedRowsLabel();
    }

    /**
     * Selects the X nearest neighbors for each currently selected case.
     * Uses Euclidean distance to find neighbors in the feature space.
     * 
     * @param neighborsCount The number of neighbors to select for each selected case
     */
    public void selectNearestNeighbors(int neighborsCount) {
        int[] selectedRows = table.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one row to find nearest neighbors.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get all numeric columns (exclude class column)
        int classColumnIndex = getClassColumnIndex();
        List<Integer> numericColumnIndices = new ArrayList<>();
        
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != classColumnIndex) {
                try {
                    // Check if the column is numeric by trying to parse the first value
                    Double.parseDouble(tableModel.getValueAt(0, col).toString());
                    numericColumnIndices.add(col);
                } catch (NumberFormatException e) {
                    // Skip non-numeric columns
                }
            }
        }
        
        if (numericColumnIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No numeric columns found to calculate distances.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if "NN" color is registered, if not, create a new distinctive color for NN class
        if (!stateManager.getClassColors().containsKey("NN")) {
            // Use a bright cyan color that stands out for nearest neighbors
            stateManager.getClassColors().put("NN", new Color(0, 204, 255));
            
            // Also generate a shape for this class
            Shape nnShape = new Ellipse2D.Double(-4, -4, 8, 8);
            stateManager.getClassShapes().put("NN", nnShape);
        }
        
        // Convert selected rows to model indices
        List<Integer> selectedModelRows = new ArrayList<>();
        for (int viewRow : selectedRows) {
            selectedModelRows.add(table.convertRowIndexToModel(viewRow));
        }
        
        // Track all rows to be selected (including original selection)
        Set<Integer> rowsToSelect = new HashSet<>(selectedModelRows);
        Set<Integer> neighborRowsToClassify = new HashSet<>(); // Track rows that will be marked as "NN"
        
        // For each selected row, find its nearest neighbors
        for (int selectedRow : selectedModelRows) {
            // Calculate distances to all other rows
            List<RowDistance> distances = new ArrayList<>();
            
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                // Skip comparing to self
                if (row == selectedRow) continue;
                
                double distance = calculateEuclideanDistance(selectedRow, row, numericColumnIndices);
                distances.add(new RowDistance(row, distance));
            }
            
            // Sort by distance (ascending)
            Collections.sort(distances);
            
            // Add the nearest X neighbors
            int neighborsToAdd = Math.min(neighborsCount, distances.size());
            for (int i = 0; i < neighborsToAdd; i++) {
                int neighborRow = distances.get(i).row;
                rowsToSelect.add(neighborRow);
                
                // Only mark as "NN" if this row isn't from the original selection
                if (!selectedModelRows.contains(neighborRow)) {
                    neighborRowsToClassify.add(neighborRow);
                }
            }
        }
        
        // Set the class of nearest neighbors to "NN"
        if (classColumnIndex != -1) {
            for (int row : neighborRowsToClassify) {
                tableModel.setValueAt("NN", row, classColumnIndex);
            }
        }
        
        // Clear current selection and select all identified rows
        table.clearSelection();
        for (int modelRow : rowsToSelect) {
            int viewRow = table.convertRowIndexToView(modelRow);
            table.addRowSelectionInterval(viewRow, viewRow);
        }
        
        updateSelectedRowsLabel();
        
        // Display info about how many rows were selected
        statsTextArea.append("\nSelected " + rowsToSelect.size() + 
            " rows including " + selectedModelRows.size() + 
            " original rows and " + neighborRowsToClassify.size() + 
            " nearest neighbors marked as 'NN'.\n");
    }
    
    // Helper class to store row index and its distance
    private static class RowDistance implements Comparable<RowDistance> {
        int row;
        double distance;
        
        RowDistance(int row, double distance) {
            this.row = row;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(RowDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    
    /**
     * Calculates Euclidean distance between two rows using only specified columns
     */
    private double calculateEuclideanDistance(int row1, int row2, List<Integer> columns) {
        double sumSquaredDiff = 0.0;
        int validDimensions = 0;
        
        for (int col : columns) {
            try {
                double val1 = Double.parseDouble(tableModel.getValueAt(row1, col).toString());
                double val2 = Double.parseDouble(tableModel.getValueAt(row2, col).toString());
                
                double diff = val1 - val2;
                sumSquaredDiff += diff * diff;
                validDimensions++;
            } catch (NumberFormatException e) {
                // Skip invalid values
            }
        }
        
        // If no valid dimensions were found, return maximum distance
        if (validDimensions == 0) return Double.MAX_VALUE;
        
        return Math.sqrt(sumSquaredDiff);
    }

    /**
     * Sets the decimal precision for formatting numbers and refreshes the table display
     * @param precision Number of decimal places to show
     */
    public void setDecimalPrecision(int precision) {
        stateManager.setDecimalPrecision(precision);
        
        // Format all numeric cells in the table with the new precision
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                Object value = tableModel.getValueAt(row, col);
                if (value != null && isNumeric(value.toString())) {
                    try {
                        double numericValue = Double.parseDouble(value.toString());
                        tableModel.setValueAt(formatDecimalWithoutScientificNotation(numericValue), row, col);
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            }
        }
        
        // Update stats to reflect new precision
        dataHandler.updateStats(tableModel, statsTextArea);
        statsTextArea.append("\nDecimal precision set to " + precision + " places.\n");
    }

    /**
     * Calculates and displays the variance of selected rows as a group of datapoints.
     * Each row is treated as a complete datapoint, and variance is calculated across all selected rows.
     */
    public void calculateSelectedRowsVariance() {
        int[] selectedRows = table.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one row to calculate variance.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (selectedRows.length < 2) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least two rows to calculate variance.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get all numeric columns (exclude class column)
        int classColumnIndex = getClassColumnIndex();
        List<Integer> numericColumnIndices = new ArrayList<>();
        List<String> numericColumnNames = new ArrayList<>();
        
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != classColumnIndex) {
                try {
                    // Check if the column is numeric by trying to parse the first value
                    Double.parseDouble(tableModel.getValueAt(0, col).toString());
                    numericColumnIndices.add(col);
                    numericColumnNames.add(tableModel.getColumnName(col));
                } catch (NumberFormatException e) {
                    // Skip non-numeric columns
                }
            }
        }
        
        if (numericColumnIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No numeric columns found to calculate variance.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Convert selected rows to model indices
        List<Integer> selectedModelRows = new ArrayList<>();
        for (int viewRow : selectedRows) {
            selectedModelRows.add(table.convertRowIndexToModel(viewRow));
        }
        
        // Calculate variance for each row as a complete datapoint
        StringBuilder varianceReport = new StringBuilder();
        varianceReport.append("\n=== VARIANCE ANALYSIS OF SELECTED ROWS ===\n");
        varianceReport.append("Selected rows: ").append(selectedRows.length).append("\n");
        varianceReport.append("Numeric columns per row: ").append(numericColumnIndices.size()).append("\n\n");
        
        // Calculate variance for each row individually
        List<Double> rowVariances = new ArrayList<>();
        List<Double> rowMeans = new ArrayList<>();
        
        for (int i = 0; i < selectedModelRows.size(); i++) {
            int modelRow = selectedModelRows.get(i);
            int viewRow = selectedRows[i];
            
            // Collect all numeric values for this row
            List<Double> rowValues = new ArrayList<>();
            for (int col : numericColumnIndices) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(modelRow, col).toString());
                    // Only add valid numeric values (not NaN or infinite)
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                        rowValues.add(value);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
            
            if (rowValues.size() >= 2) {
                double rowMean = calculateMean(rowValues);
                double rowVariance = calculateVariance(rowValues);
                double rowStdDev = Math.sqrt(rowVariance);
                
                rowMeans.add(rowMean);
                rowVariances.add(rowVariance);
                
                varianceReport.append(String.format("Row %d (View %d):\n", modelRow + 1, viewRow + 1));
                varianceReport.append(String.format("  Mean: %.6f\n", rowMean));
                varianceReport.append(String.format("  Variance: %.6f\n", rowVariance));
                varianceReport.append(String.format("  Standard Deviation: %.6f\n", rowStdDev));
                varianceReport.append(String.format("  Valid attributes: %d/%d\n\n", rowValues.size(), numericColumnIndices.size()));
            } else if (rowValues.size() == 1) {
                varianceReport.append(String.format("Row %d (View %d): Only 1 valid attribute (need at least 2 for variance)\n\n", modelRow + 1, viewRow + 1));
            } else {
                varianceReport.append(String.format("Row %d (View %d): No valid numeric attributes found\n\n", modelRow + 1, viewRow + 1));
            }
        }
        
        // Calculate overall statistics across all selected rows
        if (!rowVariances.isEmpty()) {
            // Filter out NaN values from rowMeans and rowVariances
            List<Double> validRowMeans = new ArrayList<>();
            List<Double> validRowVariances = new ArrayList<>();
            
            for (int i = 0; i < rowMeans.size(); i++) {
                if (!Double.isNaN(rowMeans.get(i)) && !Double.isInfinite(rowMeans.get(i))) {
                    validRowMeans.add(rowMeans.get(i));
                }
                if (!Double.isNaN(rowVariances.get(i)) && !Double.isInfinite(rowVariances.get(i))) {
                    validRowVariances.add(rowVariances.get(i));
                }
            }
            
            if (!validRowMeans.isEmpty()) {
                double overallMean = calculateMean(validRowMeans);
                double overallVariance = calculateVariance(validRowMeans);
                double overallStdDev = Math.sqrt(overallVariance);
                
                varianceReport.append("=== OVERALL STATISTICS ===\n");
                varianceReport.append(String.format("Average of row means: %.6f\n", overallMean));
                varianceReport.append(String.format("Variance of row means: %.6f\n", overallVariance));
                varianceReport.append(String.format("Standard deviation of row means: %.6f\n", overallStdDev));
                
                if (!validRowVariances.isEmpty()) {
                    double avgRowVariance = calculateMean(validRowVariances);
                    double avgRowStdDev = Math.sqrt(avgRowVariance);
                    varianceReport.append(String.format("Average row variance: %.6f\n", avgRowVariance));
                    varianceReport.append(String.format("Average row standard deviation: %.6f\n", avgRowStdDev));
                }
                
                varianceReport.append(String.format("Valid rows analyzed: %d/%d\n", validRowMeans.size(), selectedRows.length));
            } else {
                varianceReport.append("=== OVERALL STATISTICS ===\n");
                varianceReport.append("No valid row means found for overall statistics\n");
                varianceReport.append(String.format("Valid rows analyzed: 0/%d\n", selectedRows.length));
            }
        }
        
        varianceReport.append("==========================================\n");
        
        // Display the results
        statsTextArea.append(varianceReport.toString());
        statsTextArea.setCaretPosition(statsTextArea.getText().length());
    }
    
    /**
     * Calculates the sample variance of a list of values
     * @param values List of numeric values
     * @return Sample variance
     */
    private double calculateVariance(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double mean = calculateMean(values);
        double sumSquaredDiffs = 0.0;
        
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiffs += diff * diff;
        }
        
        // Sample variance (divide by n-1)
        return sumSquaredDiffs / (values.size() - 1);
    }
    
    /**
     * Calculates the mean of a list of values
     * @param values List of numeric values
     * @return Mean value
     */
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        
        return sum / values.size();
    }

    /**
     * Calculates and displays the n-dimensional Area Under Curve (AUC) for selected rows.
     * Each row's attributes are treated as a curve in n-dimensional space.
     */
    public void calculateSelectedRowsPolylineArea() {
        int[] selectedRows = table.getSelectedRows();
        
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select at least one row to calculate AUC.", 
                "Selection Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get all numeric columns (exclude class column)
        int classColumnIndex = getClassColumnIndex();
        List<Integer> numericColumnIndices = new ArrayList<>();
        
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != classColumnIndex) {
                try {
                    // Check if the column is numeric by trying to parse the first value
                    Double.parseDouble(tableModel.getValueAt(0, col).toString());
                    numericColumnIndices.add(col);
                } catch (NumberFormatException e) {
                    // Skip non-numeric columns
                }
            }
        }
        
        if (numericColumnIndices.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No numeric columns found to calculate AUC.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Convert selected rows to model indices
        List<Integer> selectedModelRows = new ArrayList<>();
        for (int viewRow : selectedRows) {
            selectedModelRows.add(table.convertRowIndexToModel(viewRow));
        }
        
        // Calculate n-D AUC for each selected row
        StringBuilder aucReport = new StringBuilder();
        aucReport.append("\n=== N-DIMENSIONAL AUC ANALYSIS ===\n");
        aucReport.append("Selected rows: ").append(selectedRows.length).append("\n");
        aucReport.append("Dimensions: ").append(numericColumnIndices.size()).append("\n\n");
        
        List<Double> rowAUCs = new ArrayList<>();
        
        for (int i = 0; i < selectedModelRows.size(); i++) {
            int modelRow = selectedModelRows.get(i);
            int viewRow = selectedRows[i];
            
            // Collect all numeric values for this row
            List<Double> rowValues = new ArrayList<>();
            for (int col : numericColumnIndices) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(modelRow, col).toString());
                    // Only add valid numeric values (not NaN or infinite)
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                        rowValues.add(value);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
            
            if (rowValues.size() >= 2) {
                // Calculate n-D AUC using trapezoidal rule
                double auc = calculateNDAUC(rowValues);
                rowAUCs.add(auc);
                
                aucReport.append(String.format("Row %d: AUC = %.6f (valid dims: %d/%d)\n", 
                    modelRow + 1, auc, rowValues.size(), numericColumnIndices.size()));
            } else {
                aucReport.append(String.format("Row %d: Insufficient valid dimensions\n", modelRow + 1));
            }
        }
        
        // Calculate overall statistics
        if (!rowAUCs.isEmpty()) {
            // Filter out NaN values
            List<Double> validAUCs = new ArrayList<>();
            for (double auc : rowAUCs) {
                if (!Double.isNaN(auc) && !Double.isInfinite(auc)) {
                    validAUCs.add(auc);
                }
            }
            
            if (!validAUCs.isEmpty()) {
                double avgAUC = calculateMean(validAUCs);
                double totalAUC = validAUCs.stream().mapToDouble(Double::doubleValue).sum();
                double aucVariance = calculateVariance(validAUCs);
                double aucStdDev = Math.sqrt(aucVariance);
                
                aucReport.append("\n=== OVERALL STATISTICS ===\n");
                aucReport.append(String.format("Average AUC: %.6f\n", avgAUC));
                aucReport.append(String.format("Total AUC: %.6f\n", totalAUC));
                aucReport.append(String.format("AUC Variance: %.6f\n", aucVariance));
                aucReport.append(String.format("AUC Std Dev: %.6f\n", aucStdDev));
                aucReport.append(String.format("Valid rows: %d/%d\n", validAUCs.size(), selectedRows.length));
            }
        }
        
        aucReport.append("=====================================\n");
        
        // Display the results
        statsTextArea.append(aucReport.toString());
        statsTextArea.setCaretPosition(statsTextArea.getText().length());
    }
    
    /**
     * Calculates the n-dimensional Area Under Curve (AUC) using trapezoidal rule.
     * @param values List of numeric values representing the curve points
     * @return n-D AUC value
     */
    private double calculateNDAUC(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double auc = 0.0;
        
        // Use trapezoidal rule for n-D AUC
        for (int i = 0; i < values.size() - 1; i++) {
            double y1 = values.get(i);
            double y2 = values.get(i + 1);
            auc += (y1 + y2) / 2.0;
        }
        
        return auc;
    }
}

