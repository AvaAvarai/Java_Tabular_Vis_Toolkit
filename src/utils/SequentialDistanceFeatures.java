package src.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import src.CsvViewer;

public class SequentialDistanceFeatures {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private boolean isForward = true; // Default direction for calculations

    public SequentialDistanceFeatures(CsvViewer csvViewer, DefaultTableModel tableModel, JTable table) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
        this.table = table;
    }

    public void calculateDistances() {
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

        boolean[] selected = new boolean[numericColumns.size()];
        showColumnSelectionDialog(numericColumns, columnNames, selected);

        // Check if any columns were selected
        int selectedCount = 0;
        for (boolean b : selected) if (b) selectedCount++;

        if (selectedCount >= 2) {
            calculateDistancesForSelected(numericColumns, selected);
        } else if (selectedCount > 0) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two columns selected.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showColumnSelectionDialog(List<Integer> numericColumns, List<String> columnNames, boolean[] selected) {
        JDialog dialog = new JDialog(csvViewer, "Select Columns for Distances", true);
        dialog.setLayout(new BorderLayout(5, 5));

        // Create panel with compact grid
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 3, 5, 2)); // 3 columns, 5 horizontal gap, 2 vertical gap
        checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JCheckBox[] boxes = new JCheckBox[numericColumns.size()];
        
        for (int i = 0; i < numericColumns.size(); i++) {
            boxes[i] = new JCheckBox(columnNames.get(i), true);
            boxes[i].setFont(boxes[i].getFont().deriveFont(11f)); // Slightly smaller font
            checkBoxPanel.add(boxes[i]);
            selected[i] = true;
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

        // Add separator and radio buttons for direction selection
        JPanel directionPanel = new JPanel();
        directionPanel.setLayout(new BoxLayout(directionPanel, BoxLayout.Y_AXIS));

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); // Full width line

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton forwardButton = new JRadioButton("Forward", true);
        JRadioButton backwardButton = new JRadioButton("Backward");

        ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(forwardButton);
        directionGroup.add(backwardButton);

        forwardButton.addActionListener(e -> isForward = true);
        backwardButton.addActionListener(e -> isForward = false);

        radioPanel.add(new JLabel("Select Direction:"));
        radioPanel.add(forwardButton);
        radioPanel.add(backwardButton);

        directionPanel.add(separator);
        directionPanel.add(radioPanel);
        dialog.add(directionPanel, BorderLayout.SOUTH);

        // OK/Cancel buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            for (int i = 0; i < boxes.length; i++) {
                selected[i] = boxes[i].isSelected();
            }
            dialog.dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            for (int i = 0; i < selected.length; i++) selected[i] = false;
            dialog.dispose();
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        directionPanel.add(buttonPanel);

        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer);
        dialog.setVisible(true);
    }

    private void calculateDistancesForSelected(List<Integer> columns, boolean[] selected) {
        List<Integer> selectedColumns = new ArrayList<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                selectedColumns.add(columns.get(i));
            }
        }

        if (selectedColumns.size() < 2) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two columns selected.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Calculate distances for selected columns
        for (int i = 0; i < selectedColumns.size(); i++) {
            int currentCol = selectedColumns.get(i);
            int nextCol;

            if (isForward) {
                nextCol = selectedColumns.get((i + 1) % selectedColumns.size()); // Forward direction
            } else {
                nextCol = selectedColumns.get((i - 1 + selectedColumns.size()) % selectedColumns.size()); // Backward direction
            }

            String columnName = csvViewer.getUniqueColumnName(
                "Distance(" + tableModel.getColumnName(currentCol) + 
                "-" + tableModel.getColumnName(nextCol) + ")");
            
            tableModel.addColumn(columnName);
            int newColIndex = tableModel.getColumnCount() - 1;
            
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    double current = Double.parseDouble(tableModel.getValueAt(row, currentCol).toString());
                    double next = Double.parseDouble(tableModel.getValueAt(row, nextCol).toString());
                    
                    double distance = next - current;
                    tableModel.setValueAt(String.format("%.4f", distance), row, newColIndex);
                } catch (NumberFormatException e) {
                    tableModel.setValueAt("0.0000", row, newColIndex);
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