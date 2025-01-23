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
    private long seed = 42;
    private double[][] inputWeights;
    private double[][] outputWeights;
    private double[] hiddenBiases;
    private double[] outputBiases;
    private Random random;
    private List<Integer> selectedFeatures;
    
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
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), "MLP Configuration");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Parameter inputs panel
        JPanel paramsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        
        // Learning rate input
        paramsPanel.add(new JLabel("Learning Rate:"));
        JTextField learningRateField = new JTextField(String.valueOf(learningRate));
        paramsPanel.add(learningRateField);

        // Epochs input
        paramsPanel.add(new JLabel("Epochs:"));
        JTextField epochsField = new JTextField(String.valueOf(epochs));
        paramsPanel.add(epochsField);

        // Hidden layer size input
        paramsPanel.add(new JLabel("Hidden Layer Size:"));
        JTextField hiddenLayerField = new JTextField(String.valueOf(hiddenLayerSize));
        paramsPanel.add(hiddenLayerField);

        // Random seed input
        paramsPanel.add(new JLabel("Random Seed:"));
        JTextField seedField = new JTextField(String.valueOf(seed));
        paramsPanel.add(seedField);

        mainPanel.add(paramsPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Feature selection panel
        JPanel featurePanel = new JPanel();
        featurePanel.setLayout(new BoxLayout(featurePanel, BoxLayout.Y_AXIS));
        featurePanel.setBorder(BorderFactory.createTitledBorder("Select Features"));

        List<JCheckBox> featureCheckboxes = new ArrayList<>();
        int classColumnIndex = csvViewer.getClassColumnIndex();
        
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (i != classColumnIndex) {
                JCheckBox checkBox = new JCheckBox(tableModel.getColumnName(i), true);
                featureCheckboxes.add(checkBox);
                featurePanel.add(checkBox);
            }
        }

        JScrollPane scrollPane = new JScrollPane(featurePanel);
        scrollPane.setPreferredSize(new Dimension(300, 150));
        mainPanel.add(scrollPane);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            try {
                learningRate = Double.parseDouble(learningRateField.getText());
                epochs = Integer.parseInt(epochsField.getText());
                hiddenLayerSize = Integer.parseInt(hiddenLayerField.getText());
                seed = Long.parseLong(seedField.getText());
                
                // Get selected features
                selectedFeatures = new ArrayList<>();
                int featureIndex = 0;
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (i != classColumnIndex) {
                        if (featureCheckboxes.get(featureIndex).isSelected()) {
                            selectedFeatures.add(i);
                        }
                        featureIndex++;
                    }
                }

                if (selectedFeatures.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Please select at least one feature.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                dialog.dispose();
                trainAndPredict();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter valid numeric values.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(csvViewer.getTable());
        dialog.setVisible(true);
    }

    private void trainAndPredict() {
        random = new Random(seed);
        int classColumnIndex = csvViewer.getClassColumnIndex();
        
        // Prepare data
        List<double[]> inputs = new ArrayList<>();
        List<Double> targets = new ArrayList<>();
        Map<String, Double> classMap = new HashMap<>();
        List<String> uniqueClasses = new ArrayList<>();

        // Get unique classes and create normalized mapping
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = tableModel.getValueAt(row, classColumnIndex).toString();
            if (!uniqueClasses.contains(className)) {
                uniqueClasses.add(className);
            }
        }
        
        // Create normalized class mapping
        for (int i = 0; i < uniqueClasses.size(); i++) {
            classMap.put(uniqueClasses.get(i), i / (double)(uniqueClasses.size() - 1));
        }

        // Collect and normalize input data
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] input = new double[selectedFeatures.size()];
            for (int i = 0; i < selectedFeatures.size(); i++) {
                input[i] = Double.parseDouble(tableModel.getValueAt(row, selectedFeatures.get(i)).toString());
            }
            inputs.add(input);
            targets.add(classMap.get(tableModel.getValueAt(row, classColumnIndex).toString()));
        }

        // Initialize network
        initializeNetwork(selectedFeatures.size());

        // Train network
        train(inputs, targets);

        // Add predictions column
        String columnName = csvViewer.getUniqueColumnName("MLP_Prediction");
        tableModel.addColumn(columnName);
        int predictionColumnIndex = tableModel.getColumnCount() - 1;

        // Make predictions
        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] input = inputs.get(row);
            double prediction = predict(input);
            tableModel.setValueAt(df.format(prediction), row, predictionColumnIndex);
        }
    }

    private void initializeNetwork(int inputSize) {
        inputWeights = new double[inputSize][hiddenLayerSize];
        outputWeights = new double[hiddenLayerSize][1];
        hiddenBiases = new double[hiddenLayerSize];
        outputBiases = new double[1];

        // Initialize weights with Xavier/Glorot initialization
        double inputScale = Math.sqrt(2.0 / (inputSize + hiddenLayerSize));
        double outputScale = Math.sqrt(2.0 / (hiddenLayerSize + 1));

        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenLayerSize; j++) {
                inputWeights[i][j] = (random.nextDouble() * 2 - 1) * inputScale;
            }
        }

        for (int i = 0; i < hiddenLayerSize; i++) {
            outputWeights[i][0] = (random.nextDouble() * 2 - 1) * outputScale;
            hiddenBiases[i] = 0;
        }
        outputBiases[0] = 0;
    }

    private void train(List<double[]> inputs, List<Double> targets) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0;
            
            for (int i = 0; i < inputs.size(); i++) {
                double[] input = inputs.get(i);
                double target = targets.get(i);
                
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