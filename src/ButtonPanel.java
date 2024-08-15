package src;

import javax.swing.*;
import java.awt.*;

public class ButtonPanel {

    public static JPanel createButtonPanel(CsvViewer csvViewer) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton loadButton = UIHelper.createButton("icons/load.png", "Load CSV", e -> csvViewer.loadCsvFile());
        csvViewer.toggleButton = UIHelper.createButton("icons/normalize.png", "Normalize", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleDataView();
            }
        });
        JButton highlightBlanksButton = UIHelper.createButton("icons/highlight.png", "Highlight Blanks", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.highlightBlanks();
            }
        });
        JButton heatmapButton = UIHelper.createButton("icons/heatmap.png", "Show Heatmap", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleHeatmap();
            }
        });
        JButton covarianceMatrixButton = UIHelper.createButton("icons/variance.png", "Show Covariance Matrix", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showCovarianceMatrix();
            }
        });
        JButton fontSettingsButton = UIHelper.createButton("icons/fontcolor.png", "Font Color", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showFontSettingsDialog();
            }
        });
        JButton insertRowButton = UIHelper.createButton("icons/insert.png", "Insert Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertRow();
            }
        });
        JButton deleteRowButton = UIHelper.createButton("icons/delete.png", "Delete Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.deleteRow();
            }
        });
        JButton exportButton = UIHelper.createButton("icons/export.png", "Export CSV", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.exportCsvFile();
            }
        });
        JButton parallelPlotButton = UIHelper.createButton("icons/parallel.png", "Parallel Coordinates", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showParallelCoordinatesPlot();
            }
        });
        JButton shiftedPairedButton = UIHelper.createButton("icons/shiftedpaired.png", "Shifted Paired Coordinates", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showShiftedPairedCoordinates();
            }
        });
        JButton staticCircularCoordinatesButton = UIHelper.createButton("icons/staticcircular.png", "Static Circular Coordinates", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showStaticCircularCoordinatesPlot();
            }
        });
        JButton classColorButton = UIHelper.createButton("icons/classcolor.png", "Toggle Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleClassColors();
            }
        });
        JButton setClassColorsButton = UIHelper.createButton("icons/setcolor.png", "Set Class Colors", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showColorPickerDialog();
            }
        });
        JButton ruleTesterButton = UIHelper.createButton("icons/rule_tester.png", "Rule Tester", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleTesterDialog();
            }
        });

        JButton cloneRowButton = UIHelper.createButton("icons/clone.png", "Clone Row", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.cloneSelectedRow();
            }
        });

        csvViewer.toggleTrigonometricButton = UIHelper.createButton("icons/trigon.png", "Insert Trig Columns", null);
        csvViewer.toggleTrigonometricButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleTrigonometricColumns();
                if (csvViewer.areDifferenceColumnsVisible) {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("icons/trigoff.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Remove Trig Columns");
                } else {
                    csvViewer.toggleTrigonometricButton.setIcon(UIHelper.loadIcon("icons/trigon.png", 40, 40));
                    csvViewer.toggleTrigonometricButton.setToolTipText("Insert Trig Columns");
                }
            }
        });

        csvViewer.toggleEasyCasesButton = UIHelper.createButton("icons/easy.png", "Show Easy Cases", null);
        csvViewer.toggleEasyCasesButton.addActionListener(e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.toggleEasyCases();
                if (csvViewer.hasHiddenRows()) {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/uneasy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show All Cases");
                } else {
                    csvViewer.toggleEasyCasesButton.setIcon(UIHelper.loadIcon("icons/easy.png", 40, 40));
                    csvViewer.toggleEasyCasesButton.setToolTipText("Show Non-Easy Cases");
                }
            }
        });

        JButton ruleOverlayButton = UIHelper.createButton("icons/ruleplot.png", "Rule Overlay", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showRuleOverlayPlot();
            }
        });
        
        JButton linearCombinationButton = UIHelper.createButton("icons/combo.png", "Insert Linear Combination Column", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.insertLinearCombinationColumn();
            }
        });

        // Add the new Star Coordinates button
        JButton starCoordinatesButton = UIHelper.createButton("icons/starcoordinates.png", "Star Coordinates", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                // Collect data for the plot and invoke the StarCoordinatesPlot
                csvViewer.showStarCoordinatesPlot();
            }
        });

        buttonPanel.add(loadButton);
        buttonPanel.add(csvViewer.toggleButton);
        buttonPanel.add(highlightBlanksButton);
        buttonPanel.add(heatmapButton);
        buttonPanel.add(covarianceMatrixButton);
        buttonPanel.add(fontSettingsButton);
        buttonPanel.add(insertRowButton);
        buttonPanel.add(deleteRowButton);
        buttonPanel.add(cloneRowButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(parallelPlotButton);
        buttonPanel.add(shiftedPairedButton);
        buttonPanel.add(staticCircularCoordinatesButton);
        buttonPanel.add(classColorButton);
        buttonPanel.add(setClassColorsButton);
        buttonPanel.add(ruleTesterButton);
        buttonPanel.add(csvViewer.toggleTrigonometricButton);
        buttonPanel.add(csvViewer.toggleEasyCasesButton);
        buttonPanel.add(ruleOverlayButton);
        buttonPanel.add(linearCombinationButton);
        buttonPanel.add(starCoordinatesButton);  // Add the new button

        return buttonPanel;
    }
}
