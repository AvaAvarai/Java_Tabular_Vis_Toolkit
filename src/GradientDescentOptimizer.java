package src;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GradientDescentOptimizer {

    private final CsvViewer csvViewer;
    private final double learningRate;
    private final int maxIterations;
    private final double tolerance;

    public GradientDescentOptimizer(CsvViewer csvViewer, double learningRate, int maxIterations, double tolerance) {
        this.csvViewer = csvViewer;
        this.learningRate = learningRate;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }

    public void optimizeCoefficientsUsingGradientDescent(List<Integer> originalColumnIndices, List<Double> coefficients, JPanel panel, String trigFunction) {
        initializeCoefficients(coefficients);

        int n = coefficients.size();
        double[] gradients = new double[n];

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double currentScore = evaluateClassSeparation(originalColumnIndices, coefficients.stream().mapToDouble(Double::doubleValue).toArray(), trigFunction);

            for (int i = 0; i < n; i++) {
                coefficients.set(i, coefficients.get(i) + tolerance);
                double newScore = evaluateClassSeparation(originalColumnIndices, coefficients.stream().mapToDouble(Double::doubleValue).toArray(), trigFunction);
                gradients[i] = (newScore - currentScore) / tolerance;
                coefficients.set(i, coefficients.get(i) - tolerance);
            }

            boolean hasConverged = true;
            for (int i = 0; i < n; i++) {
                double newCoefficient = coefficients.get(i) + learningRate * gradients[i];
                if (Math.abs(newCoefficient - coefficients.get(i)) > tolerance) {
                    hasConverged = false;
                }
                coefficients.set(i, newCoefficient);
            }

            if (hasConverged) {
                break;
            }
        }

        updatePanelFields(coefficients, panel);
    }

    private void initializeCoefficients(List<Double> coefficients) {
        for (int i = 0; i < coefficients.size(); i++) {
            if (coefficients.get(i) == null) {
                coefficients.set(i, 1.0);
            }
        }
    }

    private double evaluateClassSeparation(List<Integer> originalColumnIndices, double[] coefficients, String trigFunction) {
        Map<String, List<Double>> classSums = new HashMap<>();
        int classColumnIndex = csvViewer.getClassColumnIndex();

        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            double sum = 0.0;
            for (int j = 0; j < originalColumnIndices.size(); j++) {
                double value = Double.parseDouble(csvViewer.tableModel.getValueAt(row, originalColumnIndices.get(j)).toString());
                sum += coefficients[j] * value;
            }
            sum = applyTrigFunction(sum, trigFunction);
            String className = (String) csvViewer.tableModel.getValueAt(row, classColumnIndex);
            classSums.computeIfAbsent(className, k -> new ArrayList<>()).add(sum);
        }

        double overallMean = classSums.values().stream()
                .flatMapToDouble(classList -> classList.stream().mapToDouble(Double::doubleValue))
                .average().orElse(0.0);

        double betweenClassVariance = 0.0;
        double withinClassVariance = 0.0;
        int totalSampleCount = csvViewer.tableModel.getRowCount();

        for (Map.Entry<String, List<Double>> entry : classSums.entrySet()) {
            List<Double> classValues = entry.getValue();
            int classSampleCount = classValues.size();
            double classMean = classValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            betweenClassVariance += classSampleCount * Math.pow(classMean - overallMean, 2);

            double classVariance = classValues.stream().mapToDouble(v -> Math.pow(v - classMean, 2)).sum();
            withinClassVariance += classVariance;
        }

        betweenClassVariance /= totalSampleCount;
        withinClassVariance /= totalSampleCount;

        if (withinClassVariance == 0) {
            return Double.MAX_VALUE;
        }

        return betweenClassVariance / withinClassVariance;
    }

    private double applyTrigFunction(double value, String trigFunction) {
        switch (trigFunction) {
            case "cos":
                return Math.cos(value);
            case "sin":
                return Math.sin(value);
            case "tan":
                return Math.tan(value);
            case "arccos":
                return Math.acos(value);
            case "arcsin":
                return Math.asin(value);
            case "arctan":
                return Math.atan(value);
            case "None":
            default:
                return value;
        }
    }

    private void updatePanelFields(List<Double> coefficients, JPanel panel) {
        for (int i = 0; i < coefficients.size(); i++) {
            JTextField coefficientField = (JTextField) panel.getComponent(2 * i + 1);
            coefficientField.setText(coefficients.get(i).toString());
        }
    }
}
