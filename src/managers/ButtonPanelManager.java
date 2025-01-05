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
    }

    public JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(createFileMenu());
        buttonPanel.add(createVizButton());
        buttonPanel.add(createNormalizeButton());
        buttonPanel.add(createColorMenu());
        buttonPanel.add(createCovarianceMatrixButton());
        buttonPanel.add(createFontSettingsButton());
        buttonPanel.add(createRowOperationsMenu());
        buttonPanel.add(createCovarianceSortButton());
        buttonPanel.add(createSetClassColorsButton());
        buttonPanel.add(createRuleTesterButton());
        buttonPanel.add(createToggleTrigonometricButton());
        buttonPanel.add(createToggleEasyCasesButton());
        buttonPanel.add(createWeightedSumButton());
        buttonPanel.add(createSlopesAndDistancesButton());

        return buttonPanel;
    }

    private JButton createFileMenu() {
        JMenuItem loadItem = new JMenuItem("Load CSV");
        JMenuItem exportItem = new JMenuItem("Export CSV");

        loadItem.addActionListener(e -> csvViewer.loadCsvFile());
        exportItem.addActionListener(e -> csvViewer.exportCsvFile());

        JPopupMenu fileMenu = new JPopupMenu();
        fileMenu.add(loadItem);
        fileMenu.add(exportItem);

        return UIHelper.createButton("/icons/file.png", "File", e -> fileMenu.show((JComponent) e.getSource(), 0, 0));
    }

    private JButton createVizButton() {
        JMenuItem parallelPlotItem = new JMenuItem("Parallel Coordinates Plot");
        JMenuItem shiftedPairedItem = new JMenuItem("Shifted Paired Coordinates Plot");
        JMenuItem CircularCoordinatesItem = new JMenuItem("Circular/Polygonal Coordinates Plot");
        JMenuItem starCoordinatesItem = new JMenuItem("Traditional Star Coordinates Plot");
        JMenuItem concentricCoordinatesItem = new JMenuItem("Concentric Coordinates Plot");
        JMenuItem lineCoordinatesItem = new JMenuItem("Line Coordinates Plot");
        JMenuItem dtItem = new JMenuItem("Decision Tree");

        parallelPlotItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showParallelCoordinatesPlot();
            }
        });
        shiftedPairedItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showShiftedPairedCoordinates();
            }
        });
        CircularCoordinatesItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCircularCoordinatesPlot();
            }
        });
        starCoordinatesItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showStarCoordinatesPlot();
            }
        });
        concentricCoordinatesItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showConcentricCoordinatesPlot();
            }
        });
        lineCoordinatesItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showLineCoordinatesPlot();
            }
        });
        dtItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showDecisionTreeVisualization();
            }
        });

        JPopupMenu vizMenu = new JPopupMenu();
        vizMenu.add(parallelPlotItem);
        vizMenu.add(shiftedPairedItem);
        vizMenu.add(CircularCoordinatesItem);
        vizMenu.add(starCoordinatesItem);
        vizMenu.add(concentricCoordinatesItem);
        vizMenu.add(lineCoordinatesItem);
        vizMenu.add(dtItem);

        return UIHelper.createButton("/icons/start.png", "Visualizations", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                vizMenu.show((JComponent) e.getSource(), 0, 0);
            }
        });
    }

    private JButton createNormalizeButton() {
        toggleButton = UIHelper.createButton("/icons/normalize.png", "Normalize", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
                return;
            }

            JMenuItem minMaxItem = new JMenuItem("Min-Max Normalization");
            JMenuItem zScoreItem = new JMenuItem("Z-Score Normalization");
            minMaxItem.addActionListener(event -> {
                csvViewer.getStateManager().setNormalizationType("minmax");
                csvViewer.dataHandler.setNormalizationType("minmax");
                csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
                csvViewer.getStateManager().setNormalized(true);
                toggleButton.setIcon(UIHelper.loadIcon("/icons/denormalize.png", 40, 40));
                toggleButton.setToolTipText("Denormalize");
            });
            zScoreItem.addActionListener(event -> {
                csvViewer.getStateManager().setNormalizationType("zscore");
                csvViewer.dataHandler.setNormalizationType("zscore");
                csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
                csvViewer.getStateManager().setNormalized(true);
                toggleButton.setIcon(UIHelper.loadIcon("/icons/denormalize.png", 40, 40));
                toggleButton.setToolTipText("Denormalize");
            });
            JPopupMenu normalizationMenu = new JPopupMenu();
            normalizationMenu.add(minMaxItem);
            normalizationMenu.add(zScoreItem);
            if (!csvViewer.getStateManager().isNormalized()) {
                normalizationMenu.show((JComponent) e.getSource(), 0, 0);
            } else {
                csvViewer.dataHandler.normalizeOrDenormalizeData(csvViewer.getTable(), csvViewer.getStatsTextArea());
                csvViewer.getStateManager().setNormalized(false);
                toggleButton.setIcon(UIHelper.loadIcon("/icons/denormalize.png", 40, 40));
                toggleButton.setToolTipText("Denormalize");
            }
        });
        return toggleButton;
    }

    public JButton getToggleButton() {
        return toggleButton;
    }

    private JButton createColorMenu() {
        JMenuItem heatmapItem = new JMenuItem("Toggle Heatmap");
        JMenuItem classColorItem = new JMenuItem("Toggle Class Colors");
        JMenuItem highlightBlanksItem = new JMenuItem("Highlight Blanks");

        // Create popup menu
        JPopupMenu colorMenu = new JPopupMenu();
        colorMenu.add(heatmapItem);
        colorMenu.add(classColorItem);
        colorMenu.add(highlightBlanksItem);

        // Create button that ONLY shows menu
        JButton button = UIHelper.createButton("/icons/heatmap.png", "Visualization Options", e -> colorMenu.show((JComponent) e.getSource(), 0, 0));

        heatmapItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleHeatmap();
            }
        });

        classColorItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleClassColors();
            }
        });

        highlightBlanksItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.highlightBlanks();
            }
        });

        return button;
    }

    private JButton createCovarianceMatrixButton() {
        return UIHelper.createButton("/icons/variance.png", "Show Covariance Matrix", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCovarianceMatrix();
            }
        });
    }

    private JButton createSlopesAndDistancesButton() {
        return UIHelper.createButton("/icons/slopes_distances.png", "Insert Slopes and Distances", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCalculateSlopesAndDistancesDialog();
            }
        });
    }    

    private JButton createFontSettingsButton() {
        return UIHelper.createButton("/icons/fontcolor.png", "Font Color", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showFontSettingsDialog();
            }
        });
    }

    private JButton createRowOperationsMenu() {
        JMenuItem insertItem = new JMenuItem("Insert Row");
        JMenuItem deleteItem = new JMenuItem("Delete Row");
        JMenuItem cloneItem = new JMenuItem("Clone Row");

        insertItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertRow();
            }
        });

        deleteItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.deleteRow();
            }
        });

        cloneItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.cloneSelectedRow();
            }
        });

        JPopupMenu rowMenu = new JPopupMenu();
        rowMenu.add(insertItem);
        rowMenu.add(deleteItem);
        rowMenu.add(cloneItem);

        return UIHelper.createButton("/icons/clone.png", "Row Operations", e -> rowMenu.show((JComponent) e.getSource(), 0, 0));
    }

    private JButton createCovarianceSortButton() {
        return UIHelper.createButton("/icons/sort.png", "Sort Columns by Covariance", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCovarianceSortDialog();
            }
        });
    }

    private JButton createSetClassColorsButton() {
        return UIHelper.createButton("/icons/setcolor.png", "Set Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showColorPickerDialog();
            }
        });
    }

    private JButton createRuleTesterButton() {
        return UIHelper.createButton("/icons/rule_tester.png", "Rule Tester", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleTesterDialog();
            }
        });
    }

    private JButton createToggleTrigonometricButton() {
        JMenuItem forwardDiffItem = new JMenuItem("Forward Differences");
        JMenuItem backwardDiffItem = new JMenuItem("Backward Differences");
        JMenuItem directItem = new JMenuItem("Direct");
        JMenuItem invForwardDiffItem = new JMenuItem("Inverse Forward Differences");
        JMenuItem invBackwardDiffItem = new JMenuItem("Inverse Backward Differences");
        JMenuItem invDirectItem = new JMenuItem("Inverse Direct");
        JMenuItem removeItem = new JMenuItem("Remove Trigonometric Columns");

        ActionListener trigActionListener = e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
                return;
            }
            csvViewer.toggleTrigonometricColumns();
        };

        forwardDiffItem.addActionListener(trigActionListener);
        backwardDiffItem.addActionListener(trigActionListener);
        directItem.addActionListener(trigActionListener);
        invForwardDiffItem.addActionListener(trigActionListener);
        invBackwardDiffItem.addActionListener(trigActionListener);
        invDirectItem.addActionListener(trigActionListener);
        removeItem.addActionListener(trigActionListener);

        JPopupMenu trigMenu = new JPopupMenu();
        trigMenu.add(forwardDiffItem);
        trigMenu.add(backwardDiffItem);
        trigMenu.add(directItem);
        trigMenu.addSeparator();
        trigMenu.add(invForwardDiffItem);
        trigMenu.add(invBackwardDiffItem);
        trigMenu.add(invDirectItem);
        trigMenu.addSeparator();
        trigMenu.add(removeItem);

        return UIHelper.createButton("/icons/trigon.png", "Trigonometric Options", 
            e -> trigMenu.show((JComponent) e.getSource(), 0, 0));
    }

    private JButton createToggleEasyCasesButton() {
        csvViewer.toggleEasyCasesButton = UIHelper.createButton("/icons/easy.png", "Show Easy Cases", null);
        csvViewer.toggleEasyCasesButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleEasyCases();
                if (csvViewer.hasHiddenRows()) {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("/icons/uneasy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show All Cases");
                } else {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("/icons/easy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show Non-Easy Cases");
                }
            }
        });
        return csvViewer.toggleEasyCasesButton;
    }

    private JButton createWeightedSumButton() {
        return UIHelper.createButton("/icons/combo.png", "Insert Weighted Sum Column", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertWeightedSumColumn();
            }
        });
    }
}
