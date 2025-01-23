package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import src.CsvViewer;

public class MultiLayerPerceptronClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double learningRate = 0.1;
    private int epochs = 100;
    private int hiddenLayerSize = 4;
    private double[][] inputWeights;  // Weights between input and hidden layer
    private double[][] outputWeights; // Weights between hidden and output layer
    private double[] hiddenBiases;    // Biases for hidden layer
    private double[] outputBiases;     // Biases for output layer
    
    public MultiLayerPerceptronClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertMLPClassification() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        showConfigDialog();
    }

    private void showConfigDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), 
                                   "MLP Configuration");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Learning rate input
        mainPanel.add(new JLabel("Learning Rate:"));
        JTextField learningRateField = new JTextField(String.valueOf(learningRate));
        mainPanel.add(learningRateField);

        // Epochs input
        mainPanel.add(new JLabel("Epochs:"));
        JTextField epochsField = new JTextField(String.valueOf(epochs));
        mainPanel.add(epochsField);

        // Hidden layer size input
        mainPanel.add(new JLabel("Hidden Layer Size:"));
        JTextField hiddenLayerField = new JTextField(String.valueOf(hiddenLayerSize));
        mainPanel.add(hiddenLayerField);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            try {
                learningRate = Double.parseDouble(learningRateField.getText());
                epochs = Integer.parseInt(epochsField.getText());
                hiddenLayerSize = Integer.parseInt(hiddenLayerField.getText());
                dialog.dispose();
                trainAndPredict();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter valid numeric values.", 
                    "Invalid Input", 
                    JOptionPane.ERROR_MESSAGE);
            }
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

    private void trainAndPredict() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prepare data
        List<double[]> features = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Set<String> uniqueLabels = new HashSet<>();
        Map<String, Double> labelMap = new HashMap<>();

        // First pass - collect unique labels
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String label = tableModel.getValueAt(row, classColumnIndex).toString();
            uniqueLabels.add(label);
        }

        // Create normalized mapping for labels
        List<String> sortedLabels = new ArrayList<>(uniqueLabels);
        Collections.sort(sortedLabels);
        for (int i = 0; i < sortedLabels.size(); i++) {
            labelMap.put(sortedLabels.get(i), i / (double)(sortedLabels.size() - 1));
        }

        // Second pass - collect features and labels
        int numFeatures = tableModel.getColumnCount() - 1;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] featureRow = new double[numFeatures];
            int featureIndex = 0;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                if (col != classColumnIndex) {
                    featureRow[featureIndex++] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                }
            }
            features.add(featureRow);
            labels.add(tableModel.getValueAt(row, classColumnIndex).toString());
        }

        // Initialize network
        initializeNetwork(numFeatures);

        // Train network
        train(features, labels, labelMap);

        // Make predictions
        String columnName = csvViewer.getUniqueColumnName("MLP_Prediction");
        tableModel.addColumn(columnName);
        int predictionColumnIndex = tableModel.getColumnCount() - 1;
        
        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] input = features.get(row);
            double prediction = predict(input);
            tableModel.setValueAt(df.format(prediction), row, predictionColumnIndex);
        }
    }

    private void initializeNetwork(int numFeatures) {
        Random random = new Random(42);
        
        // Initialize weights with random values between -1 and 1
        inputWeights = new double[numFeatures][hiddenLayerSize];
        outputWeights = new double[hiddenLayerSize][1];
        
        for (int i = 0; i < numFeatures; i++) {
            for (int j = 0; j < hiddenLayerSize; j++) {
                inputWeights[i][j] = random.nextDouble() * 2 - 1;
            }
        }
        
        for (int i = 0; i < hiddenLayerSize; i++) {
            outputWeights[i][0] = random.nextDouble() * 2 - 1;
        }
        
        // Initialize biases
        hiddenBiases = new double[hiddenLayerSize];
        outputBiases = new double[1];
        
        for (int i = 0; i < hiddenLayerSize; i++) {
            hiddenBiases[i] = random.nextDouble() * 2 - 1;
        }
        outputBiases[0] = random.nextDouble() * 2 - 1;
    }

    private void train(List<double[]> features, List<String> labels, Map<String, Double> labelMap) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0;
            
            for (int i = 0; i < features.size(); i++) {
                double[] input = features.get(i);
                double target = labelMap.get(labels.get(i));
                
                // Forward pass
                double[] hiddenLayer = new double[hiddenLayerSize];
                for (int j = 0; j < hiddenLayerSize; j++) {
                    double sum = hiddenBiases[j];
                    for (int k = 0; k < input.length; k++) {
                        sum += input[k] * inputWeights[k][j];
                    }
                    hiddenLayer[j] = sigmoid(sum);
                }
                
                double output = outputBiases[0];
                for (int j = 0; j < hiddenLayerSize; j++) {
                    output += hiddenLayer[j] * outputWeights[j][0];
                }
                output = sigmoid(output);
                
                // Backward pass
                double outputError = (target - output) * output * (1 - output);
                double[] hiddenErrors = new double[hiddenLayerSize];
                
                for (int j = 0; j < hiddenLayerSize; j++) {
                    hiddenErrors[j] = outputError * outputWeights[j][0] * hiddenLayer[j] * (1 - hiddenLayer[j]);
                }
                
                // Update weights and biases
                for (int j = 0; j < hiddenLayerSize; j++) {
                    outputWeights[j][0] += learningRate * outputError * hiddenLayer[j];
                    for (int k = 0; k < input.length; k++) {
                        inputWeights[k][j] += learningRate * hiddenErrors[j] * input[k];
                    }
                    hiddenBiases[j] += learningRate * hiddenErrors[j];
                }
                outputBiases[0] += learningRate * outputError;
                
                totalError += Math.pow(target - output, 2);
            }
            
            if (epoch % 10 == 0) {
                System.out.println("Epoch " + epoch + ", Error: " + totalError);
            }
        }
    }

    private double predict(double[] input) {
        // Forward pass
        double[] hiddenLayer = new double[hiddenLayerSize];
        for (int j = 0; j < hiddenLayerSize; j++) {
            double sum = hiddenBiases[j];
            for (int k = 0; k < input.length; k++) {
                sum += input[k] * inputWeights[k][j];
            }
            hiddenLayer[j] = sigmoid(sum);
        }
        
        double output = outputBiases[0];
        for (int j = 0; j < hiddenLayerSize; j++) {
            output += hiddenLayer[j] * outputWeights[j][0];
        }
        return sigmoid(output);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
} 