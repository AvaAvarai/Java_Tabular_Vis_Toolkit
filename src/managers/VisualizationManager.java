package src.managers;

import src.CsvViewer;
import src.plots.*;
import src.utils.DecisionTreeModel;
import src.utils.PureRegionUtils;
import src.utils.DecisionTreeModel.TreeNode;

import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.swing.*;

public class VisualizationManager {

    private final CsvViewer csvViewer;

    public VisualizationManager(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    private void checkAndHandleNewClasses(List<String> classLabels) {
        Set<String> uniqueClasses = new HashSet<>(classLabels);
        for (String className : uniqueClasses) {
            if (!csvViewer.getClassColors().containsKey(className)) {
                csvViewer.handleNewClass(className);
            }
        }
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
        TableColumnModel columnModel = csvViewer.table.getColumnModel();
    
        // Respect the order of attributes as in the JTable (tabular view)
        for (int col = 0; col < columnModel.getColumnCount(); col++) {
            int modelIndex = csvViewer.table.convertColumnIndexToModel(col);
            if (modelIndex != classColumnIndex) {
                boolean isNumeric = true;
                List<Double> columnData = new ArrayList<>();
                for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                    if (!csvViewer.getHiddenRows().contains(row)) {
                        try {
                            columnData.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, modelIndex).toString()));
                        } catch (NumberFormatException e) {
                            isNumeric = false;
                            break;
                        }
                    }
                }
                if (isNumeric) {
                    numericalData.add(columnData);
                    attributeNames.add(csvViewer.tableModel.getColumnName(modelIndex));
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
    
        StarCoordinatesPlot starCoordinatesPlot = new StarCoordinatesPlot(numericalData, attributeNames, csvViewer.getClassColors(), csvViewer.getClassShapes(), classLabels, selectedRows, csvViewer.getDatasetName());
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

        // TODO: will we keep this feature as we are not using it currently and moving towards decision tree space visualization?
        List<PureRegionUtils> pureRegions = PureRegionUtils.calculatePureRegions(csvViewer.tableModel, csvViewer.thresholdSlider.getValue(), csvViewer.getClassColumnIndex());

        //ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, csvViewer.getClassColors(), csvViewer.getClassColumnIndex(), columnOrder, selectedRows, csvViewer.getClassShapes(), csvViewer.getDatasetName());
        //plot.setPureRegionsOverlay(pureRegions);
        //plot.setVisible(true);
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

        ShiftedPairedCoordinatesPlot shiftedPairedCoordinates = new ShiftedPairedCoordinatesPlot(data, attributeNames, csvViewer.getClassColors(), csvViewer.getClassShapes(), classLabels, numPlots, selectedRows, csvViewer.getDatasetName(), csvViewer.getTable());
        shiftedPairedCoordinates.setVisible(true);
    }

    public void showCollocatedPairedCoordinates() {
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

        CollocatedPairedCoordinatesPlot collocatedPairedCoordinates = new CollocatedPairedCoordinatesPlot(data, attributeNames, csvViewer.getClassColors(), csvViewer.getClassShapes(), classLabels, selectedRows, csvViewer.getDatasetName(), csvViewer.getTable());
        collocatedPairedCoordinates.setVisible(true);
    }

    public void showCircularCoordinatesPlot() {
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

        CircularCoordinatesPlot plot = new CircularCoordinatesPlot(data, attributeNames, csvViewer.getClassColors(), csvViewer.getClassShapes(), classLabels, selectedRows, csvViewer.getDatasetName());
        plot.setVisible(true);
    }

    public void showParallelCoordinatesPlot() {
        if (csvViewer.tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        List<List<Double>> numericalData = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        int classColumnIndex = csvViewer.getClassColumnIndex();
        TableColumnModel columnModel = csvViewer.table.getColumnModel();

        // First, collect the attribute names in the current view order
        for (int col = 0; col < columnModel.getColumnCount(); col++) {
            int modelIndex = csvViewer.table.convertColumnIndexToModel(col);
            if (modelIndex != classColumnIndex) {
                attributeNames.add(csvViewer.tableModel.getColumnName(modelIndex));
                numericalData.add(new ArrayList<>()); // Initialize lists for each attribute
            }
        }

        // Then collect data only for visible rows
        for (int viewRow = 0; viewRow < csvViewer.table.getRowCount(); viewRow++) {
            int modelRow = csvViewer.table.convertRowIndexToModel(viewRow);
            
            // Check if all columns for this row can be parsed as numbers
            boolean allNumeric = true;
            int dataIndex = 0;
            
            for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                if (modelCol != classColumnIndex) {
                    try {
                        Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                    } catch (NumberFormatException e) {
                        allNumeric = false;
                        break;
                    }
                }
            }

            if (allNumeric) {
                // Add the numeric data for this row
                dataIndex = 0;
                for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                    int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                    if (modelCol != classColumnIndex) {
                        double value = Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                        numericalData.get(dataIndex++).add(value);
                    }
                }
                // Add the class label for this row
                classLabels.add((String) csvViewer.tableModel.getValueAt(modelRow, classColumnIndex));
            }
        }

