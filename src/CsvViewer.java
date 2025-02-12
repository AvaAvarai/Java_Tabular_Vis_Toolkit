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
    
        setTitle("CSV Viewer");
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
        DecimalFormat decimalFormat = new DecimalFormat("#.##########################");
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
            if (columnName.equalsIgnoreCase("class")) {
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
}

