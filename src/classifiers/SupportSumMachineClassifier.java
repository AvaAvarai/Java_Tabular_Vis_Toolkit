package src.classifiers;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import src.utils.GradientDescentOptimizer;
import src.CsvViewer;

public class SupportSumMachineClassifier {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;

    public SupportSumMachineClassifier(CsvViewer csvViewer, DefaultTableModel tableModel) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
    }

    public void insertWeightedSumColumn() {
        if (tableModel.getColumnCount() == 0) {
            csvViewer.noDataLoadedError();
            return;
        }

        List<Integer> columnIndices = new ArrayList<>();
        List<Double> coefficients = new ArrayList<>();

        JPanel panel = new JPanel(new GridLayout(0, 2));

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            if (!columnName.equalsIgnoreCase("class")) {
                columnIndices.add(i);
                JLabel label = new JLabel("Coefficient for " + columnName + ":");
                JTextField coefficientField = new JTextField("1");
                panel.add(label);
                panel.add(coefficientField);
                coefficients.add(null);
            }
        }

        String[] initTypes = {"Flat Value", "Random Range"};
        JComboBox<String> initTypeCombo = new JComboBox<>(initTypes);
        panel.add(new JLabel("Initialization Type:"));
        panel.add(initTypeCombo);

        JLabel flatValueLabel = new JLabel("Flat Value:");
        JTextField flatValueField = new JTextField("1.0");
        panel.add(flatValueLabel);
        panel.add(flatValueField);

        JLabel minRangeLabel = new JLabel("Min Range:");
        JTextField minRangeField = new JTextField("0.0");
        JLabel maxRangeLabel = new JLabel("Max Range:");
        JTextField maxRangeField = new JTextField("1.0");

        panel.add(minRangeLabel);
        panel.add(minRangeField);
        panel.add(maxRangeLabel);
        panel.add(maxRangeField);

        JLabel coeffMinLabel = new JLabel("Coefficient Min:");
        JTextField coeffMinField = new JTextField("-1.0");
        JLabel coeffMaxLabel = new JLabel("Coefficient Max:");
        JTextField coeffMaxField = new JTextField("1.0");

        panel.add(coeffMinLabel);
        panel.add(coeffMinField);
        panel.add(coeffMaxLabel);
        panel.add(coeffMaxField);

        JCheckBox adaptiveLearningRateCheckbox = new JCheckBox("Use Adaptive Learning Rate", true);
        panel.add(new JLabel(""));
        panel.add(adaptiveLearningRateCheckbox);

        initTypeCombo.addActionListener(e -> {
            boolean isRandom = initTypeCombo.getSelectedItem().equals("Random Range");
            minRangeField.setEnabled(isRandom);
            maxRangeField.setEnabled(isRandom);
            flatValueField.setEnabled(!isRandom);
        });
        
        minRangeField.setEnabled(false);
        maxRangeField.setEnabled(false);

        String[] trigOptions = {"None", "cos", "sin", "tan", "arccos", "arcsin", "arctan"};
        JComboBox<String> trigFunctionSelector = new JComboBox<>(trigOptions);
        panel.add(new JLabel("Wrap Weighted Sum in:"));
        panel.add(trigFunctionSelector);

        JButton optimizeButton = new JButton("Optimize Coefficients");
        optimizeButton.addActionListener(e -> {
            try {
                String initType = initTypeCombo.getSelectedItem().toString();
                double flatValue = Double.parseDouble(flatValueField.getText());
                double minRange = Double.parseDouble(minRangeField.getText());
                double maxRange = Double.parseDouble(maxRangeField.getText());
                double coeffMin = Double.parseDouble(coeffMinField.getText());
                double coeffMax = Double.parseDouble(coeffMaxField.getText());
                
                if (initType.equals("Random Range") && minRange >= maxRange) {
                    JOptionPane.showMessageDialog(csvViewer, 
                        "Min range must be less than max range", 
                        "Invalid Range", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (coeffMin >= coeffMax) {
                    JOptionPane.showMessageDialog(csvViewer,
                        "Coefficient minimum must be less than maximum",
                        "Invalid Coefficient Range",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                GradientDescentOptimizer optimizer = new GradientDescentOptimizer(csvViewer, 0.01, 1000, 1e-6, adaptiveLearningRateCheckbox.isSelected());
                optimizer.optimizeCoefficientsUsingGradientDescent(
                    columnIndices, 
                    coefficients, 
                    panel, 
                    (String) trigFunctionSelector.getSelectedItem(),
                    initType.equals("Random Range") ? "random" : "flat",
                    flatValue,
                    minRange,
                    maxRange,
                    coeffMin,
                    coeffMax
                );
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(csvViewer, 
                    "Please enter valid numbers for all fields", 
                    "Invalid Input", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(optimizeButton);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(400, 400));

        int result = JOptionPane.showConfirmDialog(csvViewer, scrollPane, 
            "Enter or optimize coefficients for weighted sum", 
            JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                for (int j = 0; j < coefficients.size(); j++) {
                    coefficients.set(j, Double.parseDouble(((JTextField) panel.getComponent(2 * j + 1)).getText()));
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(csvViewer, "Please enter valid numbers for coefficients.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DecimalFormat decimalFormat = new DecimalFormat("#.####");
            StringBuilder columnNameBuilder = new StringBuilder();
            for (int i = 0; i < columnIndices.size(); i++) {
                double coeff = coefficients.get(i);
                if (i == 0 || coeff >= 0) {
                    if (i > 0) {
                        columnNameBuilder.append("+");
                    }
                    columnNameBuilder.append(decimalFormat.format(coeff));
                } else {
                    columnNameBuilder.append(decimalFormat.format(coeff));
                }
                columnNameBuilder.append("*").append(tableModel.getColumnName(columnIndices.get(i)));
            }
            String columnName = columnNameBuilder.toString();

            String newColumnName = csvViewer.getUniqueColumnName(columnName);
            tableModel.addColumn(newColumnName);

            String trigFunction = (String) trigFunctionSelector.getSelectedItem();

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                double sum = 0.0;
                try {
                    for (int j = 0; j < columnIndices.size(); j++) {
                        Object value = tableModel.getValueAt(row, columnIndices.get(j));
                        sum += coefficients.get(j) * Double.parseDouble(value.toString());
                    }
                    sum = applyTrigFunction(sum, trigFunction);
                    if (sum < min) {
                        min = sum;
                    }
                    if (sum > max) {
                        max = sum;
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    sum = Double.NaN;
                }
            }

            if (min != max) {
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    double sum = 0.0;
                    try {
                        for (int j = 0; j < columnIndices.size(); j++) {
                            Object value = tableModel.getValueAt(row, columnIndices.get(j));
                            sum += coefficients.get(j) * Double.parseDouble(value.toString());
                        }
                        sum = applyTrigFunction(sum, trigFunction);
                        if (csvViewer.getStateManager().isNormalized()) {
                            sum = (sum - min) / (max - min);
                        }
                        tableModel.setValueAt(decimalFormat.format(sum), row, tableModel.getColumnCount() - 1);
                    } catch (NumberFormatException | NullPointerException e) {
                        tableModel.setValueAt("NaN", row, tableModel.getColumnCount() - 1);
                    }
                }
            } else {
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    double sum = 0.0;
                    try {
                        for (int j = 0; j < columnIndices.size(); j++) {
                            Object value = tableModel.getValueAt(row, columnIndices.get(j));
                            sum += coefficients.get(j) * Double.parseDouble(value.toString());
                        }
                        sum = applyTrigFunction(sum, trigFunction);
                        tableModel.setValueAt(decimalFormat.format(sum), row, tableModel.getColumnCount() - 1);
                    } catch (NumberFormatException | NullPointerException e) {
                        tableModel.setValueAt("NaN", row, tableModel.getColumnCount() - 1);
                    }
                }
            }

            csvViewer.applyRowFilter();
            csvViewer.getDataHandler().updateStats(tableModel, csvViewer.getStatsTextArea());
            csvViewer.updateSelectedRowsLabel();
        }
    }

    private double applyTrigFunction(double value, String trigFunction) {
        switch (trigFunction) {
            case "cos": return Math.cos(value);
            case "sin": return Math.sin(value);
            case "tan": return Math.tan(value);
            case "arccos": return Math.acos(value);
            case "arcsin": return Math.asin(value);
            case "arctan": return Math.atan(value);
            case "None":
            default: return value;
        }
    }
}