package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import src.CsvViewer;
import src.utils.DecisionTree;

import java.util.List;
import java.text.DecimalFormat;

public class DecisionTreeClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double trainSplit = 0.7;
    private int kFold = 5;
    private boolean useKFold = false;

    public DecisionTreeClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertTreeClassification() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        showConfigDialog();
    }

    private void showConfigDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(csvViewer.getTable()), 
                                   "Decision Tree Configuration");
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Validation method panel
        JPanel validationPanel = new JPanel(new GridBagLayout());
        validationPanel.setBorder(BorderFactory.createTitledBorder("Validation Method"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Radio buttons for validation method
        ButtonGroup validationGroup = new ButtonGroup();
        JRadioButton randomSplitButton = new JRadioButton("Random Split", true);
        JRadioButton kFoldButton = new JRadioButton("K-Fold Cross Validation", false);
        validationGroup.add(randomSplitButton);
        validationGroup.add(kFoldButton);

        // Split ratio spinner
        JSpinner splitSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.1, 0.9, 0.1));
        JLabel splitLabel = new JLabel("Train Split Ratio:");
        
        // K-fold spinner
        JSpinner kFoldSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 10, 1));
        JLabel kFoldLabel = new JLabel("Number of Folds:");
        kFoldSpinner.setEnabled(false);

        // Layout components
        gbc.gridx = 0; gbc.gridy = 0;
        validationPanel.add(randomSplitButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        validationPanel.add(splitLabel, gbc);
        gbc.gridx = 1;
        validationPanel.add(splitSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        validationPanel.add(kFoldButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        validationPanel.add(kFoldLabel, gbc);
        gbc.gridx = 1;
        validationPanel.add(kFoldSpinner, gbc);

        // Enable/disable components based on selection
        randomSplitButton.addActionListener(e -> {
            splitSpinner.setEnabled(true);
            kFoldSpinner.setEnabled(false);
        });

        kFoldButton.addActionListener(e -> {
            splitSpinner.setEnabled(false);
            kFoldSpinner.setEnabled(true);
        });

        mainPanel.add(validationPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            trainSplit = (Double) splitSpinner.getValue();
            kFold = (Integer) kFoldSpinner.getValue();
            useKFold = kFoldButton.isSelected();
            dialog.dispose();
            if (useKFold) {
                performKFoldValidation();
            } else {
                performRandomSplitValidation();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(csvViewer.getTable()));
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private void performRandomSplitValidation() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            JOptionPane.showMessageDialog(csvViewer, "No class column found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get all data and attribute names
        List<String[]> allData = getAllData();
        List<String> attributeNames = getAttributeNames();

        // Shuffle and split data according to trainSplit ratio
        Collections.shuffle(allData, new Random(42));
        int trainSize = (int)(allData.size() * trainSplit);
        List<String[]> trainData = allData.subList(0, trainSize);
        List<String[]> testData = allData.subList(trainSize, allData.size());

        // Build decision tree on training data
        DecisionTree dt = new DecisionTree(trainData, attributeNames, classColumnIndex);

        // Create label mapping from training data only
        Map<String, Integer> labelMap = new HashMap<>();
        for (String[] row : trainData) {
            String className = row[classColumnIndex];
            if (!labelMap.containsKey(className)) {
                labelMap.put(className, labelMap.size());
            }
        }

        // Evaluate on test set
        int correctTest = 0;
        for (String[] instance : testData) {
            String actualClass = instance[classColumnIndex];
            String predictedClass = predictClass(dt.getRoot(), instance);
            if (predictedClass.equals(actualClass)) correctTest++;
        }
        double testAccuracy = (double) correctTest / testData.size() * 100;

        // Add prediction column
        String columnName = csvViewer.getUniqueColumnName("DT_prediction");
        tableModel.addColumn(columnName);
        int predictionColumnIndex = tableModel.getColumnCount() - 1;

        // Create evenly distributed values from 0 to 1 inclusive
        Map<String, Double> normalizedMap = new HashMap<>();
        int numClasses = labelMap.size();
        for (Map.Entry<String, Integer> entry : labelMap.entrySet()) {
            normalizedMap.put(entry.getKey(), entry.getValue() / (double)(numClasses - 1));
        }

        // Make predictions for all rows using the trained model
        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String[] instance = allData.get(row);
            String predictedClass = predictClass(dt.getRoot(), instance);
            double normalizedValue = normalizedMap.get(predictedClass);
            tableModel.setValueAt(df.format(normalizedValue), row, predictionColumnIndex);
        }

        JOptionPane.showMessageDialog(csvViewer,
            String.format("Decision Tree Performance:\nTest Set Accuracy: %.2f%%\nTrain/Test Split: %.0f%%/%.0f%%",
                testAccuracy, trainSplit * 100, (1 - trainSplit) * 100),
            "Model Performance",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void performKFoldValidation() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        List<String[]> allData = getAllData();
        List<String> attributeNames = getAttributeNames();
        
        // Shuffle data
        Collections.shuffle(allData, new Random(42));
        
        int foldSize = allData.size() / kFold;
        double totalAccuracy = 0;
        
        Map<String, Integer> labelMap = new HashMap<>();
        
        // Perform k-fold cross validation
        for (int i = 0; i < kFold; i++) {
            int startTest = i * foldSize;
            int endTest = (i == kFold - 1) ? allData.size() : (i + 1) * foldSize;
            
            List<String[]> testData = allData.subList(startTest, endTest);
            List<String[]> trainData = new ArrayList<>();
            trainData.addAll(allData.subList(0, startTest));
            trainData.addAll(allData.subList(endTest, allData.size()));
            
            DecisionTree dt = new DecisionTree(trainData, attributeNames, classColumnIndex);
            
            // Update label map
            for (String[] row : trainData) {
                String className = row[classColumnIndex];
                if (!labelMap.containsKey(className)) {
                    labelMap.put(className, labelMap.size());
                }
            }
            
            // Evaluate fold
            int correct = 0;
            for (String[] instance : testData) {
                String actualClass = instance[classColumnIndex];
                String predictedClass = predictClass(dt.getRoot(), instance);
                if (predictedClass.equals(actualClass)) correct++;
            }
            
            totalAccuracy += (double) correct / testData.size();
        }
        
        // Train final model on all data and make predictions
        DecisionTree finalModel = new DecisionTree(allData, attributeNames, classColumnIndex);
        
        // Add prediction column
        String columnName = csvViewer.getUniqueColumnName("DT_prediction");
        tableModel.addColumn(columnName);
        int predictionColumnIndex = tableModel.getColumnCount() - 1;
        
        // Create evenly distributed values from 0 to 1 inclusive
        Map<String, Double> normalizedMap = new HashMap<>();
        int numClasses = labelMap.size();
        for (Map.Entry<String, Integer> entry : labelMap.entrySet()) {
            normalizedMap.put(entry.getKey(), entry.getValue() / (double)(numClasses - 1));
        }

        // Make predictions for all rows
        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String[] instance = allData.get(row);
            String predictedClass = predictClass(finalModel.getRoot(), instance);
            double normalizedValue = normalizedMap.get(predictedClass);
            tableModel.setValueAt(df.format(normalizedValue), row, predictionColumnIndex);
        }
        
        double avgAccuracy = totalAccuracy / kFold * 100;
        JOptionPane.showMessageDialog(csvViewer,
            String.format("Decision Tree Performance:\nAverage %d-Fold CV Accuracy: %.2f%%",
                kFold, avgAccuracy),
            "Model Performance",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private List<String[]> getAllData() {
        List<String[]> allData = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String[] rowData = new String[tableModel.getColumnCount()];
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                rowData[col] = tableModel.getValueAt(row, col).toString();
            }
            allData.add(rowData);
        }
        return allData;
    }

    private List<String> getAttributeNames() {
        List<String> attributeNames = new ArrayList<>();
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            attributeNames.add(tableModel.getColumnName(col));
        }
        return attributeNames;
    }

    private String predictClass(DecisionTree.TreeNode node, String[] instance) {
        if (node.isLeaf) {
            return node.prediction;
        }
        boolean answer = node.question.apply(instance);
        return predictClass(answer ? node.right : node.left, instance);
    }
}