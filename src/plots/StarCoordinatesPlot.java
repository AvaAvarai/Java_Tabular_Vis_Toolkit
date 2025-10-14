package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import src.utils.ScreenshotUtils;
import src.utils.LegendUtils;

public class StarCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private String datasetName;
    private Set<String> hiddenClasses; // Set to keep track of hidden classes
    private boolean showAttributeLabels = true; // Flag to toggle attribute labels
    private Color backgroundColor;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);

    public StarCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, String datasetName, Color backgroundColor) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.datasetName = datasetName;
        this.hiddenClasses = new HashSet<>(); // Initialize hiddenClasses set

        setTitle("Star Coordinates Plot");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set the layout and background color of the main content pane
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Add the plot panel at the center
        StarCoordinatesPanel plotPanel = new StarCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(600, 600));
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Replace the existing legend creation with the standardized version
        JPanel legendPanel = LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses);
        add(legendPanel, BorderLayout.SOUTH);

        // Add a button to toggle attribute labels
        JButton toggleLabelsButton = new JButton("Toggle Attribute Labels");
        toggleLabelsButton.addActionListener(e -> {
            showAttributeLabels = !showAttributeLabels;
            plotPanel.repaint();
        });
        add(toggleLabelsButton, BorderLayout.NORTH);

        // Add a key listener for the space bar to save a screenshot
        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "StarCoordinates", datasetName);
            }
        });

        // Ensure the JFrame is focusable to capture key events
        setFocusable(true);
        requestFocusInWindow();
    }

    private class StarCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20;

        public StarCoordinatesPanel() {
            setBackground(new Color(0xC0C0C0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Set the background color for the entire panel to white
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw the title above the grey background
            String title = "Star Coordinates Plot";
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
            g2.setColor(backgroundColor);
            g2.fillRect(0, plotAreaY, getWidth(), plotAreaHeight);

            int plotSize = Math.min(getWidth(), plotAreaHeight) - 2 * 50;
            int centerX = getWidth() / 2;
            int centerY = plotAreaY + plotAreaHeight / 2;

            // Number of attributes excluding the class column
            int numAttributes = attributeNames.size();
            double angleIncrement = 2 * Math.PI / numAttributes;

            Point2D.Double[] attributePositions = new Point2D.Double[numAttributes];

            // Calculate positions on the circumference for each attribute
            for (int i = 0; i < numAttributes; i++) {
                double angle = i * angleIncrement - Math.PI / 2;  // Start at the top (12 o'clock)
                int labelRadius = plotSize / 2 + 40; // Adjusted for better visibility
                double x = centerX + labelRadius * Math.cos(angle);
                double y = centerY + labelRadius * Math.sin(angle);
                attributePositions[i] = new Point2D.Double(x, y);

                // Draw attribute labels if showAttributeLabels is true
                if (showAttributeLabels) {
                    g2.setFont(AXIS_LABEL_FONT); // Use the defined font for axis labels
                    g2.setColor(Color.BLACK); // Set label color to black
                    drawCenteredString(g2, attributeNames.get(i), (int) x, (int) y);
                }
            }

            // Draw the star coordinates for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row) && !hiddenClasses.contains(classLabels.get(row))) {
                    drawStar(g2, row, centerX, centerY, plotSize / 2, angleIncrement, false);
                }
            }

            // Highlight selected rows
            for (int row = 0; row < data.get(0).size(); row++) {
                if (selectedRows.contains(row) && !hiddenClasses.contains(classLabels.get(row))) {
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

        private void drawCenteredString(Graphics2D g2, String text, int x, int y) {
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            int width = metrics.stringWidth(text);
            int height = metrics.getHeight();
            g2.drawString(text, x - width / 2, y + height / 4); // Slight adjustment for better centering
        }
    }
}
