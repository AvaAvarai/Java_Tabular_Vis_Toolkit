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
        JButton fontColorButton = UIHelper.createButton("icons/fontcolor.png", "Font Color", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.chooseFontColor();
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
        JButton shiftedPairedButton = UIHelper.createButton("icons/shiftedpaired.png", "Shifted Paired", e -> {
            if (csvViewer.dataHandler.isDataEmpty()) {
                csvViewer.noDataLoadedError();
            } else {
                csvViewer.showShiftedPairedCoordinates();
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

        buttonPanel.add(loadButton);
        buttonPanel.add(csvViewer.toggleButton);
        buttonPanel.add(highlightBlanksButton);
        buttonPanel.add(heatmapButton);
        buttonPanel.add(fontColorButton);
        buttonPanel.add(insertRowButton);
        buttonPanel.add(deleteRowButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(parallelPlotButton);
        buttonPanel.add(shiftedPairedButton);
        buttonPanel.add(classColorButton);
        buttonPanel.add(setClassColorsButton);
        buttonPanel.add(ruleTesterButton);

        return buttonPanel;
    }
}
