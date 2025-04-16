package src.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import src.CsvViewer;

public class SlopeAndDistanceFeatures {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private boolean useAbsoluteDistance = false; // Flag for absolute distance

    public SlopeAndDistanceFeatures(CsvViewer csvViewer, DefaultTableModel tableModel, JTable table) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
        this.table = table;
    }

    public void showDimensionDialog() {
        List<Integer> numericColumns = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        
        // Get numeric columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            int modelIndex = table.convertColumnIndexToModel(i);
            if (isColumnNumeric(modelIndex)) {
                numericColumns.add(modelIndex);
                columnNames.add(tableModel.getColumnName(modelIndex));
            }
        }

        if (numericColumns.size() < 2) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two numeric columns.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create spinner for point dimensionality
        SpinnerNumberModel model = new SpinnerNumberModel(2, 1, numericColumns.size(), 1);
        JSpinner dimSpinner = new JSpinner(model);
        
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.add(new JLabel("Select point dimensionality:"));
        panel.add(dimSpinner);

        int result = JOptionPane.showConfirmDialog(csvViewer, panel,
            "Point Dimensionality", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            int dim = (Integer) dimSpinner.getValue();
            showColumnSelectionDialog(numericColumns, columnNames, dim);
        }
    }

    private void showColumnSelectionDialog(List<Integer> numericColumns, List<String> columnNames, int dim) {
        JDialog dialog = new JDialog(csvViewer, "Select Columns", true);
        dialog.setLayout(new BorderLayout(5, 5));

        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 3, 5, 2));
        checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JCheckBox[] boxes = new JCheckBox[numericColumns.size()];
        
        for (int i = 0; i < numericColumns.size(); i++) {
            boxes[i] = new JCheckBox(columnNames.get(i), true);
            boxes[i].setFont(boxes[i].getFont().deriveFont(11f));
            checkBoxPanel.add(boxes[i]);
        }

        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        scrollPane.setPreferredSize(new Dimension(400, Math.min(400, 30 * (numericColumns.size() / 3 + 1))));
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Add Select All/None buttons
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> {
            for (JCheckBox box : boxes) box.setSelected(true);
        });
        JButton selectNoneButton = new JButton("Select None");
        selectNoneButton.addActionListener(e -> {
            for (JCheckBox box : boxes) box.setSelected(false);
        });
        selectionPanel.add(selectAllButton);
        selectionPanel.add(selectNoneButton);
        dialog.add(selectionPanel, BorderLayout.NORTH);

        // Add options panel for absolute distance
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox absoluteDistanceCheckbox = new JCheckBox("Use Absolute Distance", useAbsoluteDistance);
        absoluteDistanceCheckbox.addActionListener(e -> useAbsoluteDistance = absoluteDistanceCheckbox.isSelected());
        optionsPanel.add(absoluteDistanceCheckbox);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        
        // Add separator
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        southPanel.add(separator);
        southPanel.add(optionsPanel);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            List<Integer> selectedColumns = new ArrayList<>();
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isSelected()) {
                    selectedColumns.add(numericColumns.get(i));
                }
            }
            
            if (selectedColumns.size() < dim * 2) {
                JOptionPane.showMessageDialog(csvViewer, 
                    "Need at least " + (dim * 2) + " columns for " + dim + "-D points.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            dialog.dispose();
            calculateFeatures(selectedColumns, dim);
        });
        buttonPanel.add(okButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        southPanel.add(buttonPanel);
        dialog.add(southPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer);
        dialog.setVisible(true);
    }

    private void calculateFeatures(List<Integer> columns, int dim) {
        // Process columns in groups of dim size
        for (int i = 0; i <= columns.size() - 2 * dim; i += dim) {
            List<Integer> point1Cols = columns.subList(i, i + dim);
            List<Integer> point2Cols = columns.subList(i + dim, i + 2 * dim);
            
            String point1Name = point1Cols.stream()
                .map(col -> tableModel.getColumnName(col))
                .reduce((a, b) -> a + "," + b)
                .get();
            String point2Name = point2Cols.stream()
                .map(col -> tableModel.getColumnName(col))
                .reduce((a, b) -> a + "," + b)
                .get();

            String absPrefix = useAbsoluteDistance ? "Abs" : "";
            String slopeCol = csvViewer.getUniqueColumnName(
                dim + "D_Slope(" + point1Name + ")/(" + point2Name + ")");
            String distCol = csvViewer.getUniqueColumnName(
                dim + "D_" + absPrefix + "Distance(" + point1Name + ")-(" + point2Name + ")");
            
            tableModel.addColumn(slopeCol);
            tableModel.addColumn(distCol);
            
            int slopeIndex = tableModel.getColumnCount() - 2;
            int distIndex = tableModel.getColumnCount() - 1;

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    // Get point coordinates
                    double[] p1 = new double[dim];
                    double[] p2 = new double[dim];
                    
                    for (int d = 0; d < dim; d++) {
                        p1[d] = Double.parseDouble(tableModel.getValueAt(row, point1Cols.get(d)).toString());
                        p2[d] = Double.parseDouble(tableModel.getValueAt(row, point2Cols.get(d)).toString());
                    }
                    
                    // Calculate n-D slope (product of coordinate ratios)
                    double slope = 1.0;
                    for (int d = 0; d < dim; d++) {
                        slope *= (p2[d] != 0) ? p1[d] / p2[d] : 0;
                    }
                    
                    // Calculate n-D distance as product differences
                    double distance = 0.0;
                    for (int d = 0; d < dim; d += 2) {
                        if (d + 1 < dim) {
                            distance += (p1[d] * p1[d+1]) - (p2[d] * p2[d+1]);
                        }
                    }
                    
                    // Apply absolute distance if selected
                    if (useAbsoluteDistance) {
                        distance = Math.abs(distance);
                    }
                    
                    tableModel.setValueAt(String.format("%.4f", slope), row, slopeIndex);
                    tableModel.setValueAt(String.format("%.4f", distance), row, distIndex);
                    
                } catch (NumberFormatException e) {
                    tableModel.setValueAt("0.0000", row, slopeIndex);
                    tableModel.setValueAt("0.0000", row, distIndex);
                }
            }
        }
    }

    private boolean isColumnNumeric(int columnIndex) {
        try {
            Double.parseDouble(tableModel.getValueAt(0, columnIndex).toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}