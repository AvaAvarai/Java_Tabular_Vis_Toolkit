package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class RuleTesterDialog extends JDialog {

    private final DefaultTableModel tableModel;
    private final JPanel rulesPanel;
    private final JComboBox<String> classComboBox;
    private final List<RulePanel> rulePanels = new ArrayList<>();
    private String[] columnNames;

    public RuleTesterDialog(JFrame parent, DefaultTableModel tableModel) {
        super(parent, "Rule Tester", true);
        this.tableModel = tableModel;
        columnNames = getColumnNames();
        classComboBox = new JComboBox<>(getUniqueClassNames());
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
        addNewRulePanel(null);

        JButton addRuleButton = new JButton("Add Rule");
        addRuleButton.addActionListener(e -> addNewRulePanel("AND/OR"));

        JScrollPane scrollPane = new JScrollPane(rulesPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(addRuleButton, BorderLayout.SOUTH);

        JPanel classPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        classPanel.add(new JLabel("Class:"));
        classPanel.add(classComboBox);
        mainPanel.add(classPanel, BorderLayout.NORTH);

        JButton testButton = new JButton("Test Rule");
        testButton.addActionListener(e -> testRule());
        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(testButton, BorderLayout.SOUTH);

        add(panel);
        setPreferredSize(new Dimension(600, 400));
        pack();
        setLocationRelativeTo(getParent());
    }

    private String[] getUniqueClassNames() {
        int classColumnIndex = tableModel.getColumnCount() - 1;
        List<String> uniqueClasses = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String className = (String) tableModel.getValueAt(i, classColumnIndex);
            if (!uniqueClasses.contains(className)) {
                uniqueClasses.add(className);
            }
        }
        return uniqueClasses.toArray(new String[0]);
    }

    private void addNewRulePanel(String conjunction) {
        RulePanel rulePanel = new RulePanel(columnNames, conjunction);
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
        String selectedClass = (String) classComboBox.getSelectedItem();
        List<Integer> truePositives = new ArrayList<>();
        List<Integer> trueNegatives = new ArrayList<>();
        List<Integer> falsePositives = new ArrayList<>();
        List<Integer> falseNegatives = new ArrayList<>();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            boolean match = true;
            for (RulePanel rulePanel : rulePanels) {
                String attribute = rulePanel.getAttribute();
                String relation1 = rulePanel.getRelation1();
                String relation2 = rulePanel.getRelation2();
                String value1 = rulePanel.getValue1();
                String value2 = rulePanel.getValue2();
                String conjunction = rulePanel.getConjunction();

                int columnIndex = tableModel.findColumn(attribute);
                double cellValue = Double.parseDouble(tableModel.getValueAt(row, columnIndex).toString());

                boolean ruleMatch = evaluateRule(cellValue, relation1, value1, relation2, value2);

                if ("AND".equals(conjunction)) {
                    match = match && ruleMatch;
                } else if ("OR".equals(conjunction)) {
                    match = match || ruleMatch;
                } else {
                    match = ruleMatch;
                }
            }

            String actualClass = (String) tableModel.getValueAt(row, tableModel.getColumnCount() - 1);
            if (match && actualClass.equals(selectedClass)) {
                truePositives.add(row);
            } else if (match && !actualClass.equals(selectedClass)) {
                falsePositives.add(row);
            } else if (!match && actualClass.equals(selectedClass)) {
                falseNegatives.add(row);
            } else {
                trueNegatives.add(row);
            }
        }

        int tp = truePositives.size();
        int fp = falsePositives.size();
        int fn = falseNegatives.size();
        int tn = trueNegatives.size();
        int total = tp + fp + fn + tn;
        double accuracy = (double) (tp + tn) / total * 100;

        showConfusionMatrix(tp, fp, fn, tn, accuracy);
    }

    private boolean evaluateRule(double cellValue, String relation1, String value1, String relation2, String value2) {
        boolean match1 = evaluateCondition1(cellValue, relation1, Double.parseDouble(value1));
        boolean match2 = value2.isEmpty() || evaluateCondition2(cellValue, relation2, Double.parseDouble(value2));
        return match1 && match2;
    }

    private boolean evaluateCondition1(double cellValue, String relation, double value) {
        switch (relation) {
            case "<":
                return cellValue > value;
            case "<=":
                return cellValue >= value;
            case ">":
                return cellValue < value;
            case ">=":
                return cellValue <= value;
            case "==":
                return cellValue == value;
            case "!=":
                return cellValue != value;
            default:
                return false;
        }
    }

    private boolean evaluateCondition2(double cellValue, String relation, double value) {
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

    private void showConfusionMatrix(int tp, int fp, int fn, int tn, double accuracy) {
        JOptionPane.showMessageDialog(this, String.format(
                "Confusion Matrix:\n\nTrue Positives: %d\nFalse Positives: %d\nFalse Negatives: %d\nTrue Negatives: %d\n\nAccuracy: %.2f%%",
                tp, fp, fn, tn, accuracy), "Confusion Matrix", JOptionPane.INFORMATION_MESSAGE);
    }

    private class RulePanel extends JPanel {
        private final JComboBox<String> attributeBox;
        private final JComboBox<String> relationBox1;
        private final JComboBox<String> relationBox2;
        private final JTextField valueField1;
        private final JTextField valueField2;
        private final JComboBox<String> conjunctionBox;
        private final JButton removeButton;

        public RulePanel(String[] attributes, String conjunction) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
            attributeBox = new JComboBox<>(attributes);
            relationBox1 = new JComboBox<>(new String[]{"<", "<=", ">", ">=", "==", "!="});
            relationBox2 = new JComboBox<>(new String[]{"<", "<=", ">", ">=", "==", "!=", ""});
            valueField1 = new JTextField(5);
            valueField2 = new JTextField(5);

            removeButton = new JButton("x");
            removeButton.setMargin(new Insets(0, 5, 0, 5));
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeRulePanel(RulePanel.this);
                }
            });

            if (conjunction != null) {
                conjunctionBox = new JComboBox<>(new String[]{"AND", "OR"});
                conjunctionBox.setSelectedItem(conjunction);
                add(conjunctionBox);
            } else {
                conjunctionBox = null;
            }
            add(valueField1);
            add(relationBox1);
            add(attributeBox);
            add(relationBox2);
            add(valueField2);
            add(removeButton);
        }

        public String getConjunction() {
            return conjunctionBox != null ? (String) conjunctionBox.getSelectedItem() : "";
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
    }
}
