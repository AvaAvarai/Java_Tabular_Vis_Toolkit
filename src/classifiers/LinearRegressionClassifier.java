package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
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
        List<Integer> numericIndices = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (isColumnNumeric(i) && !tableModel.getColumnName(i).equalsIgnoreCase("class")) {
                numericColumns.add(tableModel.getColumnName(i));
                numericIndices.add(i);
            }
        }

        if (numericColumns.size() < 2) {
            JOptionPane.showMessageDialog(csvViewer, 
                "Need at least two numeric columns for regression.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create dialog for selecting variables
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), "Multiple Linear Regression");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Dependent variable selection
        JPanel depPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        depPanel.add(new JLabel("Dependent Variable (Y):"));
        JComboBox<String> dependentVar = new JComboBox<>(numericColumns.toArray(new String[0]));
        depPanel.add(dependentVar);
        mainPanel.add(depPanel);

        // Independent variables selection
        JPanel indepPanel = new JPanel();
        indepPanel.setLayout(new BoxLayout(indepPanel, BoxLayout.Y_AXIS));
        indepPanel.setBorder(BorderFactory.createTitledBorder("Independent Variables (X)"));
        
        Map<String, JCheckBox> checkboxes = new HashMap<>();
        for (String col : numericColumns) {
            JCheckBox cb = new JCheckBox(col);
            checkboxes.put(col, cb);
            indepPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(indepPanel);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        mainPanel.add(scrollPane);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            String depVarName = (String) dependentVar.getSelectedItem();
            List<String> selectedVars = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : checkboxes.entrySet()) {
                if (entry.getValue().isSelected() && !entry.getKey().equals(depVarName)) {
                    selectedVars.add(entry.getKey());
                }
            }

            if (selectedVars.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please select at least one independent variable.", 
                    "Selection Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            performRegression(depVarName, selectedVars);
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

    private void performRegression(String depVarName, List<String> independentVars) {
        int n = tableModel.getRowCount();
        int p = independentVars.size();
        
        // Create X matrix (with column of 1's for intercept) and Y vector
        double[][] X = new double[n][p + 1];
        double[] y = new double[n];
        
        int depColIndex = tableModel.findColumn(depVarName);
        Map<String, Integer> varIndices = new HashMap<>();
        
        // Fill X and y matrices
        for (int i = 0; i < n; i++) {
            X[i][0] = 1.0; // Intercept term
            for (int j = 0; j < independentVars.size(); j++) {
                int colIndex = tableModel.findColumn(independentVars.get(j));
                varIndices.put(independentVars.get(j), colIndex);
                X[i][j + 1] = Double.parseDouble(tableModel.getValueAt(i, colIndex).toString());
            }
            y[i] = Double.parseDouble(tableModel.getValueAt(i, depColIndex).toString());
        }

        // Calculate coefficients using normal equation: β = (X'X)^-1 X'y
        double[] coefficients = calculateCoefficients(X, y);
        
        // Calculate R-squared
        double rSquared = calculateRSquared(X, y, coefficients);

        // Create formula string
        StringBuilder formula = new StringBuilder(depVarName + " = ");
        formula.append(df.format(coefficients[0])); // Intercept
        for (int i = 0; i < independentVars.size(); i++) {
            formula.append(" + ").append(df.format(coefficients[i + 1]))
                  .append(independentVars.get(i));
        }
        formula.append(" (R² = ").append(df.format(rSquared)).append(")");

        // Add new column with predicted values
        tableModel.addColumn(formula.toString());
        int newColIndex = tableModel.getColumnCount() - 1;

        // Calculate predicted values
        for (int row = 0; row < n; row++) {
            double predicted = coefficients[0]; // Intercept
            for (int j = 0; j < independentVars.size(); j++) {
                int colIndex = varIndices.get(independentVars.get(j));
                predicted += coefficients[j + 1] * 
                    Double.parseDouble(tableModel.getValueAt(row, colIndex).toString());
            }
            tableModel.setValueAt(df.format(predicted), row, newColIndex);
        }
    }

    private double[] calculateCoefficients(double[][] X, double[] y) {
        int n = X.length;
        int p = X[0].length;
        
        // Calculate X'X
        double[][] XtX = new double[p][p];
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < p; j++) {
                for (int k = 0; k < n; k++) {
                    XtX[i][j] += X[k][i] * X[k][j];
                }
            }
        }
        
        // Calculate X'y
        double[] Xty = new double[p];
        for (int i = 0; i < p; i++) {
            for (int k = 0; k < n; k++) {
                Xty[i] += X[k][i] * y[k];
            }
        }
        
        // Solve system using Gaussian elimination
        return solveSystem(XtX, Xty);
    }

    private double[] solveSystem(double[][] A, double[] b) {
        int n = A.length;
        double[][] augmented = new double[n][n + 1];
        
        // Create augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, augmented[i], 0, n);
            augmented[i][n] = b[i];
        }
        
        // Gaussian elimination
        for (int i = 0; i < n; i++) {
            // Find pivot
            int maxRow = i;
            for (int k = i + 1; k < n; k++) {
                if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                    maxRow = k;
                }
            }
            
            // Swap maximum row with current row
            double[] temp = augmented[i];
            augmented[i] = augmented[maxRow];
            augmented[maxRow] = temp;
            
            // Make all rows below this one 0 in current column
            for (int k = i + 1; k < n; k++) {
                double factor = augmented[k][i] / augmented[i][i];
                for (int j = i; j <= n; j++) {
                    augmented[k][j] -= factor * augmented[i][j];
                }
            }
        }
        
        // Back substitution
        double[] solution = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += augmented[i][j] * solution[j];
            }
            solution[i] = (augmented[i][n] - sum) / augmented[i][i];
        }
        
        return solution;
    }

    private double calculateRSquared(double[][] X, double[] y, double[] coefficients) {
        double yMean = Arrays.stream(y).average().orElse(0.0);
        double ssTotal = Arrays.stream(y).map(val -> Math.pow(val - yMean, 2)).sum();
        
        double ssResidual = 0.0;
        for (int i = 0; i < y.length; i++) {
            double predicted = coefficients[0];
            for (int j = 1; j < coefficients.length; j++) {
                predicted += coefficients[j] * X[i][j];
            }
            ssResidual += Math.pow(y[i] - predicted, 2);
        }
        
        return 1.0 - (ssResidual / ssTotal);
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