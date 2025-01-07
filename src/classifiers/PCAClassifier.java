package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import src.CsvViewer;

public class PCAClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double[][] data;
    private double[][] V;  // Right singular vectors
    private double[] singularValues;

    public PCAClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertPCAClassification() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Get unique classes in order of appearance
        ArrayList<String> uniqueClasses = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            if (!uniqueClasses.contains(className)) {
                uniqueClasses.add(className);
            }
        }

        // Create evenly distributed values from 0 to 1 inclusive
        Map<String, Double> normalizedMap = new HashMap<>();
        int numClasses = uniqueClasses.size();
        for (int i = 0; i < numClasses; i++) {
            normalizedMap.put(uniqueClasses.get(i), i / (double)(numClasses - 1));
        }

        // Prepare data matrix
        prepareData(classColumnIndex);
        
        // Perform SVD
        performSVD();

        // Project onto first principal component
        double[] projections = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                projections[i] += data[i][j] * V[j][0];  // Project onto first PC
            }
        }

        // Normalize projections to [0,1]
        double minProj = Arrays.stream(projections).min().getAsDouble();
        double maxProj = Arrays.stream(projections).max().getAsDouble();
        double range = maxProj - minProj;

        // Add prediction column
        String columnName = csvViewer.getUniqueColumnName("PCA_prediction");
        tableModel.addColumn(columnName);
        int newColumnIndex = tableModel.getColumnCount() - 1;

        // Fill in normalized projections
        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double normalizedValue = (projections[row] - minProj) / range;
            tableModel.setValueAt(df.format(normalizedValue), row, newColumnIndex);
        }
    }

    private void prepareData(int classColumnIndex) {
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