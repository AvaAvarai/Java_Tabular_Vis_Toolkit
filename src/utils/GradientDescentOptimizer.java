package src.utils;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.JTextField;
import src.CsvViewer;
/**
 * The GradientDescentOptimizer class provides functionality to optimize the coefficients
 * for a weighted sum of features using the gradient descent algorithm with optional adaptive learning rate.
 * This optimization aims to maximize the separability between classes in a dataset.
 */
public class GradientDescentOptimizer {

    private final CsvViewer csvViewer;
    private double learningRate;
    private final int maxIterations;
    private final double tolerance;
    private final Random random = new Random();
    private final double learningRateDecay = 0.95; // Learning rate decay factor
    private final double learningRateIncrease = 1.05; // Learning rate increase factor
    private final double minLearningRate = 0.0001; // Minimum learning rate
    private final double maxLearningRate = 1.0; // Maximum learning rate
    private final boolean useAdaptiveLearningRate;

    /**
     * Constructs a GradientDescentOptimizer with the specified parameters.
     *
     * @param csvViewer the CsvViewer instance that manages the data and UI.
     * @param learningRate the initial learning rate for the gradient descent algorithm.
     * @param maxIterations the maximum number of iterations for the optimization process.
     * @param tolerance the tolerance for convergence in the optimization process.
     * @param useAdaptiveLearningRate whether to use adaptive learning rate during optimization.
     */
    public GradientDescentOptimizer(CsvViewer csvViewer, double learningRate, int maxIterations, double tolerance, boolean useAdaptiveLearningRate) {
        this.csvViewer = csvViewer;
        this.learningRate = learningRate;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
        this.useAdaptiveLearningRate = useAdaptiveLearningRate;
    }

    /**
     * Optimizes the coefficients for the weighted sum using gradient descent with optional adaptive learning rate.
     * The optimized coefficients are then updated in the provided JPanel.
     *
     * @param originalColumnIndices the list of indices corresponding to the original columns in the dataset.
     * @param coefficients the list of coefficients to be optimized.
     * @param panel the JPanel containing the UI components for coefficient inputs.
     * @param trigFunction the trigonometric function to apply to the weighted sum.
     */
    public void optimizeCoefficientsUsingGradientDescent(List<Integer> originalColumnIndices, List<Double> coefficients, JPanel panel, String trigFunction, String initializationType, double flatValue, double minRange, double maxRange, double coeffMin, double coeffMax) {
        initializeCoefficients(coefficients, initializationType, flatValue, minRange, maxRange);

        int n = coefficients.size();
        double[] gradients = new double[n];
        double previousScore = Double.NEGATIVE_INFINITY;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double currentScore = evaluateClassSeparation(originalColumnIndices, coefficients.stream().mapToDouble(Double::doubleValue).toArray(), trigFunction);

            if (useAdaptiveLearningRate) {
                // Adapt learning rate based on score improvement
                if (currentScore > previousScore) {
                    // If score improved, increase learning rate
                    learningRate = Math.min(maxLearningRate, learningRate * learningRateIncrease);
                } else {
                    // If score didn't improve, decrease learning rate
                    learningRate = Math.max(minLearningRate, learningRate * learningRateDecay);
                }
            }
            previousScore = currentScore;

            for (int i = 0; i < n; i++) {
                coefficients.set(i, coefficients.get(i) + tolerance);
                double newScore = evaluateClassSeparation(originalColumnIndices, coefficients.stream().mapToDouble(Double::doubleValue).toArray(), trigFunction);
                gradients[i] = (newScore - currentScore) / tolerance;
                coefficients.set(i, coefficients.get(i) - tolerance);
            }

            boolean hasConverged = true;
            for (int i = 0; i < n; i++) {
                double newCoefficient = coefficients.get(i) + learningRate * gradients[i];
                newCoefficient = Math.max(coeffMin, Math.min(coeffMax, newCoefficient));
                
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

    /**
     * Initializes coefficients with either a flat value or random values within a range.
     *
     * @param coefficients the list of coefficients to initialize
     * @param initializationType "flat" or "random"
     * @param flatValue the value to use for flat initialization
     * @param minRange minimum value for random initialization
     * @param maxRange maximum value for random initialization
     */
    private void initializeCoefficients(List<Double> coefficients, String initializationType, 
            double flatValue, double minRange, double maxRange) {
        for (int i = 0; i < coefficients.size(); i++) {
            if (coefficients.get(i) == null) {
                switch (initializationType) {
                    case "random":
                        double randomValue = minRange + (maxRange - minRange) * random.nextDouble();
                        coefficients.set(i, randomValue);
                        break;
                    case "flat":
                    default:
                        coefficients.set(i, flatValue);
                        break;
                }
            }
        }
    }

    /**
     * Evaluates the class separability using the specified coefficients and trigonometric function.
     * The separability is measured as the ratio of between-class variance to within-class variance.
     *
     * @param originalColumnIndices the list of indices corresponding to the original columns in the dataset.
     * @param coefficients the array of coefficients for the weighted sum.
     * @param trigFunction the trigonometric function to apply to the weighted sum.
     * @return the class separability score.
     */
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

    /**
     * Applies the specified trigonometric function to a given value.
     *
     * @param value the value to which the trigonometric function is applied.
     * @param trigFunction the trigonometric function to apply.
     * @return the result of applying the trigonometric function to the value.
     */
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

    /**
     * Updates the text fields in the provided JPanel with the optimized coefficients.
     *
     * @param coefficients the list of optimized coefficients.
     * @param panel the JPanel containing the text fields to update.
     */
    private void updatePanelFields(List<Double> coefficients, JPanel panel) {
        for (int i = 0; i < coefficients.size(); i++) {
            JTextField coefficientField = (JTextField) panel.getComponent(2 * i + 1);
            coefficientField.setText(coefficients.get(i).toString());
        }
    }
}
