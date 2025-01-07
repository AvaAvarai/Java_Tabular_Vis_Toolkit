package src.classifiers;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import src.CsvViewer;
import src.utils.DecisionTreeModel;
import src.utils.DecisionTreeModel.TreeNode;

public class RandomForestClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private double trainSplit = 0.7;
    private int kFold = 5;
    private boolean useKFold = false;
    private int numTrees = 10;
    private double sampleRatio = 0.7;
    private ArrayList<DecisionTreeModel> forest;

    public RandomForestClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
        this.forest = new ArrayList<>();
    }

    public void insertForestClassification() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        showConfigDialog();
    }

    private void showConfigDialog() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(csvViewer.getTable());
        JDialog dialog = new JDialog(parent, "Random Forest Configuration", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Forest Parameters
        JPanel forestPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        forestPanel.setBorder(BorderFactory.createTitledBorder("Forest Parameters"));
        
        SpinnerNumberModel treesModel = new SpinnerNumberModel(10, 1, 100, 1);
        JSpinner treesSpinner = new JSpinner(treesModel);
        forestPanel.add(new JLabel("Number of Trees:"));
        forestPanel.add(treesSpinner);
        
        SpinnerNumberModel sampleModel = new SpinnerNumberModel(0.7, 0.1, 1.0, 0.1);
        JSpinner sampleSpinner = new JSpinner(sampleModel);
        forestPanel.add(new JLabel("Sample Ratio:"));
        forestPanel.add(sampleSpinner);

        // Validation Parameters
        JPanel validationPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        validationPanel.setBorder(BorderFactory.createTitledBorder("Validation Method"));

        ButtonGroup validationGroup = new ButtonGroup();
        JRadioButton randomSplitButton = new JRadioButton("Random Split", true);
        JRadioButton kFoldButton = new JRadioButton("K-Fold Cross Validation", false);
        validationGroup.add(randomSplitButton);
        validationGroup.add(kFoldButton);

        SpinnerNumberModel splitModel = new SpinnerNumberModel(0.7, 0.1, 0.9, 0.1);
        JSpinner splitSpinner = new JSpinner(splitModel);
        
        SpinnerNumberModel foldModel = new SpinnerNumberModel(5, 2, 10, 1);
        JSpinner foldSpinner = new JSpinner(foldModel);
        foldSpinner.setEnabled(false);

        validationPanel.add(randomSplitButton);
        validationPanel.add(new JPanel());
        validationPanel.add(new JLabel("Train Split Ratio:"));
        validationPanel.add(splitSpinner);
        validationPanel.add(kFoldButton);
        validationPanel.add(new JPanel());
        validationPanel.add(new JLabel("Number of Folds:"));
        validationPanel.add(foldSpinner);

        randomSplitButton.addActionListener(e -> {
            splitSpinner.setEnabled(true);
            foldSpinner.setEnabled(false);
        });

        kFoldButton.addActionListener(e -> {
            splitSpinner.setEnabled(false);
            foldSpinner.setEnabled(true);
        });

        mainPanel.add(forestPanel);
        mainPanel.add(validationPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            numTrees = (Integer) treesSpinner.getValue();
            sampleRatio = (Double) sampleSpinner.getValue();
            trainSplit = (Double) splitSpinner.getValue();
            kFold = (Integer) foldSpinner.getValue();
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
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private void performRandomSplitValidation() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        ArrayList<String[]> allData = getAllData();
        ArrayList<String> attributeNames = getAttributeNames();
        
        // Shuffle data
        Collections.shuffle(allData, new Random(42));
        
        int trainSize = (int)(allData.size() * trainSplit);
        ArrayList<String[]> trainData = new ArrayList<>(allData.subList(0, trainSize));
        ArrayList<String[]> testData = new ArrayList<>(allData.subList(trainSize, allData.size()));

        // Train forest
        trainForest(trainData, attributeNames, classColumnIndex);

        // Create label mapping from training data
        Map<String, Double> normalizedMap = createNormalizedMap(trainData, classColumnIndex);

        // Evaluate on test set
        int correct = 0;
        for (String[] instance : testData) {
            String actualClass = instance[classColumnIndex];
            String predictedClass = predict(instance);
            if (predictedClass.equals(actualClass)) correct++;
        }
        double accuracy = (double) correct / testData.size() * 100;

        // Add prediction column
        addPredictionColumn(allData, normalizedMap);

        showResults(accuracy);
    }

    private void trainForest(ArrayList<String[]> trainData, ArrayList<String> attributeNames, int classColumnIndex) {
        forest.clear();
        Random random = new Random(42);
        
        for (int i = 0; i < numTrees; i++) {
            // Bootstrap sample
            ArrayList<String[]> sample = new ArrayList<>();
            int sampleSize = (int)(trainData.size() * sampleRatio);
            
            for (int j = 0; j < sampleSize; j++) {
                sample.add(trainData.get(random.nextInt(trainData.size())));
            }
            
            // Train tree on bootstrap sample
            DecisionTreeModel tree = new DecisionTreeModel(sample, attributeNames, classColumnIndex);
            forest.add(tree);
        }
    }

    private String predict(String[] instance) {
        Map<String, Integer> votes = new HashMap<>();
        
        // Collect votes from all trees
        for (DecisionTreeModel tree : forest) {
            String prediction = predictFromTree(tree.getRoot(), instance);
            votes.put(prediction, votes.getOrDefault(prediction, 0) + 1);
        }
        
        // Return majority vote
        return votes.entrySet().stream()
                   .max(Map.Entry.comparingByValue())
                   .get()
                   .getKey();
    }

    private String predictFromTree(TreeNode node, String[] instance) {
        if (node.isLeaf) {
            return node.prediction;
        }
        boolean answer = node.question.apply(instance);
        return predictFromTree(answer ? node.right : node.left, instance);
    }

    private Map<String, Double> createNormalizedMap(ArrayList<String[]> data, int classColumnIndex) {
        ArrayList<String> uniqueClasses = new ArrayList<>();
        for (String[] row : data) {
            String className = row[classColumnIndex];
            if (!uniqueClasses.contains(className)) {
                uniqueClasses.add(className);
            }
        }

        Map<String, Double> normalizedMap = new HashMap<>();
        int numClasses = uniqueClasses.size();
        for (int i = 0; i < numClasses; i++) {
            normalizedMap.put(uniqueClasses.get(i), i / (double)(numClasses - 1));
        }
        return normalizedMap;
    }

    private void addPredictionColumn(ArrayList<String[]> allData, Map<String, Double> normalizedMap) {
        String columnName = csvViewer.getUniqueColumnName("RF_prediction");
        tableModel.addColumn(columnName);
        int predictionColumnIndex = tableModel.getColumnCount() - 1;

        DecimalFormat df = new DecimalFormat("#.###");
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String predictedClass = predict(allData.get(row));
            double normalizedValue = normalizedMap.get(predictedClass);
            tableModel.setValueAt(df.format(normalizedValue), row, predictionColumnIndex);
        }
    }

    private void showResults(double accuracy) {
        JOptionPane.showMessageDialog(csvViewer,
            String.format("Random Forest Performance:\nTest Accuracy: %.2f%%\n" +
                        "Trees: %d, Sample Ratio: %.1f\n" +
                        "Train/Test Split: %.0f%%/%.0f%%",
                accuracy, numTrees, sampleRatio,
                trainSplit * 100, (1 - trainSplit) * 100),
            "Model Performance",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private ArrayList<String[]> getAllData() {
        ArrayList<String[]> allData = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String[] rowData = new String[tableModel.getColumnCount()];
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                rowData[col] = tableModel.getValueAt(row, col).toString();
            }
            allData.add(rowData);
        }
        return allData;
    }

    private ArrayList<String> getAttributeNames() {
        ArrayList<String> attributeNames = new ArrayList<>();
        for (int col = 0; col < tableModel.getColumnCount(); col++) {
            attributeNames.add(tableModel.getColumnName(col));
        }
        return attributeNames;
    }

    private void performKFoldValidation() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        ArrayList<String[]> allData = getAllData();
        ArrayList<String> attributeNames = getAttributeNames();
        
        // Shuffle data
        Collections.shuffle(allData, new Random(42));
        
        int foldSize = allData.size() / kFold;
        double totalAccuracy = 0;
        
        // Perform k-fold cross validation
        for (int i = 0; i < kFold; i++) {
            int startTest = i * foldSize;
            int endTest = (i == kFold - 1) ? allData.size() : (i + 1) * foldSize;
            
            ArrayList<String[]> testData = new ArrayList<>(allData.subList(startTest, endTest));
            ArrayList<String[]> trainData = new ArrayList<>();
            trainData.addAll(allData.subList(0, startTest));
            trainData.addAll(allData.subList(endTest, allData.size()));
            
            // Train forest on this fold
            trainForest(trainData, attributeNames, classColumnIndex);
            
            // Evaluate fold
            int correct = 0;
            for (String[] instance : testData) {
                String actualClass = instance[classColumnIndex];
                String predictedClass = predict(instance);
                if (predictedClass.equals(actualClass)) correct++;
            }
            
            totalAccuracy += (double) correct / testData.size();
        }

        // Train final model on all data
        trainForest(allData, attributeNames, classColumnIndex);
        
        // Create normalized mapping
        Map<String, Double> normalizedMap = createNormalizedMap(allData, classColumnIndex);
        
        // Add predictions column
        addPredictionColumn(allData, normalizedMap);
        
        // Show results
        double avgAccuracy = totalAccuracy / kFold * 100;
        JOptionPane.showMessageDialog(csvViewer,
            String.format("Random Forest Performance:\nAverage %d-Fold CV Accuracy: %.2f%%\n" +
                        "Trees: %d, Sample Ratio: %.1f",
                kFold, avgAccuracy, numTrees, sampleRatio),
            "Model Performance",
            JOptionPane.INFORMATION_MESSAGE);
    }   
} 