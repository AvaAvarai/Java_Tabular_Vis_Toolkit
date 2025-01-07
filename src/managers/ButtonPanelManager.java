package src.managers;

import src.CsvViewer;
import src.UIHelper;
import src.classifiers.DecisionTreeClassifier;
import src.classifiers.LinearDiscriminantAnalysisClassifier;
import src.classifiers.PrincipalComponentAnalysisClassifier;
import src.utils.PrincipalComponentAnalysis;
import src.classifiers.RandomForestClassifier;
import src.classifiers.LDAFeatures;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ButtonPanelManager {

    private final CsvViewer csvViewer;
    private JButton toggleButton;

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

        // View Menu (Visualizations)
        JMenu viewMenu = new JMenu("View Visualizations");
        viewMenu.setIcon(resizeIcon("/icons/start.png"));
        addMenuItem(viewMenu, "Parallel Coordinates", "/icons/start.png", _ -> csvViewer.showParallelCoordinatesPlot());
        addMenuItem(viewMenu, "Shifted Paired Coordinates", "/icons/start.png", _ -> csvViewer.showShiftedPairedCoordinates());
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
            csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
            csvViewer.getStateManager().setNormalized(true);
        });
        addMenuItem(normalizeMenu, "Z-Score Normalization", "/icons/normalize.png", _ -> {
            csvViewer.getStateManager().setNormalizationType("zscore");
            csvViewer.dataHandler.setNormalizationType("zscore");
            csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
            csvViewer.getStateManager().setNormalized(true);
        });
        dataMenu.add(normalizeMenu);

        // Row operations submenu
        JMenu rowMenu = new JMenu("Row Operations");
        rowMenu.setIcon(resizeIcon("/icons/clone.png"));
        addMenuItem(rowMenu, "Insert Row", "/icons/clone.png", _ -> csvViewer.insertRow());
        addMenuItem(rowMenu, "Delete Row", "/icons/clone.png", _ -> csvViewer.deleteRow());
        addMenuItem(rowMenu, "Clone Row", "/icons/clone.png", _ -> csvViewer.cloneSelectedRow());
        dataMenu.add(rowMenu);

        // Other data operations
        addMenuItem(dataMenu, "Insert Slopes and Distances", "/icons/slopes_distances.png", _ -> csvViewer.showCalculateSlopesAndDistancesDialog());

        // Feature Engineering Menu
        JMenu featureMenu = new JMenu("Feature Engineering");
        featureMenu.setIcon(resizeIcon("/icons/trigon.png"));

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
        addMenuItem(featureMenu, "Linear Discriminant Analysis", "/icons/ml.png", e -> {
            LDAFeatures lda = new LDAFeatures(csvViewer, csvViewer.tableModel);
            lda.insertLDAComponents();
        });

        // Visualization Options Menu
        JMenu visualizationMenu = new JMenu("Visualization Options");
        visualizationMenu.setIcon(resizeIcon("/icons/heatmap.png"));
        addMenuItem(visualizationMenu, "Toggle Heatmap", "/icons/heatmap.png", _ -> csvViewer.toggleHeatmap());
        addMenuItem(visualizationMenu, "Toggle Class Colors", "/icons/heatmap.png", _ -> csvViewer.toggleClassColors());
        addMenuItem(visualizationMenu, "Toggle Highlight Blanks", "/icons/heatmap.png", _ -> csvViewer.highlightBlanks());
        addMenuItem(visualizationMenu, "Font Settings", "/icons/fontcolor.png", _ -> csvViewer.showFontSettingsDialog());
        addMenuItem(visualizationMenu, "Set Class Colors", "/icons/setcolor.png", _ -> csvViewer.showColorPickerDialog());

        // Analysis Menu
        JMenu analysisMenu = new JMenu("Analysis Tools");
        analysisMenu.setIcon(resizeIcon("/icons/variance.png"));

        // Machine Learning Menu
        JMenu mlMenu = new JMenu("Run ML Classifiers");
        mlMenu.setIcon(resizeIcon("/icons/ml.png"));
        
        addMenuItem(mlMenu, "Support Sum Machine", "/icons/combo.png", _ -> csvViewer.insertWeightedSumColumn());

        JMenuItem knnItem = new JMenuItem("k-Nearest Neighbors");
        knnItem.setIcon(resizeIcon("/icons/knn.png"));
        knnItem.addActionListener(e -> {
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

        addMenuItem(mlMenu, "Random Forest", "/icons/rf.png", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                RandomForestClassifier rf = new RandomForestClassifier(csvViewer, csvViewer.tableModel);
                rf.insertForestClassification();
            }
        });

        analysisMenu.add(mlMenu);

        addMenuItem(analysisMenu, "Toggle Easy Cases", "/icons/easy.png", _ -> csvViewer.toggleEasyCases());
        addMenuItem(analysisMenu, "Show Covariance Matrix", "/icons/variance.png", _ -> csvViewer.showCovarianceMatrix());
        addMenuItem(analysisMenu, "Sort Columns by Covariance", "/icons/sort.png", _ -> csvViewer.showCovarianceSortDialog());
        addMenuItem(analysisMenu, "Rule Tester", "/icons/rule_tester.png", _ -> csvViewer.showRuleTesterDialog());

        // Add all menus to menubar
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(dataMenu);
        menuBar.add(featureMenu);
        menuBar.add(visualizationMenu);
        menuBar.add(analysisMenu);
        menuBar.add(mlMenu);

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
}
