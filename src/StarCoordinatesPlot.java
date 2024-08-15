package src;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;

public class StarCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;

    public StarCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;

        setTitle("Star Coordinates Plot");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new StarCoordinatesPanel(), BorderLayout.CENTER);
        add(createLegendPanel(), BorderLayout.SOUTH);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

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
                    g2.translate(15, 15);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(30, 30));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);

            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class StarCoordinatesPanel extends JPanel {
        private static final int PADDING = 20;

        public StarCoordinatesPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int plotSize = Math.min(getWidth(), getHeight()) - 2 * PADDING; // Calculate plot size based on available space
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // Number of attributes excluding the class column
            int numAttributes = attributeNames.size();
            double angleIncrement = 2 * Math.PI / numAttributes;

            // Draw the star coordinates for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row)) {
                    drawStar(g2, row, centerX, centerY, plotSize / 2, angleIncrement, false);
                }
            }

            // Highlight selected rows
            for (int row = 0; row < data.get(0).size(); row++) {
                if (selectedRows.contains(row)) {
                    drawStar(g2, row, centerX, centerY, plotSize / 2, angleIncrement, true);
                }
            }
        }

        private void drawStar(Graphics2D g2, int row, int centerX, int centerY, double radius, double angleIncrement, boolean highlight) {
            Path2D starPath = new Path2D.Double();
            for (int i = 0; i < attributeNames.size(); i++) {
                double value = data.get(i).get(row);
                double normValue = (value - getMin(data.get(i))) / (getMax(data.get(i)) - getMin(data.get(i)));
                double angle = i * angleIncrement;
    
                double x = centerX + radius * normValue * Math.cos(angle);
                double y = centerY - radius * normValue * Math.sin(angle);
    
                if (i == 0) {
                    starPath.moveTo(x, y);
                } else {
                    starPath.lineTo(x, y);
                }
            }
    
            starPath.closePath();
    
            if (highlight) {
                g2.setPaint(Color.YELLOW);
                g2.setStroke(new BasicStroke(2));
            } else {
                g2.setPaint(classColors.get(classLabels.get(row)));
                g2.setStroke(new BasicStroke(1));
            }
    
            g2.draw(starPath);
        }

        private double getMin(List<Double> data) {
            return data.stream().min(Double::compare).orElse(Double.NaN);
        }

        private double getMax(List<Double> data) {
            return data.stream().max(Double::compare).orElse(Double.NaN);
        }
    }
}
