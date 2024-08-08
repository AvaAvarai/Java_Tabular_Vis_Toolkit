package src;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ShiftedPairedCoordinates extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private List<String> classLabels;
    private int numPlots;

    public ShiftedPairedCoordinates(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, List<String> classLabels, int numPlots) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classLabels = classLabels;
        this.numPlots = numPlots;

        setTitle("Shifted Paired Coordinates");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new ShiftedPairedCoordinatesPanel(), BorderLayout.CENTER);
    }

    private class ShiftedPairedCoordinatesPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int plotWidth = getWidth() / numPlots;
            int plotHeight = getHeight() - 50; // leave space for labels

            // Draw scatter plots
            for (int i = 0; i < numPlots; i++) {
                int x = i * plotWidth;
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    // If we are at the last plot and there's an odd number of attributes, duplicate the last attribute
                    attrIndex2 = attrIndex1;
                }
                drawScatterPlot(g2, data.get(attrIndex1), data.get(attrIndex2), x, 0, plotWidth, plotHeight, attributeNames.get(attrIndex1), attributeNames.get(attrIndex2));
            }

            // Draw connecting lines between plots
            for (int row = 0; row < data.get(0).size(); row++) {
                for (int i = 0; i < numPlots - 1; i++) {
                    int attrIndex1 = i * 2;
                    int attrIndex2 = (i * 2) + 1;
                    if (attrIndex2 >= data.size()) {
                        attrIndex2 = attrIndex1;
                    }

                    int nextAttrIndex1 = attrIndex2;
                    int nextAttrIndex2 = (i + 1) * 2;
                    if (nextAttrIndex2 >= data.size()) {
                        nextAttrIndex2 = nextAttrIndex1;
                    }

                    int plotX1 = i * plotWidth + 20;
                    int plotY1 = 20;
                    int plotSize = Math.min(plotWidth, plotHeight) - 40;

                    int plotX2 = (i + 1) * plotWidth + 20;
                    int plotY2 = 20;

                    double normX1 = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                    double normY1 = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));

                    double normX2 = (data.get(nextAttrIndex1).get(row) - getMin(data.get(nextAttrIndex1))) / (getMax(data.get(nextAttrIndex1)) - getMin(data.get(nextAttrIndex1)));
                    double normY2 = (data.get(nextAttrIndex2).get(row) - getMin(data.get(nextAttrIndex2))) / (getMax(data.get(nextAttrIndex2)) - getMin(data.get(nextAttrIndex2)));

                    int x1 = plotX1 + (int) (plotSize * normX1);
                    int y1 = plotY1 + plotSize - (int) (plotSize * normY1);

                    int x2 = plotX2 + (int) (plotSize * normX2);
                    int y2 = plotY2 + plotSize - (int) (plotSize * normY2);

                    g2.setColor(classColors.getOrDefault(classLabels.get(row), Color.BLACK));
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
        }

        private void drawScatterPlot(Graphics2D g2, List<Double> xData, List<Double> yData, int x, int y, int width, int height, String xLabel, String yLabel) {
            int plotSize = Math.min(width, height) - 40;
            int plotX = x + 20;
            int plotY = y + 20;

            // Draw axes
            g2.drawLine(plotX, plotY, plotX, plotY + plotSize);
            g2.drawLine(plotX, plotY + plotSize, plotX + plotSize, plotY + plotSize);

            // Draw labels
            g2.drawString(xLabel, plotX + plotSize / 2, plotY + plotSize + 20);
            g2.drawString(yLabel, plotX - 20, plotY + plotSize / 2);

            // Draw data points
            for (int i = 0; i < xData.size(); i++) {
                double normX = (xData.get(i) - getMin(xData)) / (getMax(xData) - getMin(xData));
                double normY = (yData.get(i) - getMin(yData)) / (getMax(yData) - getMin(yData));

                int px = plotX + (int) (normX * plotSize);
                int py = plotY + plotSize - (int) (normY * plotSize);

                // Get class label and color
                String classLabel = classLabels.get(i);
                Color color = classColors.getOrDefault(classLabel, Color.BLACK);

                g2.setColor(color);
                g2.fillOval(px - 3, py - 3, 6, 6);
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
