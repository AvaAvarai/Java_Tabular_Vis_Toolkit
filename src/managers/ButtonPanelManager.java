package src.managers;

import src.CsvViewer;
import src.UIHelper;

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
        addMenuItem(fileMenu, "Load CSV", "/icons/file.png", e -> csvViewer.loadCsvFile());
        addMenuItem(fileMenu, "Export CSV", "/icons/file.png", e -> csvViewer.exportCsvFile());

        // View Menu (Visualizations)
        JMenu viewMenu = new JMenu("Visualizations");
        viewMenu.setIcon(resizeIcon("/icons/start.png"));
        addMenuItem(viewMenu, "Parallel Coordinates Plot", "/icons/start.png", e -> csvViewer.showParallelCoordinatesPlot());
        addMenuItem(viewMenu, "Shifted Paired Coordinates Plot", "/icons/start.png", e -> csvViewer.showShiftedPairedCoordinates());
        addMenuItem(viewMenu, "Circular/Polygonal Coordinates Plot", "/icons/start.png", e -> csvViewer.showCircularCoordinatesPlot());
        addMenuItem(viewMenu, "Traditional Star Coordinates Plot", "/icons/start.png", e -> csvViewer.showStarCoordinatesPlot());
        addMenuItem(viewMenu, "Concentric Coordinates Plot", "/icons/start.png", e -> csvViewer.showConcentricCoordinatesPlot());
        addMenuItem(viewMenu, "Line Coordinates Plot", "/icons/start.png", e -> csvViewer.showLineCoordinatesPlot());
        addMenuItem(viewMenu, "Decision Tree", "/icons/start.png", e -> csvViewer.showDecisionTreeVisualization());

        // Data Menu
        JMenu dataMenu = new JMenu("Data Operations");
        dataMenu.setIcon(resizeIcon("/icons/normalize.png"));
        
        // Normalization submenu
        JMenu normalizeMenu = new JMenu("Normalize");
        normalizeMenu.setIcon(resizeIcon("/icons/normalize.png"));
        addMenuItem(normalizeMenu, "Min-Max Normalization", "/icons/normalize.png", e -> {
            csvViewer.getStateManager().setNormalizationType("minmax");
            csvViewer.dataHandler.setNormalizationType("minmax");
            csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
            csvViewer.getStateManager().setNormalized(true);
        });
        addMenuItem(normalizeMenu, "Z-Score Normalization", "/icons/normalize.png", e -> {
            csvViewer.getStateManager().setNormalizationType("zscore");
            csvViewer.dataHandler.setNormalizationType("zscore");
            csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
            csvViewer.getStateManager().setNormalized(true);
        });
        dataMenu.add(normalizeMenu);

        // Row operations submenu
        JMenu rowMenu = new JMenu("Row Operations");
        rowMenu.setIcon(resizeIcon("/icons/clone.png"));
        addMenuItem(rowMenu, "Insert Row", "/icons/clone.png", e -> csvViewer.insertRow());
        addMenuItem(rowMenu, "Delete Row", "/icons/clone.png", e -> csvViewer.deleteRow());
        addMenuItem(rowMenu, "Clone Row", "/icons/clone.png", e -> csvViewer.cloneSelectedRow());
        dataMenu.add(rowMenu);

        // Other data operations
        addMenuItem(dataMenu, "Insert Slopes and Distances", "/icons/slopes_distances.png", e -> csvViewer.showCalculateSlopesAndDistancesDialog());

        // Trigonometric Menu
        JMenu trigMenu = new JMenu("Trigonometric Operations");
        trigMenu.setIcon(resizeIcon("/icons/trigon.png"));
        addMenuItem(trigMenu, "Forward Differences", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Backward Differences", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Direct", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        trigMenu.addSeparator();
        addMenuItem(trigMenu, "Inverse Forward Differences", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Inverse Backward Differences", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        addMenuItem(trigMenu, "Inverse Direct", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());
        trigMenu.addSeparator();
        addMenuItem(trigMenu, "Remove Trigonometric Columns", "/icons/trigon.png", e -> csvViewer.toggleTrigonometricColumns());

        // Visualization Options Menu
        JMenu visualizationMenu = new JMenu("Visualization Options");
        visualizationMenu.setIcon(resizeIcon("/icons/heatmap.png"));
        addMenuItem(visualizationMenu, "Toggle Heatmap", "/icons/heatmap.png", e -> csvViewer.toggleHeatmap());
        addMenuItem(visualizationMenu, "Toggle Class Colors", "/icons/heatmap.png", e -> csvViewer.toggleClassColors());
        addMenuItem(visualizationMenu, "Toggle Highlight Blanks", "/icons/heatmap.png", e -> csvViewer.highlightBlanks());
        addMenuItem(visualizationMenu, "Font Settings", "/icons/fontcolor.png", e -> csvViewer.showFontSettingsDialog());
        addMenuItem(visualizationMenu, "Set Class Colors", "/icons/setcolor.png", e -> csvViewer.showColorPickerDialog());

        // Analysis Menu
        JMenu analysisMenu = new JMenu("Analysis Tools");
        analysisMenu.setIcon(resizeIcon("/icons/variance.png"));

        // Machine Learning Menu
        JMenu mlMenu = new JMenu("Classifiers");
        mlMenu.setIcon(resizeIcon("/icons/ml.png"));
        
        addMenuItem(mlMenu, "Support Sum Machine Classifier", "/icons/combo.png", e -> csvViewer.insertWeightedSumColumn());

        JMenuItem knnItem = new JMenuItem("k-Nearest Neighbors Classifier");
        knnItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                showKNNDialog();
            }
        });
        mlMenu.add(knnItem);

        analysisMenu.add(mlMenu);

        addMenuItem(analysisMenu, "Toggle Easy Cases", "/icons/easy.png", e -> csvViewer.toggleEasyCases());
        addMenuItem(analysisMenu, "Show Covariance Matrix", "/icons/variance.png", e -> csvViewer.showCovarianceMatrix());
        addMenuItem(analysisMenu, "Sort Columns by Covariance", "/icons/sort.png", e -> csvViewer.showCovarianceSortDialog());
        addMenuItem(analysisMenu, "Rule Tester", "/icons/rule_tester.png", e -> csvViewer.showRuleTesterDialog());

        // Add all menus to menubar
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(dataMenu);
        menuBar.add(trigMenu);
        menuBar.add(visualizationMenu);
        menuBar.add(analysisMenu);
        menuBar.add(mlMenu);

        return menuBar;
    }

    private void addMenuItem(JMenu menu, String text, String iconPath, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setIcon(resizeIcon(iconPath));
        item.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty() && !text.equals("Load CSV")) {
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
