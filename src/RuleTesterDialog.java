package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class RuleTesterDialog extends JDialog {

    private final DefaultTableModel tableModel;
    private final JPanel rulesPanel;
    private final List<RulePanel> rulePanels = new ArrayList<>();
    private String[] columnNames;

    public RuleTesterDialog(JFrame parent, DefaultTableModel tableModel) {
        super(parent, "Rule Tester", true);
        this.tableModel = tableModel;
        columnNames = getColumnNames();
        rulesPanel = new JPanel();
        rulesPanel.setLayout(new BoxLayout(rulesPanel, BoxLayout.Y_AXIS));
        initUI();
    }

    private String[] getColumnNames() {
        int columnCount = tableModel.getColumnCount();
        String[] columnNames = new String[columnCount - 1];
        for (int i = 0; i < columnCount - 1; i++) {
            columnNames[i] = tableModel.getColumnName(i);
        }
        return columnNames;
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add initial rule panel
        addNewRulePanel();

        JButton addRuleButton = new JButton("Add Rule");
        addRuleButton.addActionListener(e -> addNewRulePanel());

        JScrollPane scrollPane = new JScrollPane(rulesPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(addRuleButton, BorderLayout.SOUTH);

        JButton testButton = new JButton("Test Rule");
        testButton.addActionListener(e -> testRule());
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(testButton, BorderLayout.SOUTH);

        add(panel);
        setPreferredSize(new Dimension(600, 400));
        pack();
        setLocationRelativeTo(getParent());
    }

    private void addNewRulePanel() {
        RulePanel rulePanel = new RulePanel(columnNames, getUniqueClassNames());
        rulePanels.add(rulePanel);
        rulesPanel.add(rulePanel);
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }

    private void removeRulePanel(RulePanel rulePanel) {
        rulesPanel.remove(rulePanel);
        rulePanels.remove(rulePanel);
        rulesPanel.revalidate();
        rulesPanel.repaint();
    }

    private void testRule() {
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();
        String[] uniqueClassNames = getUniqueClassNames();

        // Initialize the confusion matrix with all class combinations including "None"
        for (String actualClass : uniqueClassNames) {
            confusionMatrix.put(actualClass, new HashMap<>());
            for (String predictedClass : uniqueClassNames) {
                confusionMatrix.get(actualClass).put(predictedClass, 0);
            }
            confusionMatrix.get(actualClass).put("None", 0);
        }
        Map<String, Integer> noneMap = new HashMap<>();
        for (String predictedClass : uniqueClassNames) {
            noneMap.put(predictedClass, 0);
        }
        noneMap.put("None", 0);
        confusionMatrix.put("None", noneMap);

        int totalInstances = 0;
        int correctPredictions = 0;

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            totalInstances++;
            String predictedClass = "None";

            for (RulePanel rulePanel : rulePanels) {
                boolean match = true;
                for (ClausePanel clausePanel : rulePanel.getClausePanels()) {
                    String attribute = clausePanel.getAttribute();
                    String relation1 = clausePanel.getRelation1();
                    String relation2 = clausePanel.getRelation2();
                    String value1 = clausePanel.getValue1();
                    String value2 = clausePanel.getValue2();

                    int columnIndex = tableModel.findColumn(attribute);
                    double cellValue = Double.parseDouble(tableModel.getValueAt(row, columnIndex).toString());

                    boolean clauseMatch = evaluateRule(cellValue, relation1, value1, relation2, value2);

                    if (clausePanel != rulePanel.getClausePanels().get(0)) {
                        if ("AND".equals(clausePanel.getAndOr())) {
                            match = match && clauseMatch;
                        } else if ("OR".equals(clausePanel.getAndOr())) {
                            match = match || clauseMatch;
                        }
                    } else {
                        match = clauseMatch;
                    }
                }

                if (match) {
                    predictedClass = rulePanel.getSelectedClass();
                    break;
                }
            }

            String actualClass = (String) tableModel.getValueAt(row, tableModel.getColumnCount() - 1);
            confusionMatrix.get(actualClass).put(predictedClass,
                    confusionMatrix.get(actualClass).get(predictedClass) + 1);

            if (actualClass.equals(predictedClass)) {
                correctPredictions++;
            }
        }

        double accuracy = (double) correctPredictions / totalInstances * 100;

        showConfusionMatrix(confusionMatrix, uniqueClassNames, accuracy);
    }

    private boolean evaluateRule(double cellValue, String relation1, String value1, String relation2, String value2) {
        if (relation1.equals("<")) {
            relation1 = ">";
        } else if (relation1.equals("<=")) {
            relation1 = ">=";
        } else if (relation1.equals(">")) {
            relation1 = "<";
        } else if (relation1.equals(">=")) {
            relation1 = "<=";
        }
        boolean match1 = evaluateCondition(cellValue, relation1, Double.parseDouble(value1));
        boolean match2 = value2.isEmpty() || evaluateCondition(cellValue, relation2, Double.parseDouble(value2));
        return match1 && match2;
    }

    private boolean evaluateCondition(double cellValue, String relation, double value) {
        switch (relation) {
            case "<":
                return cellValue < value;
            case "<=":
                return cellValue <= value;
            case ">":
                return cellValue > value;
            case ">=":
                return cellValue >= value;
            case "==":
                return cellValue == value;
            case "!=":
                return cellValue != value;
            default:
                return false;
        }
    }

    private void showConfusionMatrix(Map<String, Map<String, Integer>> confusionMatrix, String[] classNames, double accuracy) {
        String[] columnNames = new String[classNames.length + 2];
        columnNames[0] = "Actual\\Predicted";
        System.arraycopy(classNames, 0, columnNames, 1, classNames.length);
        columnNames[columnNames.length - 1] = "None";

        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        for (String actualClass : classNames) {
            Object[] rowData = new Object[columnNames.length];
            rowData[0] = actualClass;
            for (int i = 1; i < columnNames.length; i++) {
                rowData[i] = confusionMatrix.get(actualClass).get(columnNames[i]);
            }
            model.addRow(rowData);
        }

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel accuracyLabel = new JLabel(String.format("Accuracy: %.2f%%", accuracy));
        accuracyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(accuracyLabel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, panel, "Confusion Matrix", JOptionPane.INFORMATION_MESSAGE);
    }

    private String[] getUniqueClassNames() {
        int classColumnIndex = tableModel.getColumnCount() - 1;
        Set<String> uniqueClasses = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            uniqueClasses.add((String) tableModel.getValueAt(i, classColumnIndex));
        }
        return uniqueClasses.toArray(new String[0]);
    }

    private class RulePanel extends JPanel {
        private final JComboBox<String> classBox;
        private final JButton removeButton;
        private final JButton addClauseButton;
        private final List<ClausePanel> clausePanels = new ArrayList<>();

        public RulePanel(String[] attributes, String[] classNames) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel classPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            classBox = new JComboBox<>(classNames);
            classPanel.add(new JLabel("Class:"));
            classPanel.add(classBox);
            add(classPanel);

            ClausePanel initialClausePanel = new ClausePanel(attributes, false);
            clausePanels.add(initialClausePanel);
            add(initialClausePanel);

            addClauseButton = new JButton("Add Clause");
            addClauseButton.addActionListener(e -> addClause(attributes));
            add(addClauseButton);

            removeButton = new JButton("Remove Rule");
            removeButton.addActionListener(e -> removeRulePanel(RulePanel.this));
            add(removeButton);
        }

        public String getSelectedClass() {
            return (String) classBox.getSelectedItem();
        }

        public List<ClausePanel> getClausePanels() {
            return clausePanels;
        }

        private void addClause(String[] attributes) {
            ClausePanel clausePanel = new ClausePanel(attributes, true);
            clausePanels.add(clausePanel);
            add(clausePanel, getComponentCount() - 2); // Add before addClauseButton and removeButton
            revalidate();
            repaint();
        }
    }

    private class ClausePanel extends JPanel {
        private final JComboBox<String> attributeBox;
        private final JComboBox<String> relationBox1;
        private final JComboBox<String> relationBox2;
        private final JTextField valueField1;
        private final JTextField valueField2;
        private final JComboBox<String> andOrBox;
        private final JButton removeButton;

        public ClausePanel(String[] attributes, boolean includeAndOr) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
            attributeBox = new JComboBox<>(attributes);
            relationBox1 = new JComboBox<>(new String[]{"<", "<=", ">", ">=", "==", "!="});
            relationBox2 = new JComboBox<>(new String[]{"<", "<=", ">", ">=", "==", "!=", ""});
            valueField1 = new JTextField(5);
            valueField2 = new JTextField(5);

            removeButton = new JButton("x");
            removeButton.setMargin(new Insets(0, 5, 0, 5));
            removeButton.addActionListener(e -> removeClausePanel(ClausePanel.this));

            if (includeAndOr) {
                andOrBox = new JComboBox<>(new String[]{"AND", "OR"});
                add(andOrBox, 0); // Add the AND/OR selector at the beginning
            } else {
                andOrBox = null;
            }

            add(valueField1);
            add(relationBox1);
            add(attributeBox);
            add(relationBox2);
            add(valueField2);
            add(removeButton);
        }

        private void removeClausePanel(ClausePanel clausePanel) {
            RulePanel parent = (RulePanel) clausePanel.getParent();
            parent.remove(clausePanel);
            parent.getClausePanels().remove(clausePanel);
            parent.revalidate();
            parent.repaint();
        }

        public String getAttribute() {
            return (String) attributeBox.getSelectedItem();
        }

        public String getRelation1() {
            return (String) relationBox1.getSelectedItem();
        }

        public String getRelation2() {
            return (String) relationBox2.getSelectedItem();
        }

        public String getValue1() {
            return valueField1.getText();
        }

        public String getValue2() {
            return valueField2.getText();
        }

        public String getAndOr() {
            return andOrBox != null ? (String) andOrBox.getSelectedItem() : "";
        }
    }
}
