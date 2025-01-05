package src.managers;

import src.CsvViewer;
import src.UIHelper;

import javax.swing.*;
import java.awt.*;

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
        buttonPanel.add(createHighlightBlanksButton());
        buttonPanel.add(createHeatmapButton());
        buttonPanel.add(createCovarianceMatrixButton());
        buttonPanel.add(createFontSettingsButton());
        buttonPanel.add(createInsertRowButton());
        buttonPanel.add(createDeleteRowButton());
        buttonPanel.add(createCloneRowButton());
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

    private JButton createHighlightBlanksButton() {
        return UIHelper.createButton("/icons/highlight.png", "Highlight Blanks", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.highlightBlanks();
            }
        });
    }

    private JButton createHeatmapButton() {
        JMenuItem heatmapItem = new JMenuItem("Toggle Heatmap");
        JMenuItem classColorItem = new JMenuItem("Toggle Class Colors");

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

        JPopupMenu heatmapMenu = new JPopupMenu();
        heatmapMenu.add(heatmapItem);
        heatmapMenu.add(classColorItem);

        return UIHelper.createButton("/icons/heatmap.png", "Heatmap", e -> heatmapMenu.show((JComponent) e.getSource(), 0, 0));
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

    private JButton createInsertRowButton() {
        return UIHelper.createButton("/icons/insert.png", "Insert Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertRow();
            }
        });
    }

    private JButton createDeleteRowButton() {
        return UIHelper.createButton("/icons/delete.png", "Delete Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.deleteRow();
            }
        });
    }

    private JButton createCloneRowButton() {
        return UIHelper.createButton("/icons/clone.png", "Clone Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.cloneSelectedRow();
            }
        });
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
        csvViewer.toggleTrigonometricButton = UIHelper.createButton("/icons/trigon.png", "Insert Trig Columns", null);
        csvViewer.toggleTrigonometricButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleTrigonometricColumns();
                if (csvViewer.areDifferenceColumnsVisible()) {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("/icons/trigoff.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Remove Trig Columns");
                } else {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("/icons/trigon.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Insert Trig Columns");
                }
            }
        });
        return csvViewer.toggleTrigonometricButton;
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
