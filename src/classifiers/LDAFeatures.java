package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import src.CsvViewer;

public class LDAFeatures {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double[][] data;
    private double[][] eigenvectors;
    private double[] eigenvalues;
    private double convergenceThreshold = 1e-10;
    private int maxIterations = 100;

    public LDAFeatures(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertLDAComponents() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prepare data
        prepareData(classColumnIndex);
        
        // Calculate LDA components
        computeLDA(classColumnIndex);
        
        // Show dialog to select number of components
        showComponentSelectionDialog();
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

    private void computeLDA(int classColumnIndex) {
        Set<String> uniqueClasses = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            uniqueClasses.add(tableModel.getValueAt(i, classColumnIndex).toString());
        }
        ArrayList<String> classes = new ArrayList<>(uniqueClasses);

        int numClasses = classes.size();
        int numFeatures = data[0].length;

        // Calculate within-class scatter matrix
        double[][] Sw = new double[numFeatures][numFeatures];
        Map<String, ArrayList<double[]>> classData = new HashMap<>();

        // Group data by class
        for (int i = 0; i < data.length; i++) {
            String className = tableModel.getValueAt(i, classColumnIndex).toString();
            if (!classData.containsKey(className)) {
                classData.put(className, new ArrayList<>());
            }
            classData.get(className).add(data[i]);
        }

        // Calculate class means and within-class scatter
        for (String className : classes) {
            ArrayList<double[]> classPoints = classData.get(className);
            double[] classMean = new double[numFeatures];
            
            // Calculate class mean
            for (double[] point : classPoints) {
                for (int j = 0; j < numFeatures; j++) {
                    classMean[j] += point[j];
                }
            }
            for (int j = 0; j < numFeatures; j++) {
                classMean[j] /= classPoints.size();
            }

            // Add to within-class scatter
            for (double[] point : classPoints) {
                for (int j = 0; j < numFeatures; j++) {
                    for (int k = 0; k < numFeatures; k++) {
                        Sw[j][k] += (point[j] - classMean[j]) * (point[k] - classMean[k]);
                    }
                }
            }
        }

        // Calculate between-class scatter matrix
        double[][] Sb = new double[numFeatures][numFeatures];
        double[] globalMean = new double[numFeatures];
        
        // Calculate global mean
        for (double[] row : data) {
            for (int j = 0; j < numFeatures; j++) {
                globalMean[j] += row[j];
            }
        }
        for (int j = 0; j < numFeatures; j++) {
            globalMean[j] /= data.length;
        }

        // Calculate between-class scatter
        for (String className : classes) {
            ArrayList<double[]> classPoints = classData.get(className);
            double[] classMean = new double[numFeatures];
            
            // Calculate class mean
            for (double[] point : classPoints) {
                for (int j = 0; j < numFeatures; j++) {
                    classMean[j] += point[j];
                }
            }
            for (int j = 0; j < numFeatures; j++) {
                classMean[j] /= classPoints.size();
            }

            // Add to between-class scatter
            for (int j = 0; j < numFeatures; j++) {
                for (int k = 0; k < numFeatures; k++) {
                    Sb[j][k] += classPoints.size() * (classMean[j] - globalMean[j]) * 
                               (classMean[k] - globalMean[k]);
                }
            }
        }

        // Solve generalized eigenvalue problem Sb * v = λ * Sw * v
        // For simplicity, we'll use Sw^-1 * Sb
        double[][] SwInv = inverse(Sw);
        double[][] SwInvSb = matrixMultiply(SwInv, Sb);
        
        // Get eigenvectors using power iteration
        int maxComponents = Math.min(numClasses - 1, numFeatures);
        eigenvectors = new double[numFeatures][maxComponents];
        eigenvalues = new double[maxComponents];
        
        for (int i = 0; i < maxComponents; i++) {
            double[] vector = new double[numFeatures];
            vector[i] = 1.0;
            
            // Power iteration
            for (int iter = 0; iter < maxIterations; iter++) {
                double[] newVector = matrixVectorMultiply(SwInvSb, vector);
                normalize(newVector);
                
                if (convergence(vector, newVector)) {
                    break;
                }
                vector = newVector;
            }
            
            // Store eigenvector
            for (int j = 0; j < numFeatures; j++) {
                eigenvectors[j][i] = vector[j];
            }
            
            // Calculate eigenvalue
            double[] Av = matrixVectorMultiply(SwInvSb, vector);
            eigenvalues[i] = dotProduct(vector, Av);
            
            // Deflate matrix
            for (int j = 0; j < numFeatures; j++) {
                for (int k = 0; k < numFeatures; k++) {
                    SwInvSb[j][k] -= eigenvalues[i] * vector[j] * vector[k];
                }
            }
        }
    }

