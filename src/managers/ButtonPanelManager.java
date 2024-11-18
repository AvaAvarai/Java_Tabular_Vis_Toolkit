package src.managers;

import src.CsvViewer;
import src.UIHelper;

import javax.swing.*;
import java.awt.*;

public class ButtonPanelManager {

    private final CsvViewer csvViewer;

    public ButtonPanelManager(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    public JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(createLoadButton());
        buttonPanel.add(createToggleButton());
        buttonPanel.add(createHighlightBlanksButton());
        buttonPanel.add(createHeatmapButton());
        buttonPanel.add(createCovarianceMatrixButton());
        buttonPanel.add(createFontSettingsButton());
        buttonPanel.add(createInsertRowButton());
        buttonPanel.add(createDeleteRowButton());
        buttonPanel.add(createCloneRowButton());
        buttonPanel.add(createExportButton());
        buttonPanel.add(createVizButton());
        buttonPanel.add(createCovarianceSortButton());
        buttonPanel.add(createClassColorButton());
        buttonPanel.add(createSetClassColorsButton());
        buttonPanel.add(createRuleTesterButton());
        buttonPanel.add(createToggleTrigonometricButton());
        buttonPanel.add(createToggleEasyCasesButton());
        buttonPanel.add(createRuleOverlayButton());
        buttonPanel.add(createLinearCombinationButton());
        buttonPanel.add(createSlopesAndDistancesButton());

        return buttonPanel;
    }

    private JButton createVizButton() {
        JMenuItem parallelPlotItem = new JMenuItem("Parallel Plot");
        JMenuItem shiftedPairedItem = new JMenuItem("Shifted Paired Coordinates");
        JMenuItem staticCircularCoordinatesItem = new JMenuItem("Static Circular Coordinates Plot");
        JMenuItem starCoordinatesItem = new JMenuItem("Star Coordinates");
        JMenuItem concentricCoordinatesItem = new JMenuItem("Concentric Coordinates Plot");
        JMenuItem lineCoordinatesItem = new JMenuItem("Line Coordinates");
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
        staticCircularCoordinatesItem.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showStaticCircularCoordinatesPlot();
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
        vizMenu.add(staticCircularCoordinatesItem);
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

    private JButton createLoadButton() {
        return UIHelper.createButton("/icons/load.png", "Load CSV", e -> csvViewer.loadCsvFile());
    }

    private JButton createToggleButton() {
        csvViewer.toggleButton = UIHelper.createButton("/icons/normalize.png", "Normalize", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleDataView();
            }
        });
        return csvViewer.toggleButton;
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
        return UIHelper.createButton("/icons/heatmap.png", "Show Heatmap", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleHeatmap();
            }
        });
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

    private JButton createExportButton() {
        return UIHelper.createButton("/icons/export.png", "Export CSV", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.exportCsvFile();
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

    private JButton createClassColorButton() {
        return UIHelper.createButton("/icons/classcolor.png", "Toggle Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleClassColors();
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

    private JButton createRuleOverlayButton() {
        return UIHelper.createButton("/icons/ruleplot.png", "Rule Overlay Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleOverlayPlot();
            }
        });
    }

    private JButton createLinearCombinationButton() {
        return UIHelper.createButton("/icons/combo.png", "Insert Linear Combination Column", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertLinearCombinationColumn();
            }
        });
    }
}
