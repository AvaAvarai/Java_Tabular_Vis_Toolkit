package src.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DecisionTreeModel {

    private static final String RIGHT_CHILD = "RIGHT_CHILD";
    private static final String LEFT_CHILD = "LEFT_CHILD";

    public static class TreeNode {
        public Function<String[], Boolean> question;
        public String questionText;
        public boolean isLeaf;
        public TreeNode left;
        public TreeNode right;
        public String prediction;
        public int caseCount;
    }    

    private TreeNode root;
    private List<String> attributeNames;

    public DecisionTreeModel(List<String[]> data, List<String> attributeNames, int labelColumnIndex) {
        this.attributeNames = attributeNames;
        this.root = buildTree(data, labelColumnIndex);
    }

    private TreeNode buildTree(List<String[]> data, int labelColumnIndex) {
        if (isPure(data, labelColumnIndex)) {
            TreeNode leaf = new TreeNode();
            leaf.isLeaf = true;
            leaf.prediction = data.get(0)[labelColumnIndex];
            leaf.caseCount = data.size(); // Track the number of cases
            return leaf;
        }

        double bestGain = 0.0;
        int bestIndex = -1;
        double bestValue = Double.MIN_VALUE;

        for (int i = 0; i < data.get(0).length; i++) {
            if (i == labelColumnIndex) continue;

            Set<Double> uniqueValues = getUniqueValues(data, i);
            for (Double value : uniqueValues) {
                double gain = informationGain(data, labelColumnIndex, i, value);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestIndex = i;
                    bestValue = value;
                }
            }
        }

        if (bestGain == 0) {
            TreeNode leaf = new TreeNode();
            leaf.isLeaf = true;
            leaf.prediction = mostCommonLabel(data, labelColumnIndex);
            leaf.caseCount = data.size(); // Track the number of cases
            return leaf;
        }

        final int bestIndexFinal = bestIndex;
        final double bestValueFinal = bestValue;
        Function<String[], Boolean> question = dataRow -> {
            try {
                double value = Double.parseDouble(dataRow[bestIndexFinal]);
                return value <= bestValueFinal;
            } catch (NumberFormatException e) {
                return false; // Handle the case where dataRow[bestIndex] is not a number
            }
        };
        
        Map<String, List<String[]>> partitions = partitionByQuestion(question, data);

        TreeNode node = new TreeNode();
        node.question = question;
        node.questionText = "Is " + attributeNames.get(bestIndex) + " <= " + bestValue + "?";
        node.left = buildTree(partitions.get(LEFT_CHILD), labelColumnIndex);
        node.right = buildTree(partitions.get(RIGHT_CHILD), labelColumnIndex);

        return node;
    }

    private boolean isPure(List<String[]> data, int labelColumnIndex) {
        String firstLabel = data.get(0)[labelColumnIndex];
        for (String[] row : data) {
            if (!row[labelColumnIndex].equals(firstLabel)) {
                return false;
            }
        }
        return true;
    }

    private Set<Double> getUniqueValues(List<String[]> data, int index) {
        Set<Double> uniqueValues = new HashSet<>();
        for (String[] row : data) {
            try {
                uniqueValues.add(Double.parseDouble(row[index]));
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        return uniqueValues;
    }

    private String mostCommonLabel(List<String[]> data, int labelColumnIndex) {
        Map<String, Integer> labelCounts = new HashMap<>();
        for (String[] row : data) {
            String label = row[labelColumnIndex];
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }
        return labelCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    private double informationGain(List<String[]> data, int labelColumnIndex, int featureIndex, double value) {
        double currentImpurity = gini(data, labelColumnIndex);
        Map<String, List<String[]>> partitions = partitionByQuestion(dataRow -> {
            try {
                return Double.parseDouble(dataRow[featureIndex]) <= value;
            } catch (NumberFormatException e) {
                return false; // Handle non-numeric values gracefully
            }
        }, data);

        double leftProbability = (double) partitions.get(LEFT_CHILD).size() / data.size();
        double rightProbability = 1 - leftProbability;

        double gain = currentImpurity;
        gain -= leftProbability * gini(partitions.get(LEFT_CHILD), labelColumnIndex);
        gain -= rightProbability * gini(partitions.get(RIGHT_CHILD), labelColumnIndex);
        return gain;
    }

    private double gini(List<String[]> data, int labelColumnIndex) {
        Map<String, Integer> labelCounts = new HashMap<>();
        for (String[] row : data) {
            String label = row[labelColumnIndex];
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }

        double impurity = 1.0;
        for (int count : labelCounts.values()) {
            double prob = (double) count / data.size();
            impurity -= prob * prob;
        }
        return impurity;
    }

    private Map<String, List<String[]>> partitionByQuestion(Function<String[], Boolean> question, List<String[]> data) {
        Map<String, List<String[]>> partitions = new HashMap<>();
        partitions.put(RIGHT_CHILD, new ArrayList<>());
        partitions.put(LEFT_CHILD, new ArrayList<>());

        for (String[] row : data) {
            if (question.apply(row)) {
                partitions.get(RIGHT_CHILD).add(row);
            } else {
                partitions.get(LEFT_CHILD).add(row);
            }
        }
        return partitions;
    }

    public void printDecisionTree() {
        printTreeNode(root, 0);
    }

    private void printTreeNode(TreeNode node, int level) {
        if (node.isLeaf) {
            System.out.println("  ".repeat(level) + "Predict: " + node.prediction + " (" + node.caseCount + " cases)");
        } else {
            System.out.println("  ".repeat(level) + "Q: " + node.questionText);
            printTreeNode(node.left, level + 1);
            printTreeNode(node.right, level + 1);
        }
    }

    public TreeNode getRoot() {
        return root;
    }     
}
