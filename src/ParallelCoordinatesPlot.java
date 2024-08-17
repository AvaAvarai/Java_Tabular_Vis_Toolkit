package src;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class ParallelCoordinatesPlot extends JFrame {

    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<PureRegion> pureRegions;

    public ParallelCoordinatesPlot(List<String[]> data, String[] columnNames, Map<String, Color> classColors, int classColumnIndex, int[] columnOrder, List<Integer> selectedRows, Map<String, Shape> classShapes) {
        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        this.classColors = classColors;
        this.classShapes = classShapes;

        DefaultCategoryDataset dataset = createDataset(data, columnNames, classColumnIndex, columnOrder);
        JFreeChart chart = createChart(dataset, columnNames);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        LineAndShapeRenderer renderer = new CustomRenderer(dataset, columnNames, selectedRows);
        plot.setRenderer(renderer);

        // Manually create a custom legend
        LegendItemCollection legendItems = new LegendItemCollection();
        Set<String> addedClasses = new HashSet<>();

        for (String className : classColors.keySet()) {
            if (!addedClasses.contains(className)) {
                Shape shape = classShapes.get(className);
                Paint paint = classColors.get(className);
                LegendItem item = new LegendItem(className, "", "", "", shape, paint, new BasicStroke(1.0f), paint);
                legendItems.add(item);
                addedClasses.add(className);
            }
        }

        plot.setFixedLegendItems(legendItems);

        // Adjust the category axis to increase space between the axes
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryMargin(0.3); // Increase space between categories

        // Set chart width as a multiple of the number of attributes, but without stretching components
        int attributeCount = columnNames.length;
        int baseWidth = 150; // Adjust base width as necessary
        int chartWidth = attributeCount * baseWidth;

        // Wrap the chart in a JPanel
        JPanel chartPanel = new JPanel(new BorderLayout());
        ChartPanel cp = new ChartPanel(chart);

        // Set the preferred size based on the attribute count
        cp.setPreferredSize(new Dimension(chartWidth, 600));  // Height is kept constant, adjust as needed
        cp.setMaximumDrawWidth(chartWidth);  // Prevents stretching of the chart components
        cp.setMaximumDrawHeight(600);

        chartPanel.add(cp, BorderLayout.CENTER);

        // Add the chart panel to a JScrollPane
        JScrollPane scrollPane = new JScrollPane(chartPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Set the scroll pane as the content pane of the JFrame
        setContentPane(scrollPane);
    }

    private DefaultCategoryDataset createDataset(List<String[]> data, String[] columnNames, int classColumnIndex, int[] columnOrder) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            String className = row[classColumnIndex];
            String seriesName = className + " - Row " + i;

            for (int j = 0; j < columnOrder.length; j++) {
                if (columnOrder[j] != classColumnIndex) {
                    try {
                        double value = Double.parseDouble(row[columnOrder[j]]);
                        dataset.addValue(value, seriesName, columnNames[j]);
                    } catch (NumberFormatException e) {
                        // Handle non-numeric data
                    }
                }
            }
        }

        return dataset;
    }

    private JFreeChart createChart(DefaultCategoryDataset dataset, String[] columnNames) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Parallel Coordinates Plot", // chart title
                "Attribute", // domain axis label
                "", // range axis label (removed)
                dataset);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDomainAxis(new CategoryAxis());
        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setVisible(false); // Hide the range axis
        plot.setRangeAxis(rangeAxis);
        plot.setRangeGridlinesVisible(false); // Remove the background grid lines

        return chart;
    }

    public void setPureRegionsOverlay(List<PureRegion> pureRegions) {
        this.pureRegions = pureRegions;
        repaint();
    }

    private class CustomRenderer extends LineAndShapeRenderer {

        private final CategoryDataset dataset;
        private final String[] columnNames;
        private final List<Integer> selectedRows;
        private List<Integer> drawOrder;
        private final Map<Integer, Double> columnMinValues = new HashMap<>();
        private final Map<Integer, Double> columnMaxValues = new HashMap<>();

        public CustomRenderer(CategoryDataset dataset, String[] columnNames, List<Integer> selectedRows) {
            this.dataset = dataset;
            this.columnNames = columnNames;
            this.selectedRows = selectedRows;
            setDrawOutlines(true); // Ensure shapes are outlined
            setUseOutlinePaint(true);
            setDefaultOutlinePaint(Color.BLACK); // Ensure the outlines are black
        }

        @Override
        public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
            if (pass != 1) return;  // Ensure we only draw on the second pass (shapes and lines).

            if (drawOrder == null) {
                drawOrder = new ArrayList<>();
                for (int i = 0; i < dataset.getRowCount(); i++) {
                    if (!selectedRows.contains(i)) {
                        drawOrder.add(i);
                    }
                }
                drawOrder.addAll(selectedRows);

                // Calculate min and max values for each column
                for (int col = 0; col < dataset.getColumnCount(); col++) {
                    double min = Double.MAX_VALUE;
                    double max = Double.MIN_VALUE;
                    for (int r = 0; r < dataset.getRowCount(); r++) {
                        double value = dataset.getValue(r, col).doubleValue();
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                    }
                    columnMinValues.put(col, min);
                    columnMaxValues.put(col, max);
                }
            }

            int actualRow = drawOrder.get(row);

            // Draw the lines in the order of the tabular view
            if (column > 0) {
                double x1 = domainAxis.getCategoryMiddle(column - 1, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                double y1 = getNormalizedY(actualRow, column - 1, dataArea);

                double x2 = domainAxis.getCategoryMiddle(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                double y2 = getNormalizedY(actualRow, column, dataArea);

                if (selectedRows.contains(actualRow)) {
                    g2.setPaint(Color.YELLOW);
                } else {
                    g2.setPaint(getItemPaint(actualRow, column));
                }
                g2.setStroke(new BasicStroke(1.0f)); // Keep line thickness constant
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
            }

            // Draw the scatterplot shapes in the order of the tabular view
            if (column == dataset.getColumnCount() - 1) {
                for (int col = 0; col < dataset.getColumnCount(); col++) {
                    double x = domainAxis.getCategoryMiddle(col, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double y = getNormalizedY(actualRow, col, dataArea);

                    // Draw the shapes in yellow if the row is selected
                    if (selectedRows.contains(actualRow)) {
                        g2.setPaint(Color.YELLOW);
                    } else {
                        g2.setPaint(getItemPaint(actualRow, col));
                    }

                    Shape shape = getItemShape(actualRow, col);
                    g2.translate(x, y);
                    g2.fill(shape);
                    g2.setStroke(new BasicStroke(1.0f)); // Keep outline thickness constant
                    g2.draw(shape); // Draw outline
                    g2.translate(-x, -y);
                }
            }

            // If selected rows are not empty, we want to draw the lines again
            if (selectedRows.size() != 0) {
                if (column > 0) {
                    double x1 = domainAxis.getCategoryMiddle(column - 1, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double y1 = getNormalizedY(actualRow, column - 1, dataArea);

                    double x2 = domainAxis.getCategoryMiddle(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double y2 = getNormalizedY(actualRow, column, dataArea);

                    if (selectedRows.contains(actualRow)) {
                        g2.setPaint(Color.YELLOW);
                    } else {
                        // Don't draw
                        return;
                    }
                    g2.setStroke(new BasicStroke(1.0f)); // Keep line thickness constant
                    g2.draw(new Line2D.Double(x1, y1, x2, y2));
                }
            }

            if (actualRow == 0 && column == 0) {
                // Draw the parallel lines (axes) manually first
                for (int i = 0; i < dataset.getColumnCount(); i++) {
                    double x = domainAxis.getCategoryMiddle(i, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    g2.setPaint(Color.BLACK);
                    g2.setStroke(new BasicStroke(1.0f)); // Use 1.0f for a thin line
                    Line2D line = new Line2D.Double(x, dataArea.getMinY(), x, dataArea.getMaxY());
                    g2.draw(line);
                }
            }

            if (pureRegions != null) {
                for (PureRegion region : pureRegions) {
                    int startColIndex = findColumnIndex(region.attributeName, columnNames);

                    // Ensure the startColIndex is valid
                    if (startColIndex >= 0 && startColIndex < dataset.getColumnCount()) {
                        double xCenter = domainAxis.getCategoryMiddle(startColIndex, getColumnCount(), dataArea, plot.getDomainAxisEdge());

                        double startY = getNormalizedY(region.start, startColIndex, dataArea, columnMinValues, columnMaxValues);
                        double endY = getNormalizedY(region.end, startColIndex, dataArea, columnMinValues, columnMaxValues);

                        // Set the width of the rectangle to a fixed value of 0.1
                        double rectWidth = 0.1 * (dataArea.getWidth() / dataset.getColumnCount());

                        // Draw the rectangle with a fixed width
                        g2.setPaint(new Color(classColors.get(region.currentClass).getRed(), classColors.get(region.currentClass).getGreen(), classColors.get(region.currentClass).getBlue(), 100));
                        g2.fill(new Rectangle2D.Double(xCenter - rectWidth / 2, Math.min(startY, endY), rectWidth, Math.abs(startY - endY)));
                    }
                }
            }
        }

        private double getNormalizedY(int row, int column, Rectangle2D dataArea) {
            double value = dataset.getValue(row, column).doubleValue();
            double min = columnMinValues.get(column);
            double max = columnMaxValues.get(column);
            double normalizedValue = (value - min) / (max - min);
            return dataArea.getMaxY() - (normalizedValue * dataArea.getHeight());
        }

        private double getNormalizedY(double value, int column, Rectangle2D dataArea, Map<Integer, Double> minValues, Map<Integer, Double> maxValues) {
            double min = minValues.get(column);
            double max = maxValues.get(column);
            double normalizedValue = (value - min) / (max - min);
            return dataArea.getMaxY() - (normalizedValue * dataArea.getHeight());
        }

        private int findColumnIndex(String columnName, String[] columnNames) {
            for (int i = 0; i < columnNames.length; i++) {
                if (columnNames[i].equalsIgnoreCase(columnName)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public Paint getItemPaint(int row, int column) {
            String seriesKey = (String) getPlot().getDataset().getRowKey(row);
            String className = seriesKey.contains(" - ") ? seriesKey.split(" - ")[0] : seriesKey;
            return classColors.getOrDefault(className, Color.BLACK);
        }

        @Override
        public Shape getItemShape(int row, int column) {
            String seriesKey = (String) getPlot().getDataset().getRowKey(row);
            String className = seriesKey.contains(" - ") ? seriesKey.split(" - ")[0] : seriesKey;
            return classShapes.getOrDefault(className, new Ellipse2D.Double(-3, -3, 6, 6));
        }
    }
}
