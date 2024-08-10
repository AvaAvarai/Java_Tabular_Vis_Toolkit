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

    public ParallelCoordinatesPlot(List<String[]> data, String[] columnNames, Map<String, Color> classColors, int classColumnIndex, int[] columnOrder, List<Integer> selectedRows) {
        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        this.classColors = classColors;
        this.classShapes = createClassShapes();

        DefaultCategoryDataset dataset = createDataset(data, columnNames, classColumnIndex, columnOrder);
        JFreeChart chart = createChart(dataset, columnNames, selectedRows);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer() {

            // Drawing highlights last
            private List<Integer> drawOrder;

            @Override
            public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
                // Skip rendering for the first pass to handle ordering manually
                if (pass == 0) {
                    return;
                }

                if (drawOrder == null) {
                    drawOrder = new ArrayList<>();
                    for (int i = 0; i < dataset.getRowCount(); i++) {
                        if (!selectedRows.contains(i)) {
                            drawOrder.add(i);
                        }
                    }
                    drawOrder.addAll(selectedRows);
                }

                int actualRow = drawOrder.get(row);

                super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, actualRow, column, pass);
                if (column > 0) {
                    double x1 = domainAxis.getCategoryMiddle(column - 1, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double y1 = rangeAxis.valueToJava2D(dataset.getValue(actualRow, column - 1).doubleValue(), dataArea, plot.getRangeAxisEdge());

                    double x2 = domainAxis.getCategoryMiddle(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double y2 = rangeAxis.valueToJava2D(dataset.getValue(actualRow, column).doubleValue(), dataArea, plot.getRangeAxisEdge());

                    if (selectedRows.contains(actualRow)) {
                        g2.setPaint(Color.YELLOW);
                    } else {
                        g2.setPaint(getItemPaint(actualRow, column));
                    }
                    g2.draw(new Line2D.Double(x1, y1, x2, y2));
                }
                if (actualRow == 0 && column == 0) {
                    // Draw the parallel lines (axes) manually
                    for (int i = 0; i < dataset.getColumnCount(); i++) {
                        double x = domainAxis.getCategoryMiddle(i, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                        g2.setPaint(Color.BLACK);
                        g2.setStroke(new BasicStroke(1.0f)); // Use 1.0f for a thin line
                        Line2D line = new Line2D.Double(x, dataArea.getMinY(), x, dataArea.getMaxY());
                        g2.draw(line);
                    }
                }
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
        };
        renderer.setDefaultShapesVisible(true);
        plot.setRenderer(renderer);

        // Manually create a custom legend
        LegendItemCollection legendItems = new LegendItemCollection();
        Set<String> addedClasses = new HashSet<>();

        for (String className : classColors.keySet()) {
            if (!addedClasses.contains(className)) {
                Shape shape = classShapes.get(className);
                Paint paint = classColors.get(className);
                LegendItem item = new LegendItem(className, "", "", "", shape, paint, new BasicStroke(), paint);
                legendItems.add(item);
                addedClasses.add(className);
            }
        }

        plot.setFixedLegendItems(legendItems);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private Map<String, Shape> createClassShapes() {
        Map<String, Shape> shapes = new HashMap<>();
        Shape[] availableShapes = {
            new Ellipse2D.Double(-3, -3, 6, 6),
            new Rectangle2D.Double(-3, -3, 6, 6),
            new Polygon(new int[]{-3, 3, 0}, new int[]{-3, -3, 3}, 3)
        };

        int i = 0;
        for (String className : classColors.keySet()) {
            shapes.put(className, availableShapes[i % availableShapes.length]);
            i++;
        }

        return shapes;
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

    private JFreeChart createChart(DefaultCategoryDataset dataset, String[] columnNames, List<Integer> selectedRows) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Parallel Coordinates Plot", // chart title
                "Attribute", // domain axis label
                "Value", // range axis label
                dataset);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDomainAxis(new CategoryAxis());
        plot.setRangeAxis(new NumberAxis());

        return chart;
    }
}