        // Only proceed if we have numeric data
        if (!numericalData.isEmpty() && !numericalData.get(0).isEmpty()) {
            List<Integer> selectedRows = new ArrayList<>();
            // Convert selected rows to indices in our filtered dataset
            int[] selectedViewRows = csvViewer.table.getSelectedRows();
            int dataRowIndex = 0;
            
            for (int viewRow = 0; viewRow < csvViewer.table.getRowCount(); viewRow++) {
                int modelRow = csvViewer.table.convertRowIndexToModel(viewRow);
                boolean isNumericRow = true;
                
                // Check if this row is all numeric
                for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                    int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                    if (modelCol != classColumnIndex) {
                        try {
                            Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                        } catch (NumberFormatException e) {
                            isNumericRow = false;
                            break;
                        }
                    }
                }
                
                if (isNumericRow) {
                    // If this row is selected in the view, add its index in our filtered dataset
                    for (int selectedViewRow : selectedViewRows) {
                        if (viewRow == selectedViewRow) {
                            selectedRows.add(dataRowIndex);
                            break;
                        }
                    }
                    dataRowIndex++;
                }
            }

            checkAndHandleNewClasses(classLabels);

            ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(
                numericalData,
                attributeNames,
                csvViewer.getClassColors(),
                csvViewer.getClassShapes(),
                classLabels,
                selectedRows,
                csvViewer.getDatasetName()
            );
            plot.setSize(800, 800);
            plot.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(csvViewer, 
                "No numeric data available to visualize.", 
                "Visualization Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showDecisionTreeVisualization() {

        List<String> attributeNames = new ArrayList<>();
        for (int i = 0; i < csvViewer.getTable().getColumnCount(); i++) {
            attributeNames.add(csvViewer.getTable().getColumnName(i));
        }
        int labelColumnIndex = csvViewer.getClassColumnIndex();
        DecisionTreeModel decisionTree = new DecisionTreeModel(csvViewer.getDataHandler().isDataNormalized() ? csvViewer.getDataHandler().getNormalizedData() : csvViewer.getDataHandler().getOriginalData(), attributeNames, labelColumnIndex);
        TreeNode root = decisionTree.getRoot();
        JFrame frame = new JFrame("Decision Tree Visualization");
        DecisionTreePlot treePanel = new DecisionTreePlot(root, attributeNames, csvViewer.getClassColors());

        JScrollPane scrollPane = new JScrollPane(treePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        frame.add(scrollPane);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    public void showConcentricCoordinatesPlot() {
        if (csvViewer.tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

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

        ConcentricCoordinatesPlot plot = new ConcentricCoordinatesPlot(data, attributeNames, csvViewer.getClassColors(), csvViewer.getClassShapes(), classLabels, selectedRows, selectedRows, csvViewer.getDatasetName());
        plot.setVisible(true);
    }

    public void showLineCoordinatesPlot() {
        if (csvViewer.tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        List<List<Double>> numericalData = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        int classColumnIndex = csvViewer.getClassColumnIndex();
        TableColumnModel columnModel = csvViewer.table.getColumnModel();

        // Collect numerical data and attribute names
        for (int col = 0; col < columnModel.getColumnCount(); col++) {
            int modelIndex = csvViewer.table.convertColumnIndexToModel(col);
            if (modelIndex != classColumnIndex) {
                boolean isNumeric = true;
                List<Double> columnData = new ArrayList<>();
                for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
                    if (!csvViewer.getHiddenRows().contains(row)) {
                        try {
                            columnData.add(Double.parseDouble(csvViewer.tableModel.getValueAt(row, modelIndex).toString()));
                        } catch (NumberFormatException e) {
                            isNumeric = false;
                            break;
                        }
                    }
                }
                if (isNumeric) {
                    numericalData.add(columnData);
                    attributeNames.add(csvViewer.tableModel.getColumnName(modelIndex));
                }
            }
        }

        // Collect class labels
        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            if (!csvViewer.getHiddenRows().contains(row)) {
                classLabels.add((String) csvViewer.tableModel.getValueAt(row, classColumnIndex));
            }
        }

        List<Integer> selectedRows = csvViewer.getSelectedRowsIndices();
        selectedRows.removeIf(csvViewer.getHiddenRows()::contains);

        LineCoordinatesPlot plot = new LineCoordinatesPlot(
            numericalData,
            attributeNames,
            csvViewer.getClassColors(),
            csvViewer.getClassShapes(),
            classLabels,
            selectedRows,
            csvViewer.getDatasetName()
        );
        plot.setVisible(true);
    }
    
    public void showMultiRowParallelCoordinatesPlot() {
        if (csvViewer.tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        List<List<Double>> numericalData = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        List<String> classLabels = new ArrayList<>();

        int classColumnIndex = csvViewer.getClassColumnIndex();
        TableColumnModel columnModel = csvViewer.table.getColumnModel();

        // First, collect the attribute names in the current view order
        for (int col = 0; col < columnModel.getColumnCount(); col++) {
            int modelIndex = csvViewer.table.convertColumnIndexToModel(col);
            if (modelIndex != classColumnIndex) {
                attributeNames.add(csvViewer.tableModel.getColumnName(modelIndex));
                numericalData.add(new ArrayList<>()); // Initialize lists for each attribute
            }
        }

        // Then collect data only for visible rows
        for (int viewRow = 0; viewRow < csvViewer.table.getRowCount(); viewRow++) {
            int modelRow = csvViewer.table.convertRowIndexToModel(viewRow);
            
            // Check if all columns for this row can be parsed as numbers
            boolean allNumeric = true;
            int dataIndex = 0;
            
            for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                if (modelCol != classColumnIndex) {
                    try {
                        Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                    } catch (NumberFormatException e) {
                        allNumeric = false;
                        break;
                    }
                }
            }

            if (allNumeric) {
                // Add the numeric data for this row
                dataIndex = 0;
                for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                    int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                    if (modelCol != classColumnIndex) {
                        double value = Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                        numericalData.get(dataIndex++).add(value);
                    }
                }
                // Add the class label for this row
                classLabels.add((String) csvViewer.tableModel.getValueAt(modelRow, classColumnIndex));
            }
        }

        // Only proceed if we have numeric data
        if (!numericalData.isEmpty() && !numericalData.get(0).isEmpty()) {
            List<Integer> selectedRows = new ArrayList<>();
            // Convert selected rows to indices in our filtered dataset
            int[] selectedViewRows = csvViewer.table.getSelectedRows();
            int dataRowIndex = 0;
            
            for (int viewRow = 0; viewRow < csvViewer.table.getRowCount(); viewRow++) {
                int modelRow = csvViewer.table.convertRowIndexToModel(viewRow);
                boolean isNumericRow = true;
                
                // Check if this row is all numeric
                for (int viewCol = 0; viewCol < columnModel.getColumnCount(); viewCol++) {
                    int modelCol = csvViewer.table.convertColumnIndexToModel(viewCol);
                    if (modelCol != classColumnIndex) {
                        try {
                            Double.parseDouble(csvViewer.tableModel.getValueAt(modelRow, modelCol).toString());
                        } catch (NumberFormatException e) {
                            isNumericRow = false;
                            break;
                        }
                    }
                }
                
                if (isNumericRow) {
                    // If this row is selected in the view, add its index in our filtered dataset
                    for (int selectedViewRow : selectedViewRows) {
                        if (viewRow == selectedViewRow) {
                            selectedRows.add(dataRowIndex);
                            break;
                        }
                    }
                    dataRowIndex++;
                }
            }

            checkAndHandleNewClasses(classLabels);

            // Calculate default number of rows based on attribute count
            int attributeCount = attributeNames.size();
            int defaultRows = Math.min(5, attributeCount);
            
            // Show dialog to get number of rows from user
            String input = JOptionPane.showInputDialog(
                csvViewer,
                "Enter number of rows for the Multi-Row Parallel Coordinates Plot (1-" + attributeCount + "):",
                "Multi-Row Parallel Coordinates",
                JOptionPane.QUESTION_MESSAGE
            );
            
            // Validate input
            int numRows;
            try {
                numRows = Integer.parseInt(input);
                if (numRows < 1) numRows = 1;
                if (numRows > attributeCount) numRows = attributeCount;
            } catch (NumberFormatException | NullPointerException e) {
                numRows = defaultRows; // Default if input is invalid or canceled
            }

            // Create and show the plot
            MultiRowParallelCoordinatesPlot plot = new MultiRowParallelCoordinatesPlot(
                numericalData,
                attributeNames,
                csvViewer.getClassColors(),
                csvViewer.getClassShapes(),
                classLabels,
                selectedRows,
                csvViewer.getDatasetName(),
                numRows
            );
            plot.setSize(1024, Math.min(900, 200 + numRows * 300)); // Adjust size based on row count
            plot.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(csvViewer, 
                "No numeric data available to visualize.", 
                "Visualization Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