    private void showComponentSelectionDialog() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(csvViewer.getTable());
        JDialog dialog = new JDialog(parent, "LDA Feature Configuration", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Max iterations spinner
        SpinnerNumberModel iterModel = new SpinnerNumberModel(100, 10, 1000, 10);
        JSpinner iterSpinner = new JSpinner(iterModel);
        JPanel iterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iterPanel.add(new JLabel("Max iterations:"));
        iterPanel.add(iterSpinner);
        panel.add(iterPanel);
        
        // Convergence threshold as percentage
        SpinnerNumberModel threshModel = new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1);
        JSpinner threshSpinner = new JSpinner(threshModel);
        JPanel threshPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        threshPanel.add(new JLabel("Convergence threshold (%):"));
        threshPanel.add(threshSpinner);
        panel.add(threshPanel);
        
        // Normalize checkbox
        JCheckBox normalizeBox = new JCheckBox("Normalize component to [0,1]", true);
        panel.add(normalizeBox);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            maxIterations = (Integer) iterSpinner.getValue();
            // Convert percentage to decimal
            convergenceThreshold = (Double) threshSpinner.getValue() / 100.0;
            boolean normalize = normalizeBox.isSelected();
            dialog.dispose();
            insertComponents(1, normalize);
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void insertComponents(int numComponents, boolean normalize) {
        DecimalFormat df = new DecimalFormat("#.###");
        
        // Project data onto LDA components
        for (int k = 0; k < numComponents; k++) {
            String columnName = csvViewer.getUniqueColumnName("LDA" + (k + 1));
            tableModel.addColumn(columnName);
            int newColIndex = tableModel.getColumnCount() - 1;
            
            double[] projections = new double[data.length];
            
            // Calculate projections
            for (int i = 0; i < data.length; i++) {
                double projection = 0;
                for (int j = 0; j < data[0].length; j++) {
                    projection += data[i][j] * eigenvectors[j][k];
                }
                projections[i] = projection;
            }
            
            // Normalize if requested
            if (normalize) {
                double min = Arrays.stream(projections).min().getAsDouble();
                double max = Arrays.stream(projections).max().getAsDouble();
                double range = max - min;
                
                for (int i = 0; i < data.length; i++) {
                    double normalizedValue = (projections[i] - min) / range;
                    tableModel.setValueAt(df.format(normalizedValue), i, newColIndex);
                }
            } else {
                for (int i = 0; i < data.length; i++) {
                    tableModel.setValueAt(df.format(projections[i]), i, newColIndex);
                }
            }
        }
    }

    // Matrix operation helper methods
    private double[][] inverse(double[][] matrix) {
        // Simple Gaussian elimination for matrix inversion
        int n = matrix.length;
        double[][] augmented = new double[n][2*n];
        
        // Create augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][i + n] = 1;
        }
        
        // Forward elimination
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
        
        // Extract inverse
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmented[i], n, inverse[i], 0, n);
        }
        
        return inverse;
    }

    private double[][] matrixMultiply(double[][] a, double[][] b) {
        int m = a.length;
        int n = b[0].length;
        int p = a[0].length;
        double[][] result = new double[m][n];
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        
        return result;
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
        return diff < convergenceThreshold;
    }
} 