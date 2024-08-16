package src;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassColorDialog extends JDialog {
    private final Map<String, Color> classColors;
    private final Map<String, Shape> classShapes;
    private final JComboBox<String> classComboBox;
    private final JPanel legendPanel;
    private final Map<String, Color> tempClassColors = new HashMap<>();
    private final Map<String, Shape> tempClassShapes = new HashMap<>();
    private final Shape[] availableShapes;
    private JRadioButton[] shapeButtons; // Now class-level
    private final Map<JRadioButton, Shape> shapeMap = new HashMap<>(); // To associate buttons with shapes

    public ClassColorDialog(JFrame parent, Map<String, Color> classColors, Map<String, Shape> classShapes, Set<String> classNames) {
        super(parent, "Select Class, Color & Shape", true);
        this.classColors = classColors;
        this.classShapes = classShapes;

        availableShapes = new Shape[]{
                new Ellipse2D.Double(-5, -5, 10, 10),
                new Rectangle2D.Double(-5, -5, 10, 10),
                new Polygon(new int[]{-5, 5, 0}, new int[]{-5, -5, 5}, 3),
                ShapeUtils.createStar(4, 10, 5),
                ShapeUtils.createStar(5, 10, 5),
                ShapeUtils.createStar(6, 10, 5),
                ShapeUtils.createStar(7, 10, 5),
                ShapeUtils.createStar(8, 10, 5)
        };

        for (String className : classNames) {
            tempClassColors.put(className, classColors.getOrDefault(className, Color.WHITE));
            tempClassShapes.put(className, classShapes.getOrDefault(className, availableShapes[0]));
        }

        setLayout(new BorderLayout());
        classComboBox = createClassComboBox(classNames);
        legendPanel = createLegendPanel(classNames);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(classComboBox, BorderLayout.NORTH);
        mainPanel.add(legendPanel, BorderLayout.CENTER);
        mainPanel.add(createShapePickerPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setSize(getWidth(), getHeight() + 75);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private JComboBox<String> createClassComboBox(Set<String> classNames) {
        JComboBox<String> comboBox = new JComboBox<>(classNames.toArray(new String[0]));
        comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = new JLabel(value);
            JLabel colorSwatch = new JLabel();
            colorSwatch.setOpaque(true);
            colorSwatch.setPreferredSize(new Dimension(30, 30));
            colorSwatch.setBackground(tempClassColors.getOrDefault(value, Color.WHITE));

            JLabel shapeSwatch = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    Shape shape = tempClassShapes.getOrDefault(value, new Ellipse2D.Double(-6, -6, 12, 12));
                    g2.fill(shape);
                }
            };
            shapeSwatch.setPreferredSize(new Dimension(30, 30));

            panel.add(colorSwatch, BorderLayout.WEST);
            panel.add(shapeSwatch, BorderLayout.CENTER);
            panel.add(label, BorderLayout.EAST);

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return panel;
        });

        comboBox.addActionListener(e -> updateShapeSelection());
        return comboBox;
    }

    private JPanel createLegendPanel(Set<String> classNames) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Current Class Colors & Shapes"));

        for (String className : classNames) {
            JPanel colorLabelPanel = new JPanel(new BorderLayout());

            JLabel colorBox = new JLabel();
            colorBox.setOpaque(true);
            colorBox.setBackground(tempClassColors.getOrDefault(className, Color.WHITE));
            colorBox.setPreferredSize(new Dimension(20, 20));

            JLabel shapeBox = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    g2.fill(tempClassShapes.getOrDefault(className, new Ellipse2D.Double(-3, -3, 6, 6)));
                }
            };
            shapeBox.setPreferredSize(new Dimension(20, 20));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.add(colorBox, BorderLayout.WEST);
            colorLabelPanel.add(shapeBox, BorderLayout.CENTER);
            colorLabelPanel.add(label, BorderLayout.EAST);

            panel.add(colorLabelPanel);
        }

        return panel;
    }

    private JPanel createShapePickerPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Pick Shape"));

        ButtonGroup shapeButtonGroup = new ButtonGroup();
        shapeButtons = new JRadioButton[availableShapes.length];

        for (int i = 0; i < availableShapes.length; i++) {
            Shape shape = availableShapes[i];
            shapeButtons[i] = new JRadioButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.BLACK);
                    g2.translate(20, 15);
                    g2.scale(1.5, 1.5);
                    g2.fill(shape);
                    g2.translate(-15, -15);
                }
            };
            shapeButtons[i].setPreferredSize(new Dimension(40, 40));
            shapeButtonGroup.add(shapeButtons[i]);
            panel.add(shapeButtons[i]);

            // Map each button to its corresponding shape
            shapeMap.put(shapeButtons[i], shape);

            shapeButtons[i].addActionListener(e -> {
                String selectedClass = (String) classComboBox.getSelectedItem();
                tempClassShapes.put(selectedClass, shape);
            });
        }

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton setColorButton = new JButton("Set Color");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        setColorButton.addActionListener(e -> {
            String selectedClass = (String) classComboBox.getSelectedItem();
            Color color = JColorChooser.showDialog(this, "Choose color for " + selectedClass, tempClassColors.getOrDefault(selectedClass, Color.WHITE));
            if (color != null) {
                tempClassColors.put(selectedClass, color);
                classComboBox.repaint();
            }
        });

        okButton.addActionListener(e -> {
            classColors.putAll(tempClassColors);
            classShapes.putAll(tempClassShapes);
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        panel.add(setColorButton);
        panel.add(okButton);
        panel.add(cancelButton);

        return panel;
    }

    private void updateShapeSelection() {
        String selectedClass = (String) classComboBox.getSelectedItem();
        Shape currentShape = tempClassShapes.get(selectedClass);

        for (JRadioButton shapeButton : shapeButtons) {
            if (shapeMap.get(shapeButton).equals(currentShape)) {
                shapeButton.setSelected(true);
                break;
            }
        }
    }
}
