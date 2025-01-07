package src.classifiers;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import javax.swing.table.DefaultTableModel;
import src.CsvViewer;
import java.text.DecimalFormat;

public class KNearestNeighborsClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;

    public KNearestNeighborsClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertKNNClassification(int k, String metric) {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        // Extract features and labels
        List<double[]> features = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Map<String, Double> labelMap = new HashMap<>();
        List<String> uniqueLabels = new ArrayList<>();
        
        int classColumnIndex = csvViewer.getClassColumnIndex();

        // First pass - collect unique labels in order
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String label = tableModel.getValueAt(row, classColumnIndex).toString();
            if (!uniqueLabels.contains(label)) {
                uniqueLabels.add(label);
            }
        }

        // Create evenly distributed values from 0 to 1 inclusive
        int numLabels = uniqueLabels.size();
        for (int i = 0; i < numLabels; i++) {
            labelMap.put(uniqueLabels.get(i), i / (double)(numLabels - 1));
        }

        // Second pass - collect features and mapped labels
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            double[] featureRow = new double[tableModel.getColumnCount() - 1];
            int featureIndex = 0;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                if (col != classColumnIndex) {
                    featureRow[featureIndex++] = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                }
            }
            features.add(featureRow);
            labels.add(tableModel.getValueAt(row, classColumnIndex).toString());
        }

        // Add new column for k-NN numerical classification
        String newColumnName = csvViewer.getUniqueColumnName("kNN_" + k + "_" + metric);
        tableModel.addColumn(newColumnName);
        int newColumnIndex = tableModel.getColumnCount() - 1;

        DecimalFormat df = new DecimalFormat("#.####");
        
        // Classify each instance
        for (int i = 0; i < features.size(); i++) {
            double[] query = features.get(i);
            String prediction = classifyKNN(query, features, labels, k, metric);
            tableModel.setValueAt(df.format(labelMap.get(prediction)), i, newColumnIndex);
        }
    }

    private String classifyKNN(double[] query, List<double[]> features, List<String> labels, int k, String metric) {
        List<double[]> distances = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            double distance = calculateDistance(query, features.get(i), metric);
            distances.add(new double[]{distance, i});
        }

        distances.sort(Comparator.comparingDouble(a -> a[0]));

        Map<String, Integer> voteCount = new HashMap<>();
        for (int i = 0; i < k; i++) {
            int index = (int) distances.get(i)[1];
            String label = labels.get(index);
            voteCount.put(label, voteCount.getOrDefault(label, 0) + 1);
        }

        return voteCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    private double calculateDistance(double[] a, double[] b, String metric) {
        switch (metric.toLowerCase()) {
            case "manhattan":
                return manhattanDistance(a, b);
            case "euclidean":
            default:
                return euclideanDistance(a, b);
        }
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    private double manhattanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }
}