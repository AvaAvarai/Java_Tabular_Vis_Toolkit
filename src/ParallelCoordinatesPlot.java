package src;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParallelCoordinatesPlot extends JFrame {

    private Map<String, Color> classColors;

    public ParallelCoordinatesPlot(List<String[]> data, String[] columnNames, Map<String, Color> classColors, int classColumnIndex) {
        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        this.classColors = classColors;
        DefaultCategoryDataset dataset = createDataset(data, columnNames, classColumnIndex);
        JFreeChart chart = createChart(dataset, columnNames);

        // Manually create a custom legend
        LegendItemCollection legendItems = new LegendItemCollection();
        Set<String> addedClasses = new HashSet<>();

        for (String className : classColors.keySet()) {
            if (!addedClasses.contains(className)) {
                LegendItem item = new LegendItem(className, classColors.get(className));
                legendItems.add(item);
                addedClasses.add(className);
            }
        }

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setFixedLegendItems(legendItems);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private DefaultCategoryDataset createDataset(List<String[]> data, String[] columnNames, int classColumnIndex) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            String className = row[classColumnIndex];
            String seriesName = className + " - Row " + i;

            for (int j = 0; j < row.length; j++) {
                if (j != classColumnIndex) {
                    try {
                        double value = Double.parseDouble(row[j]);
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
                "Value", // range axis label
                dataset);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDomainAxis(new CategoryAxis());
        plot.setRangeAxis(new NumberAxis());

        LineAndShapeRenderer renderer = new LineAndShapeRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                String seriesKey = (String) getPlot().getDataset().getRowKey(row);
                String className = seriesKey.contains(" - ") ? seriesKey.split(" - ")[0] : seriesKey;
                return classColors.getOrDefault(className, Color.BLACK);
            }
        };
        plot.setRenderer(renderer);

        return chart;
    }
}
