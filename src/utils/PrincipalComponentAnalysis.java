package src.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import src.CsvViewer;

public class PrincipalComponentAnalysis {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double[][] data;
    private double[][] V;  // Right singular vectors (principal components)
    private double[] singularValues;

    public PrincipalComponentAnalysis(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertPrincipalComponents() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        // Prepare data matrix
        prepareData();
        
        // Perform SVD
        performSVD();

        // Show dialog to select number of components
        showComponentSelectionDialog();
    }

    private void prepareData() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount() - 1; // Exclude class column
        data = new double[rows][cols];
        
        // Convert data to matrix and center it
        double[] means = new double[cols];
        
        // Calculate means (skip class column)
        int dataCol = 0;
        for (int j = 0; j < tableModel.getColumnCount(); j++) {
            if (j == classColumnIndex) continue;
            
            double sum = 0;
            for (int i = 0; i < rows; i++) {
                double value = Double.parseDouble(tableModel.getValueAt(i, j).toString());
                sum += value;
                data[i][dataCol] = value;
            }
            means[dataCol] = sum / rows;
            dataCol++;
        }
        
        // Center the data
        for (int j = 0; j < cols; j++) {
            for (int i = 0; i < rows; i++) {
                data[i][j] -= means[j];
            }
        }
    }

    private void performSVD() {
        int m = data.length;
        int n = data[0].length;
        V = new double[n][n];
        singularValues = new double[Math.min(m, n)];
        
        // Initialize V with identity matrix
        for (int i = 0; i < n; i++) {
            V[i][i] = 1.0;
        }
        
        // Compute A^T * A
        double[][] ATA = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < m; k++) {
                    ATA[i][j] += data[k][i] * data[k][j];
                }
            }
        }
        
        // Power iteration to find eigenvectors
        for (int k = 0; k < n; k++) {
            double[] vector = new double[n];
            vector[k] = 1.0;
            
            // Power iteration
            for (int iter = 0; iter < 100; iter++) {
                double[] newVector = matrixVectorMultiply(ATA, vector);
                normalize(newVector);
                
                if (convergence(vector, newVector)) {
                    break;
                }
                vector = newVector;
            }
            
            // Store eigenvector
            for (int i = 0; i < n; i++) {
                V[i][k] = vector[i];
            }
            
            // Compute singular value
            double[] Av = matrixVectorMultiply(ATA, vector);
            singularValues[k] = Math.sqrt(dotProduct(vector, Av));
            
            // Deflate matrix
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    ATA[i][j] -= singularValues[k] * vector[i] * vector[j];
                }
            }
        }
    }

    private void showComponentSelectionDialog() {
        int maxComponents = Math.min(data.length, data[0].length);
        
        // Cast the ancestor to Frame explicitly
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(csvViewer.getTable());
        JDialog dialog = new JDialog(parent, "Select Principal Components", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Spinner for number of components
        SpinnerNumberModel model = new SpinnerNumberModel(2, 1, maxComponents, 1);
        JSpinner spinner = new JSpinner(model);
        panel.add(new JLabel("Number of components to insert:"));
        panel.add(spinner);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int numComponents = (Integer) spinner.getValue();
            dialog.dispose();
            insertComponents(numComponents);
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(csvViewer.getTable()));
        dialog.setVisible(true);
    }

    private void insertComponents(int numComponents) {
        DecimalFormat df = new DecimalFormat("#.###");
        
        // Project data onto principal components
        for (int k = 0; k < numComponents; k++) {
            String columnName = csvViewer.getUniqueColumnName("PC" + (k + 1));
            tableModel.addColumn(columnName);
            int newColIndex = tableModel.getColumnCount() - 1;
            
            for (int i = 0; i < data.length; i++) {
                double projection = 0;
                for (int j = 0; j < data[0].length; j++) {
                    projection += data[i][j] * V[j][k];
                }
                tableModel.setValueAt(df.format(projection), i, newColIndex);
            }
        }
    }

    private double[] matrixVectorMultiply(double[][] matrix, double[] vector) {
        int n = vector.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    private void normalize(double[] vector) {
        double norm = Math.sqrt(dotProduct(vector, vector));
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }

    private double dotProduct(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private boolean convergence(double[] old, double[] current) {
        double diff = 0;
        for (int i = 0; i < old.length; i++) {
            diff += Math.abs(old[i] - current[i]);
        }
        return diff < 1e-10;
    }
} 