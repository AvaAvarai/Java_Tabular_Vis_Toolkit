package src.managers;

import src.CsvViewer;
import src.UIHelper;
import src.classifiers.DecisionTreeClassifier;
import src.classifiers.LinearDiscriminantAnalysisClassifier;
import src.classifiers.LinearRegressionClassifier;
import src.classifiers.PrincipalComponentAnalysisClassifier;
import src.utils.LinearDiscriminantAnalysis;
import src.utils.PrincipalComponentAnalysis;
import src.classifiers.RandomForestClassifier;
import src.utils.SequentialSlopeFeatures;
import src.utils.SequentialDistanceFeatures;
import src.utils.SlopeAndDistanceFeatures;
import javax.swing.table.DefaultTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.awt.geom.Ellipse2D;
import java.awt.Shape;
import java.util.stream.IntStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import src.classifiers.MultiLayerPerceptronClassifier;

public class ButtonPanelManager {

    private final CsvViewer csvViewer;
    private JButton toggleButton;
    private float currentHue = 0.42f;

    public ButtonPanelManager(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
        this.toggleButton = new JButton();
    }

    public JButton getToggleButton() {
        return toggleButton;
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setIcon(resizeIcon("/icons/file.png"));
        addMenuItem(fileMenu, "Open Data", "/icons/file.png", _ -> csvViewer.loadCsvFile());
        addMenuItem(fileMenu, "Save Data", "/icons/export.png", _ -> csvViewer.exportCsvFile());
        addMenuItem(fileMenu, "Close Data", "/icons/delete.png", _ -> csvViewer.closeData());

        // View Menu (Visualizations)
        JMenu viewMenu = new JMenu("View Visualizations");
        viewMenu.setIcon(resizeIcon("/icons/start.png"));
        addMenuItem(viewMenu, "Parallel Coordinates", "/icons/start.png", _ -> csvViewer.showParallelCoordinatesPlot());
        addMenuItem(viewMenu, "Multi-Row Parallel Coordinates", "/icons/start.png", _ -> csvViewer.showMultiRowParallelCoordinatesPlot());
        addMenuItem(viewMenu, "Shifted Paired Coordinates", "/icons/start.png", _ -> csvViewer.showShiftedPairedCoordinates());
        addMenuItem(viewMenu, "Multi-Row Shifted Paired Coordinates", "/icons/start.png", _ -> csvViewer.showMultiRowShiftedPairedCoordinatesPlot());
        addMenuItem(viewMenu, "Collocated Paired Coordinates", "/icons/start.png", _ -> csvViewer.showCollocatedPairedCoordinates());
        addMenuItem(viewMenu, "Circular/Polygonal Coordinates", "/icons/start.png", _ -> csvViewer.showCircularCoordinatesPlot());
        addMenuItem(viewMenu, "Traditional Star Coordinates", "/icons/start.png", _ -> csvViewer.showStarCoordinatesPlot());
        addMenuItem(viewMenu, "Concentric Coordinates", "/icons/start.png", _ -> csvViewer.showConcentricCoordinatesPlot());
        addMenuItem(viewMenu, "Line Coordinates", "/icons/start.png", _ -> csvViewer.showLineCoordinatesPlot());
        addMenuItem(viewMenu, "Decision Tree", "/icons/start.png", _ -> csvViewer.showDecisionTreeVisualization());
        
        // Data Menu
        JMenu dataMenu = new JMenu("Data Operations");
        dataMenu.setIcon(resizeIcon("/icons/normalize.png"));
        
        // Normalization submenu
        JMenu normalizeMenu = new JMenu("Normalize");
        normalizeMenu.setIcon(resizeIcon("/icons/normalize.png"));
        addMenuItem(normalizeMenu, "Min-Max Normalization", "/icons/normalize.png", _ -> {
            csvViewer.getStateManager().setNormalizationType("minmax");
            csvViewer.dataHandler.setNormalizationType("minmax");
            csvViewer.dataHandler.toggleNormalization(csvViewer.getTable(), csvViewer.getStatsTextArea());
        });
        addMenuItem(normalizeMenu, "Z-Score Normalization", "/icons/normalize.png", _ -> {
            csvViewer.getStateManager().setNormalizationType("zscore");
            csvViewer.dataHandler.setNormalizationType("zscore");
            csvViewer.dataHandler.toggleNormalization(csvViewer.getTable(), csvViewer.getStatsTextArea());
        });
        normalizeMenu.addSeparator();
        addMenuItem(normalizeMenu, "Denormalize Data", "/icons/normalize.png", _ -> {
            if (csvViewer.dataHandler.isDataNormalized()) {
                csvViewer.dataHandler.toggleNormalization(csvViewer.getTable(), csvViewer.getStatsTextArea());
                csvViewer.getStateManager().setNormalized(false);
            }
        });
        dataMenu.add(normalizeMenu);

        // Row operations submenu
        JMenu rowMenu = new JMenu("Row Operations");
        rowMenu.setIcon(resizeIcon("/icons/clone.png"));
        addMenuItem(rowMenu, "Insert Row", "/icons/clone.png", _ -> csvViewer.insertRow());
        addMenuItem(rowMenu, "Delete Row", "/icons/clone.png", _ -> csvViewer.deleteRow());
        addMenuItem(rowMenu, "Clone Row", "/icons/clone.png", _ -> csvViewer.cloneSelectedRow());
        rowMenu.addSeparator();
        addMenuItem(rowMenu, "Revert Rows to Original", "/icons/undo.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                int choice = JOptionPane.showConfirmDialog(csvViewer,
                    "This will revert all rows to their original state but keep added columns.\nContinue?",
                    "Revert Rows", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    DefaultTableModel model = csvViewer.tableModel;
                    List<List<String>> originalData = csvViewer.getStateManager().getOriginalData();
                    
                    // Reset row count to original
                    model.setRowCount(originalData.size());
                    
                    // Restore original data for existing columns
                    List<String> originalColumns = csvViewer.getStateManager().getOriginalColumnNames();
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        String colName = model.getColumnName(col);
                        if (originalColumns.contains(colName)) {
                            int origColIndex = originalColumns.indexOf(colName);
                            for (int row = 0; row < originalData.size(); row++) {
                                model.setValueAt(originalData.get(row).get(origColIndex), row, col);
                            }
                        }
                    }
                }
            }
        });
        dataMenu.add(rowMenu);

        // Feature Engineering Menu
        JMenu featureMenu = new JMenu("Feature Engineering");
        featureMenu.setIcon(resizeIcon("/icons/trigon.png"));

        // Add "Remove Engineered Columns" option at the top
        addMenuItem(featureMenu, "Remove Engineered Columns", "/icons/undo.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                List<String> originalColumns = csvViewer.getStateManager().getOriginalColumnNames();
                DefaultTableModel model = csvViewer.tableModel;
                
                // Remove specific non-original columns by name
                for (int i = model.getColumnCount() - 1; i >= 0; i--) {
                    String colName = model.getColumnName(i);
                    if (!originalColumns.contains(colName)) {
                        // Remove this specific column
                        for (int row = 0; row < model.getRowCount(); row++) {
                            model.setValueAt(null, row, i);
                        }
                        // Shift remaining columns left
                        for (int col = i; col < model.getColumnCount() - 1; col++) {
                            for (int row = 0; row < model.getRowCount(); row++) {
                                model.setValueAt(model.getValueAt(row, col + 1), row, col);
                            }
                        }
                        model.setColumnCount(model.getColumnCount() - 1);
                    }
                }
            }
        });
        
        featureMenu.addSeparator();

        // Trigonometric submenu
        JMenu trigMenu = new JMenu("Trigonometric Operations");
        trigMenu.setIcon(resizeIcon("/icons/trigon.png"));
        addMenuItem(trigMenu, "Forward Differences", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Backward Differences", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Direct", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        trigMenu.addSeparator();
        addMenuItem(trigMenu, "Inverse Forward Differences", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Inverse Backward Differences", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Inverse Direct", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        trigMenu.addSeparator();
        addMenuItem(trigMenu, "Remove Trigonometric Columns", "/icons/trigon.png", _ -> csvViewer.toggleTrigonometricColumns());
        featureMenu.add(trigMenu);

        // Add PCA to Feature Engineering menu
        addMenuItem(featureMenu, "Principal Component Analysis", "/icons/ml.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis(csvViewer, csvViewer.tableModel);
                pca.insertPrincipalComponents();
            }
        });

        // Add LDA Features to Feature Engineering menu
        addMenuItem(featureMenu, "Linear Discriminant Analysis", "/icons/ml.png", _ -> {
            LinearDiscriminantAnalysis lda = new LinearDiscriminantAnalysis(csvViewer, csvViewer.tableModel);
            lda.insertLDAComponents();
        });

        // Add Slopes and Distances to Feature Engineering menu
        addMenuItem(featureMenu, "Calculate Slopes and Distances", "/icons/rule.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                SlopeAndDistanceFeatures features = new SlopeAndDistanceFeatures(
                    csvViewer, csvViewer.tableModel, csvViewer.getTable());
                features.showDimensionDialog();
            }
        });

        // In Feature Engineering menu
        addMenuItem(featureMenu, "Calculate Sequential Slopes", "/icons/rule.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                SequentialSlopeFeatures slopes = new SequentialSlopeFeatures(
                    csvViewer, csvViewer.tableModel, csvViewer.getTable());
                slopes.calculateSlopes();
            }
        });

        addMenuItem(featureMenu, "Calculate Sequential Distances", "/icons/rule.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                SequentialDistanceFeatures distances = new SequentialDistanceFeatures(
                    csvViewer, csvViewer.tableModel, csvViewer.getTable());
                distances.calculateDistances();
            }
        });

        // Visualization Options Menu
        JMenu visualizationMenu = new JMenu("Visualization Options");
        visualizationMenu.setIcon(resizeIcon("/icons/heatmap.png"));
        addMenuItem(visualizationMenu, "Toggle Heatmap", "/icons/heatmap.png", _ -> csvViewer.toggleHeatmap());
        addMenuItem(visualizationMenu, "Toggle Class Colors", "/icons/heatmap.png", _ -> csvViewer.toggleClassColors());
        addMenuItem(visualizationMenu, "Toggle Highlight Blanks", "/icons/heatmap.png", _ -> csvViewer.highlightBlanks());
        addMenuItem(visualizationMenu, "Font Settings", "/icons/fontcolor.png", _ -> csvViewer.showFontSettingsDialog());
        addMenuItem(visualizationMenu, "Set Class Colors", "/icons/setcolor.png", _ -> csvViewer.showColorPickerDialog());
        
        // Add decimal precision submenu
        JMenu precisionMenu = new JMenu("Set Decimal Precision");
        precisionMenu.setIcon(resizeIcon("/icons/function.png"));
        
        // Add radio buttons for precision options
        ButtonGroup precisionGroup = new ButtonGroup();
        
        for (int i = 0; i <= 6; i++) {
            final int precision = i;
            JRadioButtonMenuItem precisionItem = new JRadioButtonMenuItem(i + " decimal places");
            
            // Check the current precision setting
            if (precision == csvViewer.getStateManager().getDecimalPrecision()) {
                precisionItem.setSelected(true);
            }
            
            precisionItem.addActionListener(_ -> csvViewer.setDecimalPrecision(precision));
            precisionGroup.add(precisionItem);
            precisionMenu.add(precisionItem);
        }
        
        visualizationMenu.add(precisionMenu);

        // Analysis Menu
        JMenu analysisMenu = new JMenu("Analysis Tools");
        analysisMenu.setIcon(resizeIcon("/icons/variance.png"));

        // Machine Learning Menu
        JMenu mlMenu = new JMenu("Run ML Classifiers");
        mlMenu.setIcon(resizeIcon("/icons/ml.png"));

        JMenuItem knnItem = new JMenuItem("k-Nearest Neighbors");
        knnItem.setIcon(resizeIcon("/icons/knn.png"));
        knnItem.addActionListener(_ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                showKNNDialog();
            }
        });
        mlMenu.add(knnItem);

        addMenuItem(mlMenu, "Linear Discriminant Analysis", "/icons/lda.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                LinearDiscriminantAnalysisClassifier lda = new LinearDiscriminantAnalysisClassifier(csvViewer, csvViewer.tableModel);
                lda.insertLDAClassification();
            }
        });

        addMenuItem(mlMenu, "Decision Tree", "/icons/dt.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                DecisionTreeClassifier dt = new DecisionTreeClassifier(csvViewer, csvViewer.tableModel);
                dt.insertTreeClassification();
            }
        });

        addMenuItem(mlMenu, "Principal Component Analysis", "/icons/ml.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                PrincipalComponentAnalysisClassifier pcaClassifier = new PrincipalComponentAnalysisClassifier(csvViewer, csvViewer.tableModel);
                pcaClassifier.insertPCAClassification();
            }
        });

        addMenuItem(mlMenu, "Random Forest", "/icons/rf.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                RandomForestClassifier rf = new RandomForestClassifier(csvViewer, csvViewer.tableModel);
                rf.insertForestClassification();
            }
        });

        addMenuItem(mlMenu, "Support Sum Machine", "/icons/combo.png", _ -> csvViewer.insertWeightedSumColumn());

        addMenuItem(mlMenu, "Linear Regression", "/icons/lda.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                LinearRegressionClassifier lr = new LinearRegressionClassifier(csvViewer, csvViewer.tableModel);
                lr.insertLinearRegression();
            }
        });

        addMenuItem(mlMenu, "Multi-Layer Perceptron", "/icons/mlp.png", _ -> {
            MultiLayerPerceptronClassifier mlp = new MultiLayerPerceptronClassifier(csvViewer, csvViewer.tableModel);
            mlp.insertMLPClassification();
        });

        analysisMenu.add(mlMenu);

        addMenuItem(analysisMenu, "Toggle Easy Cases", "/icons/easy.png", _ -> csvViewer.toggleEasyCases());
        addMenuItem(analysisMenu, "Show Covariance Matrix", "/icons/variance.png", _ -> csvViewer.showCovarianceMatrix());
        addMenuItem(analysisMenu, "Sort Columns by Covariance", "/icons/sort.png", _ -> csvViewer.showCovarianceSortDialog());
        addMenuItem(analysisMenu, "Rule Tester", "/icons/rule.png", _ -> csvViewer.showRuleTesterDialog());

        addMenuItem(analysisMenu, "Add Mean Case", "/icons/clone.png", _ -> csvViewer.addMeanCase());

        addMenuItem(analysisMenu, "Select Nearest Neighbors", "/icons/knn.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
                return;
            }
            showNearestNeighborsDialog();
        });

        addMenuItem(analysisMenu, "Insert Noise Cases", "/icons/variance.png", 
            _ -> {
                if (csvViewer.dataHandler.isDataEmpty()) {
                    csvViewer.noDataLoadedError();
                    return;
                }
                showNoiseDialog();
            });

        addMenuItem(analysisMenu, "Insert Linear Function", "/icons/function.png", _ -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
                return;
            }
            showLinearFunctionDialog();
        });

        // Add all menus to menubar
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(dataMenu);
        menuBar.add(featureMenu);
        menuBar.add(visualizationMenu);
        menuBar.add(analysisMenu);
        menuBar.add(mlMenu);

        JMenu selectionMenu = new JMenu("Selection");
        selectionMenu.setIcon(resizeIcon("/icons/file.png"));

        addMenuItem(selectionMenu, "Keep Only Selected Rows", "/icons/file.png", 
            _ -> csvViewer.keepOnlySelectedRows());
        addMenuItem(selectionMenu, "Select Cases Within Bounds...", "/icons/file.png", 
            _ -> showAttributeSelectionDialog(false));
        addMenuItem(selectionMenu, "Keep Only Cases Within Bounds...", "/icons/file.png", 
            _ -> showAttributeSelectionDialog(true));
        addMenuItem(selectionMenu, "Select Cases in Pure Regions", "/icons/easy.png", 
            _ -> csvViewer.selectCasesInPureRegions());

        menuBar.add(selectionMenu);

        return menuBar;
    }

    private void addMenuItem(JMenu menu, String text, String iconPath, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setIcon(resizeIcon(iconPath));
        item.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty() && !text.equals("Open Data")) {
                csvViewer.noDataLoadedError();
            } else {
                listener.actionPerformed(e);
            }
        });
        menu.add(item);
    }

    private ImageIcon resizeIcon(String path) {
        return UIHelper.loadIcon(path, 16, 16); // Small 16x16 icons for menu items
    }

    private void showKNNDialog() {
        JDialog dialog = new JDialog((Frame)null, "k-NN Parameters", true);
        dialog.setLayout(new BorderLayout(5,5));
        
        JPanel panel = new JPanel(new GridLayout(0,2,5,5));
        
        // K value spinner
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(3, 1, 100, 2);
        JSpinner kSpinner = new JSpinner(spinnerModel);
        panel.add(new JLabel("Number of neighbors (k):"));
        panel.add(kSpinner);
        
        // Distance metric combo box
        String[] metrics = {"Euclidean", "Manhattan"};
        JComboBox<String> metricBox = new JComboBox<>(metrics);
        panel.add(new JLabel("Distance metric:"));
        panel.add(metricBox);
        
        dialog.add(panel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int k = (Integer)kSpinner.getValue();
            String metric = (String)metricBox.getSelectedItem();
            csvViewer.insertKNNClassification(k, metric);
            dialog.dispose();
        });
        buttonPanel.add(okButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void showAttributeSelectionDialog(boolean keepMode) {
        JDialog dialog = new JDialog((Frame)null, "Select Attributes", true);
        dialog.setLayout(new BorderLayout(5,5));
        
        // Create panel for checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        
        // Get column names and create checkboxes
        List<JCheckBox> checkboxes = new ArrayList<>();
        for (int i = 0; i < csvViewer.tableModel.getColumnCount(); i++) {
            String colName = csvViewer.tableModel.getColumnName(i);
            // Skip non-numerical columns
            try {
                Double.parseDouble(csvViewer.tableModel.getValueAt(0, i).toString());
                JCheckBox cb = new JCheckBox(colName);
                cb.setSelected(true); // Default to selected
                checkboxes.add(cb);
                checkboxPanel.add(cb);
            } catch (NumberFormatException e) {
                // Skip non-numerical columns
            }
        }
        
        // Add radio buttons for matching mode
        JPanel matchPanel = new JPanel();
        matchPanel.setBorder(BorderFactory.createTitledBorder("Matching Mode"));
        ButtonGroup group = new ButtonGroup();
        JRadioButton anyMatch = new JRadioButton("Match ANY selected attribute");
        JRadioButton allMatch = new JRadioButton("Match ALL selected attributes");
        anyMatch.setSelected(true); // Default to ANY
        group.add(anyMatch);
        group.add(allMatch);
        matchPanel.add(anyMatch);
        matchPanel.add(allMatch);
        
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(checkboxPanel), BorderLayout.CENTER);
        contentPanel.add(matchPanel, BorderLayout.SOUTH);
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            List<Integer> selectedColumns = new ArrayList<>();
            for (int i = 0; i < checkboxes.size(); i++) {
                if (checkboxes.get(i).isSelected()) {
                    selectedColumns.add(i);
                }
            }
            if (selectedColumns.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please select at least one attribute.", 
                    "Selection Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean requireAllAttributes = allMatch.isSelected();
            if (keepMode) {
                csvViewer.keepOnlyCasesWithinBounds(requireAllAttributes, selectedColumns);
            } else {
                csvViewer.selectCasesWithinBounds(requireAllAttributes, selectedColumns);
            }
            dialog.dispose();
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void showNoiseDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), 
            "Insert Noise Cases");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Number of cases panel
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(10, 1, 1000, 1);
        JSpinner countSpinner = new JSpinner(spinnerModel);
        countPanel.add(new JLabel("Number of noise cases:"));
        countPanel.add(countSpinner);
        mainPanel.add(countPanel);

        // Distribution selection panel
        JPanel distPanel = new JPanel();
        distPanel.setLayout(new BoxLayout(distPanel, BoxLayout.Y_AXIS));
        distPanel.setBorder(BorderFactory.createTitledBorder("Distribution"));

        ButtonGroup group = new ButtonGroup();
        JRadioButton gaussianButton = new JRadioButton("Gaussian (Normal) Distribution", true);
        JRadioButton oneClassButton = new JRadioButton("Sample from One Class Distribution", false);
        JRadioButton allClassesButton = new JRadioButton("Sample from All Classes Distribution", false);
        
        group.add(gaussianButton);
        group.add(oneClassButton);
        group.add(allClassesButton);

        // Class selection combo box (for One Class option)
        Set<String> uniqueClasses = new HashSet<>();
        int classCol = csvViewer.getClassColumnIndex();
        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            uniqueClasses.add(csvViewer.tableModel.getValueAt(row, classCol).toString());
        }
        JComboBox<String> classBox = new JComboBox<>(uniqueClasses.toArray(new String[0]));
        classBox.setEnabled(false);

        oneClassButton.addActionListener(e -> classBox.setEnabled(true));
        gaussianButton.addActionListener(e -> classBox.setEnabled(false));
        allClassesButton.addActionListener(e -> classBox.setEnabled(false));

        distPanel.add(gaussianButton);
        distPanel.add(oneClassButton);
        JPanel classPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        classPanel.add(new JLabel("    Class:"));
        classPanel.add(classBox);
        distPanel.add(classPanel);
        distPanel.add(allClassesButton);

        mainPanel.add(distPanel);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            int numCases = (int)countSpinner.getValue();
            String distribution = gaussianButton.isSelected() ? "gaussian" :
                                oneClassButton.isSelected() ? "oneclass" : "allclasses";
            String selectedClass = (String)classBox.getSelectedItem();
            insertNoiseCases(numCases, distribution, selectedClass);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer);
        dialog.setVisible(true);
    }

    private void insertNoiseCases(int numCases, String distribution, String selectedClass) {
        boolean isNormalized = csvViewer.dataHandler.isDataNormalized();
        int classColumnIndex = csvViewer.getClassColumnIndex();
        
        // Get class-specific data if needed
        Map<String, List<Integer>> classRows = new HashMap<>();
        if (!distribution.equals("gaussian")) {
            for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                String classValue = csvViewer.tableModel.getValueAt(row, classColumnIndex).toString();
                classRows.computeIfAbsent(classValue, k -> new ArrayList<>()).add(row);
            }
        }
        
        // Calculate distribution stats for each numeric column
        Map<Integer, DistributionStats> columnStats = new HashMap<>();
        for (int col = 0; col < csvViewer.tableModel.getColumnCount(); col++) {
            if (col == classColumnIndex) continue;
            
            try {
                List<Integer> rowsToProcess;
                if (distribution.equals("oneclass")) {
                    rowsToProcess = classRows.get(selectedClass);
                } else if (distribution.equals("allclasses")) {
                    rowsToProcess = IntStream.range(0, csvViewer.tableModel.getRowCount())
                        .boxed().collect(Collectors.toList());
                } else { // gaussian
                    rowsToProcess = csvViewer.getTable().getSelectedRowCount() > 1 ?
                        Arrays.stream(csvViewer.getTable().getSelectedRows())
                            .mapToObj(row -> csvViewer.getTable().convertRowIndexToModel(row))
                            .collect(Collectors.toList()) :
                        IntStream.range(0, csvViewer.tableModel.getRowCount())
                            .boxed().collect(Collectors.toList());
                }
                
                // Get all values for this column
                List<Double> values = new ArrayList<>();
                for (int row : rowsToProcess) {
                    values.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, col).toString()));
                }
                
                // Calculate distribution stats
                double min = values.stream().mapToDouble(d -> d).min().getAsDouble();
                double max = values.stream().mapToDouble(d -> d).max().getAsDouble();
                
                // Create histogram bins
                int numBins = (int)Math.sqrt(values.size());  // Sturges' formula
                double[] bins = new double[numBins];
                int[] counts = new int[numBins];
                double binWidth = (max - min) / numBins;
                
                // Fill bins
                for (double value : values) {
                    int binIndex = Math.min(numBins - 1, (int)((value - min) / binWidth));
                    counts[binIndex]++;
                    bins[binIndex] += value;
                }
                
                // Calculate average value in each bin
                for (int i = 0; i < numBins; i++) {
                    if (counts[i] > 0) {
                        bins[i] /= counts[i];
                    } else {
                        bins[i] = min + (i + 0.5) * binWidth;
                    }
                }
                
                columnStats.put(col, new DistributionStats(bins, counts, min, max));
                
            } catch (NumberFormatException e) {
                // Skip non-numeric columns
            }
        }
        
        // Generate cases using the calculated distributions
        Random random = new Random();
        for (int i = 0; i < numCases; i++) {
            Object[] rowData = new Object[csvViewer.tableModel.getColumnCount()];
            
            // Handle class and colors...
            String syntheticClass;
            switch (distribution) {
                case "gaussian":
                    syntheticClass = "NOISE";
                    break;
                case "oneclass":
                    syntheticClass = selectedClass + "-synthetic";
                    break;
                default: // allclasses
                    syntheticClass = "ALL-synthetic";  // One single synthetic class for all-classes distribution
                    break;
            }
            
            if (!csvViewer.getClassColors().containsKey(syntheticClass)) {
                csvViewer.getClassColors().put(syntheticClass, Color.getHSBColor(currentHue, 0.8f, 0.9f));
                Shape shape = new Ellipse2D.Double(-3, -3, 6, 6);
                csvViewer.getClassShapes().put(syntheticClass, shape);
                currentHue = (currentHue + 0.618034f) % 1f;
            }
            rowData[classColumnIndex] = syntheticClass;
            
            // Generate values according to density distribution
            for (int col = 0; col < csvViewer.tableModel.getColumnCount(); col++) {
                if (col != classColumnIndex && columnStats.containsKey(col)) {
                    DistributionStats stats = columnStats.get(col);
                    double value;

                    if (distribution.equals("gaussian")) {
                        // For gaussian, use proper normal distribution
                        double mean = (stats.max + stats.min) / 2.0;
                        double stdDev = (stats.max - stats.min) / 6.0;  // So ~99.7% falls within range
                        value = random.nextGaussian() * stdDev + mean;
                    } else {
                        // For class distributions, use histogram sampling
                        int totalCount = Arrays.stream(stats.counts).sum();
                        int target = random.nextInt(totalCount);
                        int sum = 0;
                        int selectedBin = 0;
                        for (int bin = 0; bin < stats.counts.length; bin++) {
                            sum += stats.counts[bin];
                            if (sum > target) {
                                selectedBin = bin;
                                break;
                            }
                        }
                        
                        double binValue = stats.bins[selectedBin];
                        double binWidth = (stats.max - stats.min) / stats.bins.length;
                        value = binValue + (random.nextDouble() - 0.5) * binWidth;
                    }
                    
                    if (isNormalized) {
                        value = Math.min(1.0, Math.max(0.0, value));
                    }
                    rowData[col] = String.format("%.4f", value);
                }
            }
            
            csvViewer.tableModel.addRow(rowData);
        }
        
        // Update UI
        csvViewer.getDataHandler().updateStats(csvViewer.tableModel, csvViewer.getStatsTextArea());
        csvViewer.getTable().repaint();
    }

    private static class DistributionStats {
        final double[] bins;
        final int[] counts;
        final double min;
        final double max;
        
        DistributionStats(double[] bins, int[] counts, double min, double max) {
            this.bins = bins;
            this.counts = counts;
            this.min = min;
            this.max = max;
        }
    }

    private void showLinearFunctionDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), 
            "Insert Linear Function");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Intercept panel
        JPanel interceptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        interceptPanel.add(new JLabel("Intercept:"));
        JTextField interceptField = new JTextField("0.0", 10);
        interceptPanel.add(interceptField);
        mainPanel.add(interceptPanel);

        // Attributes panel
        JPanel attributesPanel = new JPanel();
        attributesPanel.setLayout(new BoxLayout(attributesPanel, BoxLayout.Y_AXIS));
        attributesPanel.setBorder(BorderFactory.createTitledBorder("Attributes"));

        List<JCheckBox> checkboxes = new ArrayList<>();
        List<JTextField> coefficientFields = new ArrayList<>();

        // Add row for each numeric attribute
        for (int i = 0; i < csvViewer.tableModel.getColumnCount(); i++) {
            String colName = csvViewer.tableModel.getColumnName(i);
            if (isNumericColumn(i) && !colName.equalsIgnoreCase("class")) {
                JPanel attrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                
                JCheckBox checkbox = new JCheckBox(colName, true);
                checkboxes.add(checkbox);
                attrPanel.add(checkbox);
                
                JTextField coeffField = new JTextField("0.0", 10);
                coefficientFields.add(coeffField);
                attrPanel.add(coeffField);
                
                attributesPanel.add(attrPanel);
            }
        }

        JScrollPane scrollPane = new JScrollPane(attributesPanel);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        mainPanel.add(scrollPane);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            try {
                double intercept = Double.parseDouble(interceptField.getText());
                StringBuilder formula = new StringBuilder();
                formula.append("f(x) = ").append(String.format("%.3f", intercept));
                
                // Create new column with computed values
                DefaultTableModel model = csvViewer.tableModel;
                int rowCount = model.getRowCount();
                double[] results = new double[rowCount];
                
                // Initialize with intercept
                Arrays.fill(results, intercept);
                
                // Add each selected attribute's contribution
                for (int i = 0; i < checkboxes.size(); i++) {
                    if (checkboxes.get(i).isSelected()) {
                        String attrName = checkboxes.get(i).getText();
                        double coeff = Double.parseDouble(coefficientFields.get(i).getText());
                        
                        if (coeff != 0.0) {
                            formula.append(" + ")
                                   .append(String.format("%.3f", coeff))
                                   .append(attrName);
                            
                            int colIndex = model.findColumn(attrName);
                            for (int row = 0; row < rowCount; row++) {
                                double value = Double.parseDouble(model.getValueAt(row, colIndex).toString());
                                results[row] += coeff * value;
                            }
                        }
                    }
                }
                
                // Add the new column
                model.addColumn(formula.toString());
                int newColIndex = model.getColumnCount() - 1;
                
                // If data is normalized, normalize the results
                if (csvViewer.dataHandler.isDataNormalized()) {
                    double min = Arrays.stream(results).min().getAsDouble();
                    double max = Arrays.stream(results).max().getAsDouble();
                    double range = max - min;
                    
                    // If all values are the same (range = 0), set all values to 0.5
                    if (range == 0) {
                        Arrays.fill(results, 0.0);  // When all inputs are 0, output should be 0
                    } else {
                        for (int row = 0; row < rowCount; row++) {
                            results[row] = (results[row] - min) / range;
                        }
                    }
                }
                
                // Fill in the values
                DecimalFormat df = new DecimalFormat("#.###");
                for (int row = 0; row < rowCount; row++) {
                    model.setValueAt(df.format(results[row]), row, newColIndex);
                }
                
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter valid numeric values for coefficients.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer);
        dialog.setVisible(true);
    }

    private boolean isNumericColumn(int columnIndex) {
        try {
            Double.parseDouble(csvViewer.tableModel.getValueAt(0, columnIndex).toString());
            return true;
            } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showNearestNeighborsDialog() {
        JDialog dialog = new JDialog((Frame)null, "Nearest Neighbors Selection", true);
        dialog.setLayout(new BorderLayout(5,5));
        
        JPanel panel = new JPanel(new GridLayout(0,2,5,5));
        
        // Create spinner for number of neighbors
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 100, 1);
        JSpinner neighborSpinner = new JSpinner(spinnerModel);
        panel.add(new JLabel("Number of neighbors per selected case:"));
        panel.add(neighborSpinner);
        
        dialog.add(panel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int neighborsCount = (Integer)neighborSpinner.getValue();
            csvViewer.selectNearestNeighbors(neighborsCount);
            dialog.dispose();
        });
        buttonPanel.add(okButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
