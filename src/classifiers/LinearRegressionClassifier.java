package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import src.CsvViewer;

public class LinearRegressionClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private final DecimalFormat df = new DecimalFormat("#.###");

    public LinearRegressionClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertLinearRegression() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        // Get list of numeric columns
        List<String> numericColumns = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (isColumnNumeric(i)) {
                numericColumns.add(tableModel.getColumnName(i));
            }
        }

        if (numericColumns.size() < 2) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two numeric columns for regression.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create dialog for selecting dependent and independent variables
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), "Linear Regression");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> dependentVar = new JComboBox<>(numericColumns.toArray(new String[0]));
        JComboBox<String> independentVar = new JComboBox<>(numericColumns.toArray(new String[0]));

        mainPanel.add(new JLabel("Dependent Variable (Y):"));
        mainPanel.add(dependentVar);
        mainPanel.add(new JLabel("Independent Variable (X):"));
        mainPanel.add(independentVar);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            String yColumn = (String) dependentVar.getSelectedItem();
            String xColumn = (String) independentVar.getSelectedItem();
            
            if (yColumn.equals(xColumn)) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please select different columns for X and Y.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            performRegression(yColumn, xColumn);
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer);
        dialog.setVisible(true);
    }

    private void performRegression(String yColumn, String xColumn) {
        int xColIndex = tableModel.findColumn(xColumn);
        int yColIndex = tableModel.findColumn(yColumn);

        // Calculate means
        double sumX = 0, sumY = 0, n = 0;
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            try {
                double x = Double.parseDouble(tableModel.getValueAt(row, xColIndex).toString());
                double y = Double.parseDouble(tableModel.getValueAt(row, yColIndex).toString());
                xValues.add(x);
                yValues.add(y);
                sumX += x;
                sumY += y;
                n++;
            } catch (NumberFormatException ignored) {}
        }

        double meanX = sumX / n;
        double meanY = sumY / n;

        // Calculate slope and intercept
        double numerator = 0, denominator = 0;
        for (int i = 0; i < xValues.size(); i++) {
            double x = xValues.get(i);
            double y = yValues.get(i);
            numerator += (x - meanX) * (y - meanY);
            denominator += (x - meanX) * (x - meanX);
        }

        double slope = numerator / denominator;
        double intercept = meanY - slope * meanX;

        // Calculate R-squared
        double rSquared = calculateRSquared(xValues, yValues, slope, intercept, meanY);

        // Create new column with predicted values
        String formula = String.format("Y = %sx + %s (R² = %s)", 
            df.format(slope), 
            df.format(intercept),
            df.format(rSquared));
        
        tableModel.addColumn(formula);
        int newColIndex = tableModel.getColumnCount() - 1;

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            try {
                double x = Double.parseDouble(tableModel.getValueAt(row, xColIndex).toString());
                double predicted = slope * x + intercept;
                tableModel.setValueAt(df.format(predicted), row, newColIndex);
            } catch (NumberFormatException e) {
                tableModel.setValueAt("", row, newColIndex);
            }
        }
    }

    private double calculateRSquared(List<Double> x, List<Double> y, double slope, double intercept, double meanY) {
        double ssRes = 0, ssTot = 0;
        for (int i = 0; i < x.size(); i++) {
            double predicted = slope * x.get(i) + intercept;
            ssRes += Math.pow(y.get(i) - predicted, 2);
            ssTot += Math.pow(y.get(i) - meanY, 2);
        }
        return 1 - (ssRes / ssTot);
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