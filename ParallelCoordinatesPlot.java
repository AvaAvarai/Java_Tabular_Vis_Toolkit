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
import java.util.List;

public class ParallelCoordinatesPlot extends JFrame {

    public ParallelCoordinatesPlot(List<String[]> data, String[] columnNames) {
        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        DefaultCategoryDataset dataset = createDataset(data, columnNames);
        JFreeChart chart = createChart(dataset, columnNames);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private DefaultCategoryDataset createDataset(List<String[]> data, String[] columnNames) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i = 0; i < data.size(); i++) {
            String[] row = data.get(i);
            for (int j = 0; j < row.length; j++) {
                try {
                    double value = Double.parseDouble(row[j]);
                    dataset.addValue(value, "Row " + i, columnNames[j]);
                } catch (NumberFormatException e) {
                    // Handle non-numeric data
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

        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        plot.setRenderer(renderer);

        return chart;
    }
}
