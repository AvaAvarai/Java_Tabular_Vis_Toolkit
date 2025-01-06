package src.classifiers;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import javax.swing.table.DefaultTableModel;
import src.CsvViewer;

public class KNearestNeighbors {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;

    public KNearestNeighbors(CsvViewer csvViewer, DefaultTableModel tableModel) {
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
        int classColumnIndex = csvViewer.getClassColumnIndex();

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

        // Add a new column for k-NN classification
        String newColumnName = csvViewer.getUniqueColumnName("kNN Classification (k=" + k + ", metric=" + metric + ")");
        tableModel.addColumn(newColumnName);
        int newColumnIndex = tableModel.getColumnCount() - 1;

        // Classify each instance
        for (int i = 0; i < features.size(); i++) {
            double[] query = features.get(i);
            String prediction = classifyKNN(query, features, labels, k, metric);
            tableModel.setValueAt(prediction, i, newColumnIndex);
        }

        csvViewer.getStateManager().addClassColumn(newColumnIndex);
    }

    private String classifyKNN(double[] query, List<double[]> features, List<String> labels, int k, String metric) {
        List<double[]> distances = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            double distance = calculateDistance(query, features.get(i), metric);
            distances.add(new double[]{distance, i});
        }

        // Sort by distance
        distances.sort(Comparator.comparingDouble(a -> a[0]));

        // Take the top-k neighbors
        Map<String, Integer> voteCount = new HashMap<>();
        for (int i = 0; i < k; i++) {
            int index = (int) distances.get(i)[1];
            String label = labels.get(index);
            voteCount.put(label, voteCount.getOrDefault(label, 0) + 1);
        }

        // Find the majority vote
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