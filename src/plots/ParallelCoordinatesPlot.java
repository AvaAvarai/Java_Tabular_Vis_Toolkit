package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

public class ParallelCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private int numAttributes;
    private double globalMaxValue;
    private double globalMinValue;
    private double axisMinValue; // New variable to store the minimum value for axes

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);

    // Spacing settings
    private static final int TITLE_PADDING = 20;
    private static final int AXIS_LABEL_PADDING = 30; // Space below plot for labels

    public ParallelCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, String datasetName) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.numAttributes = attributeNames.size();

        // Calculate global max and min values
        this.globalMaxValue = data.stream()
            .flatMap(List::stream)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(1.0);
        this.globalMinValue = data.stream()
            .flatMap(List::stream)
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);

        // Set the axis minimum value
        this.axisMinValue = (globalMinValue < 0) ? globalMinValue : 0;

        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        ParallelCoordinatesPanel plotPanel = new ParallelCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(numAttributes * 150, 600 + AXIS_LABEL_PADDING));

        // Add the plot panel to a scroll pane
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Minimize space around the plot

        // Add a key listener for the space bar to save a screenshot
        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ParallelCoordinates", datasetName);
            }
        });

        // Ensure the JFrame is focusable to capture key events
        setFocusable(true);
        requestFocusInWindow();

        // Add the scroll pane and legend to the main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
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

    private class ParallelCoordinatesPanel extends JPanel {
        public ParallelCoordinatesPanel() {
            setBackground(new Color(0xC0C0C0));
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
            String title = "Parallel Coordinates Plot";
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics(TITLE_FONT);
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            // Calculate plot area dimensions
            int plotAreaY = titleHeight + TITLE_PADDING;
            int plotAreaHeight = getHeight() - plotAreaY - AXIS_LABEL_PADDING;

            // Set the background color for the plot area
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, plotAreaY, getWidth(), plotAreaHeight);

            // Calculate axis spacing and centering
            int margin = 50;
            int axisSpacing = (getWidth() - 2 * margin) / (numAttributes - 1);

            // Draw axis lines
            drawAxisLines(g2, axisSpacing, margin, plotAreaY, plotAreaHeight);

            // Draw the parallel coordinates for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row)) {
                    drawParallelCoordinates(g2, row, axisSpacing, margin, plotAreaY, plotAreaHeight);
                }
            }

            // Highlight selected rows
            for (int row = 0; row < data.get(0).size(); row++) {
                if (selectedRows.contains(row)) {
                    drawParallelCoordinates(g2, row, axisSpacing, margin, plotAreaY, plotAreaHeight);
                }
            }

            // Draw attribute labels
            drawAttributeLabels(g2, axisSpacing, margin, plotAreaY + plotAreaHeight + AXIS_LABEL_PADDING);
        }

        private void drawAxisLines(Graphics2D g2, int axisSpacing, int margin, int plotAreaY, int plotAreaHeight) {
            g2.setColor(Color.BLACK);
            for (int i = 0; i < numAttributes; i++) {
                int x = margin + i * axisSpacing;
                g2.draw(new Line2D.Double(x, plotAreaY, x, plotAreaY + plotAreaHeight));
            }
        }

        private void drawParallelCoordinates(Graphics2D g2, int row, int axisSpacing, int margin, int plotAreaY, int plotAreaHeight) {
            Point2D.Double[] points = new Point2D.Double[numAttributes];

            for (int i = 0; i < numAttributes; i++) {
                double value = data.get(i).get(row);
                double normalizedValue = (value - axisMinValue) / (globalMaxValue - axisMinValue);

                double x = margin + i * axisSpacing;
                double y = plotAreaHeight - (normalizedValue * plotAreaHeight) + plotAreaY;

                points[i] = new Point2D.Double(x, y);
            }

            String classLabel = classLabels.get(row);
            Color color = selectedRows.contains(row) ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);

            for (int i = 0; i < numAttributes - 1; i++) {
                g2.draw(new Line2D.Double(points[i], points[i + 1]));
            }

            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);  // Move the origin to the point location
                g2.fill(classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6))); // Draw the shape
                g2.translate(-points[i].x, -points[i].y); // Move back the origin
            }
        }

        private void drawAttributeLabels(Graphics2D g2, int axisSpacing, int margin, int labelY) {
            g2.setFont(AXIS_LABEL_FONT);
            g2.setColor(Color.BLACK);

            for (int i = 0; i < numAttributes; i++) {
                int x = margin + i * axisSpacing;
                g2.drawString(attributeNames.get(i), x - g2.getFontMetrics().stringWidth(attributeNames.get(i)) / 2, labelY);
            }
        }
    }
}
