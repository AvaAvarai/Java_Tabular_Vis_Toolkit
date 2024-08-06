package src;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParallelCoordinatesPlot extends JFrame {

    private Map<String, Color> classColors;

    public ParallelCoordinatesPlot(List<String[]> data, String[] columnNames, int classColumnIndex) {
        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        classColors = assignColorsToClasses(data, classColumnIndex);
        DefaultCategoryDataset dataset = createDataset(data, columnNames, classColumnIndex);
        JFreeChart chart = createChart(dataset, columnNames);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private DefaultCategoryDataset createDataset(List<String[]> data, String[] columnNames, int classColumnIndex) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            String className = row[classColumnIndex];
            for (int j = 0; j < row.length; j++) {
                if (j != classColumnIndex) {
                    try {
                        double value = Double.parseDouble(row[j]);
                        dataset.addValue(value, className + " - Row " + i, columnNames[j]);
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
                String className = seriesKey.split(" - ")[0];
                return classColors.get(className);
            }
        };
        plot.setRenderer(renderer);

        return chart;
    }

    private Map<String, Color> assignColorsToClasses(List<String[]> data, int classColumnIndex) {
        Map<String, Color> colorMap = new HashMap<>();
        String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#000000"};

        int colorIndex = 0;
        for (String[] row : data) {
            String className = row[classColumnIndex];
            if (!colorMap.containsKey(className)) {
                colorMap.put(className, Color.decode(colors[colorIndex % colors.length]));
                colorIndex++;
            }
        }

        return colorMap;
    }
}
