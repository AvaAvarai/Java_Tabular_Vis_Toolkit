package src;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.List;
import java.util.Map;

public class StaticCircularCoordinatesPlot extends JFrame {
    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private int numAttributes;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);

    public StaticCircularCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.numAttributes = attributeNames.size();
        setTitle("Static Circular Coordinates");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Set the layout and background color of the main content pane
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Add the plot panel at the center
        add(new StaticCircularCoordinatesPanel(), BorderLayout.CENTER);

        // Add a legend panel at the bottom (horizontal)
        add(createLegendPanel(), BorderLayout.SOUTH);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

        // Add each class color and shape to the legend
        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE);

            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(color);
                    g2.translate(32, 20);
                    g2.scale(2, 2);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(40, 40));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);

            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class StaticCircularCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20; // Add 20px padding between title and plot

        public StaticCircularCoordinatesPanel() {
            // Set the panel's background to transparent
            setBackground(new Color(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Set the background color for the entire panel to white
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw the title above the grey background
            String title = "Static Circular Coordinates Plot";
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics(TITLE_FONT);
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            // Calculate plot area dimensions
            int plotAreaY = titleHeight + TITLE_PADDING;
            int plotAreaHeight = getHeight() - plotAreaY;

            // Set the background color for the plot area
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, plotAreaY, getWidth(), plotAreaHeight);

            int radius = Math.min(getWidth() / 2, plotAreaHeight / 2) - 50;
            int centerX = getWidth() / 2;
            int centerY = plotAreaY + plotAreaHeight / 2;
            double angleStep = 2 * Math.PI / numAttributes;

            Point2D.Double[] attributePositions = new Point2D.Double[numAttributes];

            // Draw the circular axis
            g2.setColor(Color.BLACK);
            g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));

            // Calculate positions on the circumference for each attribute
            for (int i = 0; i < numAttributes; i++) {
                double angle = i * angleStep - Math.PI / 2;  // Start at the top (12 o'clock)
                int labelradius1 = radius + 15; 
                int labelradius2 = radius + 20;
                if (i > numAttributes / 2) {
                    labelradius1 = radius + 95;
                    labelradius2 = radius + 30;
                }
                double x = centerX + labelradius1 * Math.cos(angle);
                double y = centerY + labelradius2 * Math.sin(angle);
                attributePositions[i] = new Point2D.Double(x, y);

                // Draw attribute labels
                g2.setFont(AXIS_LABEL_FONT); // Use the defined font for axis labels
                g2.drawString(attributeNames.get(i), (int) x, (int) y);
            }

            // Draw non-selected rows first
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row)) {
                    drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, false);
                }
            }

            // Draw selected rows last (highlighted in yellow)
            for (int row : selectedRows) {
                drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, true);
            }
        }

        private void drawRow(Graphics2D g2, int row, Point2D.Double[] attributePositions, int centerX, int centerY, int radius, double angleStep, boolean isSelected) {
            Point2D.Double[] points = new Point2D.Double[numAttributes];
            String classLabel = classLabels.get(row);
            Color color = isSelected ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
        
            // Calculate points
            for (int i = 0; i < numAttributes; i++) {
                double value = data.get(i).get(row);
                double minValue = data.get(i).stream().min(Double::compare).orElse(0.0);
                double maxValue = data.get(i).stream().max(Double::compare).orElse(1.0);
        
                // Calculate the corresponding angle based on value directly between the min and max
                double angleOffset = (value - minValue) / (maxValue - minValue) * angleStep;
        
                double x = centerX + radius * Math.cos(i * angleStep + angleOffset - Math.PI / 2);
                double y = centerY + radius * Math.sin(i * angleStep + angleOffset - Math.PI / 2);
                points[i] = new Point2D.Double(x, y);
            }
        
            // Connect points sequentially with class color or yellow if selected
            g2.setColor(color);
            for (int i = 0; i < numAttributes - 1; i++) {
                g2.draw(new Line2D.Double(points[i], points[i + 1]));
            }
            g2.draw(new Line2D.Double(points[numAttributes - 1], points[0]));
        
            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);  // Move the origin to the point location
                g2.fill(shape);       // Draw the shape at the translated origin
                g2.translate(-points[i].x, -points[i].y); // Move back the origin
            }
        }        
    }
}
