package src;

import src.plots.*;
import src.utils.PureRegionUtils;

import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

public class VisualizationManager {

    private final CsvViewer csvViewer;

    public VisualizationManager(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    public void showStarCoordinatesPlot() {
        if (csvViewer.tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        List<List<Double>> numericalData = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        int classColumnIndex = csvViewer.getClassColumnIndex();
        for (int col = 0; col < csvViewer.tableModel.getColumnCount(); col++) {
            if (col != classColumnIndex) {
                boolean isNumeric = true;
                List<Double> columnData = new ArrayList<>();
                for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                    if (!csvViewer.getHiddenRows().contains(row)) {
                        try {
                            columnData.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, col).toString()));
                        } catch (NumberFormatException e) {
                            isNumeric = false;
                            break;
                        }
                    }
                }
                if (isNumeric) {
                    numericalData.add(columnData);
                    attributeNames.add(csvViewer.tableModel.getColumnName(col));
                }
            }
        }

        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                classLabels.add((String) csvViewer.tableModel.getValueAt(row, classColumnIndex));
            }
        }

        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        StarCoordinatesPlot starCoordinatesPlot = new StarCoordinatesPlot(numericalData, attributeNames, csvViewer.classColors, csvViewer.classShapes, classLabels, selectedRows);
        starCoordinatesPlot.setVisible(true);
    }

    public void showRuleOverlayPlot() {
        TableColumnModel columnModel = csvViewer.table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        String[] columnNames = new String[columnCount];
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = columnModel.getColumn(i).getHeaderValue().toString();
            columnOrder[i] = csvViewer.table.convertColumnIndexToModel(i);
        }

        List<String[]> data = new ArrayList<>();
        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                String[] rowData = new String[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    Object value = csvViewer.tableModel.getValueAt(row, col);
                    rowData[col] = value != null ? value.toString() : "";
                }
                data.add(rowData);
            }
        }

        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        List<PureRegionUtils> pureRegions = PureRegionUtils.calculatePureRegions(csvViewer.tableModel, csvViewer.thresholdSlider.getValue(), csvViewer.getClassColumnIndex());

        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, csvViewer.classColors, csvViewer.getClassColumnIndex(), columnOrder, selectedRows, csvViewer.classShapes, csvViewer.getDatasetName());
        plot.setPureRegionsOverlay(pureRegions);
        plot.setVisible(true);
    }

    public void showShiftedPairedCoordinates() {
        List<List<Double>> data = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        TableColumnModel columnModel = csvViewer.table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnOrder[i] = csvViewer.table.convertColumnIndexToModel(i);
        }

        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                if (!csvViewer.getHiddenRows().contains(row)) {
                    try {
                        columnData.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, columnOrder[col]).toString()));
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            if (isNumeric) {
                data.add(columnData);
                attributeNames.add(csvViewer.tableModel.getColumnName(columnOrder[col]));
            }
        }

        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                classLabels.add((String) csvViewer.tableModel.getValueAt(row, csvViewer.getClassColumnIndex()));
            }
        }

        int numPlots = (attributeNames.size() + 1) / 2;
        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        ShiftedPairedCoordinatesPlot shiftedPairedCoordinates = new ShiftedPairedCoordinatesPlot(data, attributeNames, csvViewer.classColors, csvViewer.classShapes, classLabels, numPlots, selectedRows, csvViewer.getDatasetName());
        shiftedPairedCoordinates.setVisible(true);
    }

    public void showStaticCircularCoordinatesPlot() {
        List<List<Double>> data = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        TableColumnModel columnModel = csvViewer.table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnOrder[i] = csvViewer.table.convertColumnIndexToModel(i);
        }

        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                if (!csvViewer.getHiddenRows().contains(row)) {
                    try {
                        columnData.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, columnOrder[col]).toString()));
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                        break;
                    }
                }
            }
            if (isNumeric) {
                data.add(columnData);
                attributeNames.add(csvViewer.tableModel.getColumnName(columnOrder[col]));
            }
        }

        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                classLabels.add((String) csvViewer.tableModel.getValueAt(row, csvViewer.getClassColumnIndex()));
            }
        }

        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        StaticCircularCoordinatesPlot plot = new StaticCircularCoordinatesPlot(data, attributeNames, csvViewer.classColors, csvViewer.classShapes, classLabels, selectedRows);
        plot.setVisible(true);
    }

    public void showParallelCoordinatesPlot() {
        TableColumnModel columnModel = csvViewer.table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        String[] columnNames = new String[columnCount];
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = columnModel.getColumn(i).getHeaderValue().toString();
            columnOrder[i] = csvViewer.table.convertColumnIndexToModel(i);
        }

        List<String[]> data = new ArrayList<>();
        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                String[] rowData = new String[columnCount];
                for (int col = 0; col < columnCount; col++) {
                    Object value = csvViewer.tableModel.getValueAt(row, col);
                    rowData[col] = value != null ? value.toString() : "";
                }
                data.add(rowData);
            }
        }

        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, csvViewer.classColors, csvViewer.getClassColumnIndex(), columnOrder, selectedRows, csvViewer.classShapes, csvViewer.getDatasetName());
        plot.setVisible(true);
    }
}
