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

        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, 
                "No class column found.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get list of numeric columns (excluding class column)
        List<String> numericColumns = new ArrayList<>();
        List<Integer> numericIndices = new ArrayList<>();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != classColumnIndex && isColumnNumeric(i)) {
                numericColumns.add(tableModel.getColumnName(i));
                numericIndices.add(i);
            }
        }

        if (numericColumns.isEmpty()) {
            JOptionPane.showMessageDialog(csvViewer, 
                "No numeric predictor columns found.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create dialog for selecting predictor variables
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), 
            "Multiple Linear Regression");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Show which column is being used as dependent variable
        JLabel depLabel = new JLabel("Predicting class column: " + tableModel.getColumnName(classColumnIndex));
        depLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(depLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Predictor variables selection
        JPanel predictorPanel = new JPanel();
        predictorPanel.setLayout(new BoxLayout(predictorPanel, BoxLayout.Y_AXIS));
        predictorPanel.setBorder(BorderFactory.createTitledBorder("Select Predictor Variables"));
        
        Map<String, JCheckBox> checkboxes = new HashMap<>();
        for (String col : numericColumns) {
            JCheckBox cb = new JCheckBox(col);
            cb.setSelected(true); // Default all to selected
            checkboxes.put(col, cb);
            predictorPanel.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(predictorPanel);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        mainPanel.add(scrollPane);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            List<String> selectedVars = new ArrayList<>();
            for (Map.Entry<String, JCheckBox> entry : checkboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedVars.add(entry.getKey());
                }
            }

            if (selectedVars.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please select at least one predictor variable.", 
                    "Selection Error", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            performRegression(classColumnIndex, selectedVars);
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

    private void performRegression(int classColumnIndex, List<String> predictorVars) {
        int n = tableModel.getRowCount();
        int p = predictorVars.size();
        
        // Create X matrix (with column of 1's for intercept) and Y vector
        double[][] X = new double[n][p + 1];
        double[] y = new double[n];
        Map<String, Integer> varIndices = new HashMap<>();

        // Create class value mapping if class is categorical
        Map<String, Double> classMapping = new HashMap<>();
        List<String> uniqueClasses = new ArrayList<>();
        
        // Collect unique class values
        for (int i = 0; i < n; i++) {
            String classValue = tableModel.getValueAt(i, classColumnIndex).toString();
            if (!uniqueClasses.contains(classValue)) {
                uniqueClasses.add(classValue);
            }
        }

        // Create evenly distributed values from 0 to 1 inclusive
        for (int i = 0; i < uniqueClasses.size(); i++) {
            double normalizedValue = uniqueClasses.size() > 1 ? 
                i / (double)(uniqueClasses.size() - 1) : 1.0;
            classMapping.put(uniqueClasses.get(i), normalizedValue);
        }
        
        // Fill X and y matrices
        for (int i = 0; i < n; i++) {
            X[i][0] = 1.0; // Intercept term
            for (int j = 0; j < predictorVars.size(); j++) {
                int colIndex = tableModel.findColumn(predictorVars.get(j));
                varIndices.put(predictorVars.get(j), colIndex);
                X[i][j + 1] = Double.parseDouble(tableModel.getValueAt(i, colIndex).toString());
            }
            String classValue = tableModel.getValueAt(i, classColumnIndex).toString();
            y[i] = classMapping.get(classValue);
        }

        // Calculate coefficients using normal equation
        double[] coefficients = calculateCoefficients(X, y);
        
        // Calculate R-squared
        double rSquared = calculateRSquared(X, y, coefficients);

        // Create formula string
        StringBuilder formula = new StringBuilder("Predicted_Class = ");
        formula.append(df.format(coefficients[0])); // Intercept
        for (int i = 0; i < predictorVars.size(); i++) {
            formula.append(" + ").append(df.format(coefficients[i + 1]))
                  .append(predictorVars.get(i));
        }
        formula.append(" (R² = ").append(df.format(rSquared)).append(")");

        // Add new column with predicted values
        tableModel.addColumn(formula.toString());
        int newColIndex = tableModel.getColumnCount() - 1;

        // Calculate predicted values
        for (int row = 0; row < n; row++) {
            double predicted = coefficients[0]; // Intercept
            for (int j = 0; j < predictorVars.size(); j++) {
                int colIndex = varIndices.get(predictorVars.get(j));
                predicted += coefficients[j + 1] * 
                    Double.parseDouble(tableModel.getValueAt(row, colIndex).toString());
            }
            tableModel.setValueAt(df.format(predicted), row, newColIndex);
        }

        // Show class value mapping in a dialog
        StringBuilder mappingInfo = new StringBuilder("Class Value Mapping:\n\n");
        for (String className : uniqueClasses) {
            mappingInfo.append(className)
                      .append(" → ")
                      .append(df.format(classMapping.get(className)))
                      .append("\n");
        }
        
        JOptionPane.showMessageDialog(csvViewer,
            mappingInfo.toString(),
            "Class Encoding Information",
            JOptionPane.INFORMATION_MESSAGE);
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