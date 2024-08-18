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
        buttonPanel.add(createParallelPlotButton());
        buttonPanel.add(createShiftedPairedButton());
        buttonPanel.add(createStaticCircularCoordinatesButton());
        buttonPanel.add(createStarCoordinatesButton());
        buttonPanel.add(createCovarianceSortButton());
        buttonPanel.add(createClassColorButton());
        buttonPanel.add(createSetClassColorsButton());
        buttonPanel.add(createRuleTesterButton());
        buttonPanel.add(createToggleTrigonometricButton());
        buttonPanel.add(createToggleEasyCasesButton());
        buttonPanel.add(createRuleOverlayButton());
        buttonPanel.add(createLinearCombinationButton());

        return buttonPanel;
    }

    private JButton createLoadButton() {
        return UIHelper.createButton("resources/icons/load.png", "Load CSV", e -> csvViewer.loadCsvFile());
    }

    private JButton createToggleButton() {
        csvViewer.toggleButton = UIHelper.createButton("resources/icons/normalize.png", "Normalize", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleDataView();
            }
        });
        return csvViewer.toggleButton;
    }

    private JButton createHighlightBlanksButton() {
        return UIHelper.createButton("resources/icons/highlight.png", "Highlight Blanks", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.highlightBlanks();
            }
        });
    }

    private JButton createHeatmapButton() {
        return UIHelper.createButton("resources/icons/heatmap.png", "Show Heatmap", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleHeatmap();
            }
        });
    }

    private JButton createCovarianceMatrixButton() {
        return UIHelper.createButton("resources/icons/variance.png", "Show Covariance Matrix", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCovarianceMatrix();
            }
        });
    }

    private JButton createFontSettingsButton() {
        return UIHelper.createButton("resources/icons/fontcolor.png", "Font Color", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showFontSettingsDialog();
            }
        });
    }

    private JButton createInsertRowButton() {
        return UIHelper.createButton("resources/icons/insert.png", "Insert Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertRow();
            }
        });
    }

    private JButton createDeleteRowButton() {
        return UIHelper.createButton("resources/icons/delete.png", "Delete Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.deleteRow();
            }
        });
    }

    private JButton createCloneRowButton() {
        return UIHelper.createButton("resources/icons/clone.png", "Clone Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.cloneSelectedRow();
            }
        });
    }

    private JButton createExportButton() {
        return UIHelper.createButton("resources/icons/export.png", "Export CSV", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.exportCsvFile();
            }
        });
    }

    private JButton createParallelPlotButton() {
        return UIHelper.createButton("resources/icons/parallel.png", "Parallel Coordinates Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showParallelCoordinatesPlot();
            }
        });
    }

    private JButton createShiftedPairedButton() {
        return UIHelper.createButton("resources/icons/shiftedpaired.png", "Shifted Paired Coordinates Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showShiftedPairedCoordinates();
            }
        });
    }

    private JButton createStaticCircularCoordinatesButton() {
        return UIHelper.createButton("resources/icons/staticcircular.png", "Static Circular Coordinates Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showStaticCircularCoordinatesPlot();
            }
        });
    }

    private JButton createStarCoordinatesButton() {
        return UIHelper.createButton("resources/icons/star.png", "Star Coordinates Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showStarCoordinatesPlot();
            }
        });
    }

    private JButton createCovarianceSortButton() {
        return UIHelper.createButton("resources/icons/sort.png", "Sort Columns by Covariance", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCovarianceSortDialog();
            }
        });
    }

    private JButton createClassColorButton() {
        return UIHelper.createButton("resources/icons/classcolor.png", "Toggle Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleClassColors();
            }
        });
    }

    private JButton createSetClassColorsButton() {
        return UIHelper.createButton("resources/icons/setcolor.png", "Set Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showColorPickerDialog();
            }
        });
    }

    private JButton createRuleTesterButton() {
        return UIHelper.createButton("resources/icons/rule_tester.png", "Rule Tester", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleTesterDialog();
            }
        });
    }

    private JButton createToggleTrigonometricButton() {
        csvViewer.toggleTrigonometricButton = UIHelper.createButton("resources/icons/trigon.png", "Insert Trig Columns", null);
        csvViewer.toggleTrigonometricButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleTrigonometricColumns();
                if (csvViewer.areDifferenceColumnsVisible()) {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("resources/icons/trigoff.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Remove Trig Columns");
                } else {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("resources/icons/trigon.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Insert Trig Columns");
                }
            }
        });
        return csvViewer.toggleTrigonometricButton;
    }

    private JButton createToggleEasyCasesButton() {
        csvViewer.toggleEasyCasesButton = UIHelper.createButton("resources/icons/easy.png", "Show Easy Cases", null);
        csvViewer.toggleEasyCasesButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleEasyCases();
                if (csvViewer.hasHiddenRows()) {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("resources/icons/uneasy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show All Cases");
                } else {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("resources/icons/easy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show Non-Easy Cases");
                }
            }
        });
        return csvViewer.toggleEasyCasesButton;
    }

    private JButton createRuleOverlayButton() {
        return UIHelper.createButton("resources/icons/ruleplot.png", "Rule Overlay Plot", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleOverlayPlot();
            }
        });
    }

    private JButton createLinearCombinationButton() {
        return UIHelper.createButton("resources/icons/combo.png", "Insert Linear Combination Column", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertLinearCombinationColumn();
            }
        });
    }
}
