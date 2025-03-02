package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import src.CsvViewer;
import java.util.*;
import java.text.DecimalFormat;

/**
 * This class implements the Linear Discriminant Analysis (LDA) algorithm for dimensionality reduction.
 * It is designed to work with CSV data and provides a method to insert the LDA classification into the data model.
 * The LDA algorithm supports multiple classes and uses the power iteration method to find the eigenvectors.
 */
public class LinearDiscriminantAnalysisClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;

    /**
     * Constructor for LinearDiscriminantAnalysisClassifier.
     * 
     * @param csvViewer The CSV viewer object that provides access to the CSV data.
     * @param tableModel The table model that represents the CSV data.
     */
    public LinearDiscriminantAnalysisClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    /**
     * Inserts the LDA classification into the table model.
     * This method performs the following steps:
     * 1. Checks if there is any data loaded.
     * 2. Identifies the class column index.
     * 3. Collects data by class and calculates class means.
     * 4. Calculates the global mean.
     * 5. Computes the within-class scatter matrix (Sw) and between-class scatter matrix (Sb).
     * 6. Calculates the inverse of Sw and multiplies it by Sb to find the projection matrix.
     * 7. Finds the eigenvectors of the projection matrix using power iteration.
     * 8. Builds the LDA function string based on the eigenvectors.
     * 9. Projects the data onto the LDA space and normalizes the projections to [0,1] range.
     * 10. Adds the LDA classification to the table model.
     */
    public void insertLDAClassification() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // TODO: make an option to select if the user wants to put function to the name or not
        // Get unique classes and their counts
        Map<String, List<double[]>> classData = new HashMap<>();
        Set<String> uniqueClasses = new HashSet<>();
        int numFeatures = tableModel.getColumnCount() - 1;

        // Collect data by class
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            uniqueClasses.add(className);
            
            double[] features = new double[numFeatures];
            int featureIndex = 0;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                if (col != classColumnIndex) {
                    features[featureIndex++] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                }
            }
            
            classData.computeIfAbsent(className, k -> new ArrayList<>()).add(features);
        }

        // Calculate class means
        Map<String, double[]> classMeans = new HashMap<>();
        for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
            double[] mean = new double[numFeatures];
            List<double[]> samples = entry.getValue();
            
            for (double[] sample : samples) {
                for (int i = 0; i < numFeatures; i++) {
                    mean[i] += sample[i];
                }
            }
            
            for (int i = 0; i < numFeatures; i++) {
                mean[i] /= samples.size();
            }
            
            classMeans.put(entry.getKey(), mean);
        }

        // Calculate global mean
        double[] globalMean = new double[numFeatures];
        int totalSamples = 0;
        for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
            double[] classMean = classMeans.get(entry.getKey());
            int classSize = entry.getValue().size();
            totalSamples += classSize;
            
            for (int i = 0; i < numFeatures; i++) {
                globalMean[i] += classMean[i] * classSize;
            }
        }
        for (int i = 0; i < numFeatures; i++) {
            globalMean[i] /= totalSamples;
        }

        // Calculate within-class scatter matrix
        double[][] Sw = new double[numFeatures][numFeatures];
        for (List<double[]> samples : classData.values()) {
            for (double[] sample : samples) {
                for (int i = 0; i < numFeatures; i++) {
                    for (int j = 0; j < numFeatures; j++) {
                        Sw[i][j] += (sample[i] - globalMean[i]) * (sample[j] - globalMean[j]);
                    }
                }
            }
        }

        // Calculate between-class scatter matrix
        double[][] Sb = new double[numFeatures][numFeatures];
        for (Map.Entry<String, List<double[]>> entry : classData.entrySet()) {
            double[] classMean = classMeans.get(entry.getKey());
            int classSize = entry.getValue().size();
            
            for (int i = 0; i < numFeatures; i++) {
                for (int j = 0; j < numFeatures; j++) {
                    Sb[i][j] += classSize * (classMean[i] - globalMean[i]) * (classMean[j] - globalMean[j]);
                }
            }
        }

        // Add small regularization to Sw to ensure it's invertible
        double epsilon = 1e-10;
        for (int i = 0; i < numFeatures; i++) {
            Sw[i][i] += epsilon;
        }

        // Calculate Sw inverse
        double[][] SwInv = inverse(Sw);
        
        // Calculate SwInv * Sb
        double[][] M = multiply(SwInv, Sb);

        // Find eigenvectors using power iteration
        double[][] eigenVectors = powerIteration(M, uniqueClasses.size()); // Get all eigenvectors

        // Build the LDA function string
        StringBuilder ldaFunction = new StringBuilder("LDA = ");
        DecimalFormat df = new DecimalFormat("#.####");
        int featureIndex = 0;
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            if (col != classColumnIndex) {
                double coefficient = eigenVectors[featureIndex][0];
                if (featureIndex > 0 && coefficient >= 0) {
                    ldaFunction.append(" + ");
                }
                ldaFunction.append(df.format(coefficient)).append(tableModel.getColumnName(col));
                featureIndex++;
            }
        }

        // Project data onto LDA space
        tableModel.addColumn(ldaFunction.toString());
        int newColIndex = tableModel.getColumnCount() - 1;

        // Project and normalize to [0,1] range
        double minProj = Double.MAX_VALUE;
        double maxProj = Double.MIN_VALUE;
        double[] projections = new double[tableModel.getRowCount()];

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] features = new double[numFeatures];
            featureIndex = 0;
            for (int col = 0; col < tableModel.getColumnCount() - 1; col++) {
                if (col != classColumnIndex) {
                    features[featureIndex++] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                }
            }
            
            double projection = 0;
            for (int i = 0; i < numFeatures; i++) {
                projection += features[i] * eigenVectors[i][0];
            }
            projections[row] = projection;
            
            if (projection < minProj) minProj = projection;
            if (projection > maxProj) maxProj = projection;
        }

        // Normalize and set values
        double range = maxProj - minProj;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double normalizedValue = (projections[row] - minProj) / range;
            tableModel.setValueAt(df.format(normalizedValue), row, newColIndex);
        }
    }

    /**
     * Calculates the inverse of a matrix using Gaussian elimination.
     * 
     * @param matrix The matrix to invert.
     * @return The inverse of the matrix.
     */
    private double[][] inverse(double[][] matrix) {
        int n = matrix.length;
        double[][] augmented = new double[n][2*n];
        
        // Create augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n + i] = 1.0;
        }
        
        // Gaussian elimination
        for (int i = 0; i < n; i++) {
            double pivot = augmented[i][i];
            for (int j = 0; j < 2*n; j++) {
                augmented[i][j] /= pivot;
            }
            
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    double factor = augmented[k][i];
                    for (int j = 0; j < 2*n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
        }
        
        // Extract inverse matrix
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }
        
        return inverse;
    }

    /**
     * Multiplies two matrices.
     * 
     * @param A The first matrix.
     * @param B The second matrix.
     * @return The product of A and B.
     */
    private double[][] multiply(double[][] A, double[][] B) {
        int m = A.length;
        int n = B[0].length;
        int p = A[0].length;
        double[][] result = new double[m][n];
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        
        return result;
    }

    /**
     * Finds the eigenvectors of a matrix using power iteration.
     * 
     * @param matrix The matrix to find eigenvectors for.
     * @param numComponents The number of eigenvectors to find.
     * @return The eigenvectors of the matrix.
     */
    private double[][] powerIteration(double[][] matrix, int numComponents) {
        int n = matrix.length;
        double[][] eigenvectors = new double[n][numComponents];
        
        Random rand = new Random(42); // Fixed seed for reproducibility
        
        for (int comp = 0; comp < numComponents; comp++) {
            // Initialize random vector
            double[] vector = new double[n];
            for (int i = 0; i < n; i++) {
                vector[i] = rand.nextDouble();
            }
            
            // Power iteration
            for (int iter = 0; iter < 100; iter++) {
                double[] newVector = new double[n];
                
                // Matrix-vector multiplication
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        newVector[i] += matrix[i][j] * vector[j];
                    }
                }
                
                // Normalize
                double norm = 0;
                for (int i = 0; i < n; i++) {
                    norm += newVector[i] * newVector[i];
                }
                norm = Math.sqrt(norm);
                
                for (int i = 0; i < n; i++) {
                    vector[i] = newVector[i] / norm;
                }
            }
            
            // Store eigenvector
            for (int i = 0; i < n; i++) {
                eigenvectors[i][comp] = vector[i];
            }
            
            // Deflate matrix for next component
            if (comp < numComponents - 1) {
                double[][] outer = new double[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        outer[i][j] = vector[i] * vector[j];
                    }
                }
                
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        matrix[i][j] -= outer[i][j];
                    }
                }
            }
        }
        
        return eigenvectors;
    }
}