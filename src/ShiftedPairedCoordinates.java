package src;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;

public class ShiftedPairedCoordinates extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private int numPlots;
    private List<Integer> selectedRows;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);

    public ShiftedPairedCoordinates(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, int numPlots, List<Integer> selectedRows) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.numPlots = numPlots;
        this.selectedRows = selectedRows;

        setTitle("Shifted Paired Coordinates");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set the layout and background color of the main content pane
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Add the plot panel at the center
        add(new ShiftedPairedCoordinatesPanel(), BorderLayout.CENTER);

        // Add a legend panel at the bottom (horizontal)
        add(createLegendPanel(), BorderLayout.SOUTH);
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE); // Set background color of the legend panel

        // Add each class color and shape to the legend
        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE); // Set background color for individual legend items

            // Create a larger shape symbol
            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(color);
                    g2.translate(10, 10);
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

    private class ShiftedPairedCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20; // Add 20px padding between title and plot

        public ShiftedPairedCoordinatesPanel() {
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
            String title = "Shifted Paired Coordinates Plot";
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics(TITLE_FONT);
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            // Set the background color for the plot area
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, titleHeight + TITLE_PADDING, getWidth(), getHeight() - titleHeight - TITLE_PADDING);

            int plotWidth = getWidth() / numPlots - 10;
            int plotHeight = getHeight() - titleHeight - TITLE_PADDING - 50; // leave space for labels and title

            // Draw scatter plots
            for (int i = 0; i < numPlots; i++) {
                int x = i * plotWidth;
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }
                drawScatterPlot(g2, data.get(attrIndex1), data.get(attrIndex2), x, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight, attributeNames.get(attrIndex1), attributeNames.get(attrIndex2));
            }

            // Draw connecting lines between plots
            for (int row = 0; row < data.get(0).size(); row++) {
                for (int i = 0; i < numPlots - 1; i++) {
                    int attrIndex1 = i * 2;
                    int attrIndex2 = (i * 2) + 1;
                    if (attrIndex2 >= data.size()) {
                        attrIndex2 = attrIndex1;
                    }

                    int plotX1 = i * plotWidth + 40;
                    int plotY1 = titleHeight + TITLE_PADDING + 10;
                    int plotSize = Math.min(plotWidth, plotHeight) - 40;

                    double normX1 = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                    double normY1 = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));

                    int x1 = plotX1 + (int) (plotSize * normX1);
                    int y1 = plotY1 + plotSize - (int) (plotSize * normY1) + 20;

                    // Set the line color to yellow if the row is selected
                    if (selectedRows.contains(row)) {
                        g2.setColor(Color.YELLOW);
                    } else {
                        g2.setColor(classColors.getOrDefault(classLabels.get(row), Color.BLACK));
                    }

                    int nextAttrIndex1 = (i + 1) * 2;
                    int nextAttrIndex2 = (i + 1) * 2 + 1;
                    if (nextAttrIndex2 >= data.size()) {
                        nextAttrIndex2 = nextAttrIndex1;
                    }

                    int plotX2 = (i + 1) * plotWidth + 40;
                    int plotY2 = titleHeight + TITLE_PADDING + 10;

                    double normX2 = (data.get(nextAttrIndex1).get(row) - getMin(data.get(nextAttrIndex1))) / (getMax(data.get(nextAttrIndex1)) - getMin(data.get(nextAttrIndex1)));
                    double normY2 = (data.get(nextAttrIndex2).get(row) - getMin(data.get(nextAttrIndex2))) / (getMax(data.get(nextAttrIndex2)) - getMin(data.get(nextAttrIndex2)));

                    int x2 = plotX2 + (int) (plotSize * normX2);
                    int y2 = plotY2 + plotSize - (int) (plotSize * normY2) + 20;

                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        }

        private void drawScatterPlot(Graphics2D g2, List<Double> xData, List<Double> yData, int x, int y, int width, int height, String xLabel, String yLabel) {
            int plotSize = Math.min(width, height) - 40;
            int plotX = x + 40;
            int plotY = y + 20;

            // Draw axes
            g2.setColor(Color.BLACK);
            g2.drawLine(plotX, plotY, plotX, plotY + plotSize);
            g2.drawLine(plotX, plotY + plotSize, plotX + plotSize, plotY + plotSize);

            // Draw labels
            g2.setFont(AXIS_LABEL_FONT);
            g2.setColor(Color.BLACK);
            g2.drawString(xLabel, plotX + plotSize / 2, plotY + plotSize + 20);
            g2.drawString(yLabel, plotX - g2.getFontMetrics().stringWidth(yLabel) / 2, plotY - 10);

            // Draw data points using class shapes
            for (int i = 0; i < xData.size(); i++) {
                double normX = (xData.get(i) - getMin(xData)) / (getMax(xData) - getMin(xData));
                double normY = (yData.get(i) - getMin(yData)) / (getMax(yData) - getMin(yData));

                int px = plotX + (int) (normX * plotSize);
                int py = plotY + plotSize - (int) (normY * plotSize);

                // Get class label, color, and shape
                String classLabel = classLabels.get(i);
                Color color = classColors.getOrDefault(classLabel, Color.BLACK);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));

                g2.setColor(color);
                g2.translate(px, py);
                g2.fill(shape);
                g2.translate(-px, -py);
            }
        }

        private double getMin(List<Double> data) {
            return data.stream().min(Double::compare).orElse(Double.NaN);
        }

        private double getMax(List<Double> data) {
            return data.stream().max(Double::compare).orElse(Double.NaN);
        }
    }
}
