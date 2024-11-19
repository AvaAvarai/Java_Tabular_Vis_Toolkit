package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import src.utils.ScreenshotUtils;

public class CircularCoordinatesPlot extends JFrame {
    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private int numAttributes;
    private String datasetName;
    private int curveHeight = 50; // Default curve height
    private Set<String> hiddenClasses = new HashSet<>(); // Track hidden classes
    private boolean showTicks = false; // Track if ticks should be shown
    private boolean showLabels = true; // Track if labels should be shown
    private boolean usePolygon = false; // Track if polygon should be used instead of circle
    private boolean dynamicMode = false; // Track if dynamic mode should be used
    private Map<String, Double> maxSumPerClass = new HashMap<>(); // Track max sum of each class

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);

    public CircularCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, String datasetName) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.numAttributes = attributeNames.size();
        this.datasetName = datasetName;

        setTitle("Circular/Polygonal Coordinates");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        calculateMaxSumPerClass();

        // Set the layout and background color of the main content pane
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Add the plot panel at the center
        CircularCoordinatesPanel plotPanel = new CircularCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(600, 600));
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Create a control panel for curve height and tick toggle
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add curve height control
        JPanel curvePanel = new JPanel(new BorderLayout());
        curvePanel.setBorder(BorderFactory.createTitledBorder("Curve Height"));
        JSlider curveSlider = new JSlider(-100, 100, 50);
        curveSlider.addChangeListener(e -> {
            curveHeight = curveSlider.getValue();
            repaint();
        });
        curvePanel.add(curveSlider);
        controlPanel.add(curvePanel);

        // Add tick marks toggle
        JToggleButton tickToggle = new JToggleButton("Show Tick Marks");
        tickToggle.addActionListener(e -> {
            showTicks = tickToggle.isSelected();
            repaint();
        });
        controlPanel.add(tickToggle);

        // Add label visibility toggle
        JToggleButton labelToggle = new JToggleButton("Show Labels", true);
        labelToggle.addActionListener(e -> {
            showLabels = labelToggle.isSelected();
            repaint();
        });
        controlPanel.add(labelToggle);

        // Add polygon toggle
        JToggleButton polygonToggle = new JToggleButton("Use Polygon");
        polygonToggle.addActionListener(e -> {
            usePolygon = polygonToggle.isSelected();
            repaint();
        });
        controlPanel.add(polygonToggle);

        // Add dynamic mode toggle
        JToggleButton dynamicModeToggle = new JToggleButton("Dynamic Mode");
        dynamicModeToggle.addActionListener(e -> {
            dynamicMode = dynamicModeToggle.isSelected();
            repaint();
        });
        controlPanel.add(dynamicModeToggle);

        add(controlPanel, BorderLayout.NORTH);

        // Add a legend panel at the bottom (horizontal)
        add(createLegendPanel(), BorderLayout.SOUTH);

        // Add a key listener for the space bar to save a screenshot
        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "StaticCircularCoordinates", datasetName);
            }
        });

        // Ensure the JFrame is focusable to capture key events
        setFocusable(true);
        requestFocusInWindow();
    }

    private void calculateMaxSumPerClass() {
        maxSumPerClass.clear(); // Clear previous calculations
    
        for (int row = 0; row < classLabels.size(); row++) {
            String classLabel = classLabels.get(row);
            double sum = 0;
    
            // Calculate the sum of the row values
            for (int col = 0; col < numAttributes; col++) {
                sum += data.get(col).get(row);
            }
    
            // Update the maximum sum for the class
            maxSumPerClass.put(classLabel, Math.max(maxSumPerClass.getOrDefault(classLabel, 0.0), sum));
        }
    }    

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

        // Add each class color and shape to the legend
        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE);
            colorLabelPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(hiddenClasses.contains(className) ? Color.LIGHT_GRAY : color);
                    g2.translate(32, 20);
                    g2.scale(2, 2);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(40, 40));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

            colorLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hiddenClasses.contains(className)) {
                        hiddenClasses.remove(className);
                    } else {
                        hiddenClasses.add(className);
                    }
                    repaint();
                }
            });

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);

            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class CircularCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20; // Add 20px padding between title and plot

        public CircularCoordinatesPanel() {
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
            String title = "Circular/Polygon Coordinates Plot";
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
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, plotAreaY, getWidth(), plotAreaHeight);

            int radius = Math.min(getWidth() / 2, plotAreaHeight / 2) - 50;
            int centerX = getWidth() / 2;
            int centerY = plotAreaY + plotAreaHeight / 2;
            double angleStep = 2 * Math.PI / numAttributes;

            Point2D.Double[] attributePositions = new Point2D.Double[numAttributes];
            int[] xPoints = new int[numAttributes];
            int[] yPoints = new int[numAttributes];

            // Draw the circular axis or polygon
            g2.setColor(Color.BLACK);
            for (int i = 0; i < numAttributes; i++) {
                double angle = i * angleStep - Math.PI / 2;
                xPoints[i] = (int)(centerX + radius * Math.cos(angle));
                yPoints[i] = (int)(centerY + radius * Math.sin(angle));
            }
            
            if (usePolygon) {
                g2.drawPolygon(xPoints, yPoints, numAttributes);
            } else {
                g2.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));
            }

            // Calculate positions on the circumference for each attribute
            for (int i = 0; i < numAttributes; i++) {
                double angle = i * angleStep - Math.PI / 2;  // Start at the top (12 o'clock)
                int labelradius1 = radius + 15; 
                int labelradius2 = radius + 20;
                if (i > numAttributes / 2) {
                    labelradius1 = radius + 95;
                    labelradius2 = radius + 30;
                }
                double x = centerX + labelradius1 * Math.cos(angle);
                double y = centerY + labelradius2 * Math.sin(angle);
                attributePositions[i] = new Point2D.Double(x, y);

                // Draw attribute labels if enabled
                if (showLabels) {
                    g2.setFont(AXIS_LABEL_FONT); // Use the defined font for axis labels
                    g2.drawString(attributeNames.get(i), (int) x, (int) y);
                }

                // Draw tick marks if enabled
                if (showTicks) {
                    double tickLength = 20;
                    if (!dynamicMode) {
                        double tickStartX = centerX + (radius - tickLength/2) * Math.cos(angle);
                        double tickStartY = centerY + (radius - tickLength/2) * Math.sin(angle);
                        double tickEndX = centerX + (radius + tickLength/2) * Math.cos(angle);
                        double tickEndY = centerY + (radius + tickLength/2) * Math.sin(angle);
                        g2.drawLine((int)tickStartX, (int)tickStartY, (int)tickEndX, (int)tickEndY);
                    } else {
                        for (Map.Entry<String, Double> entry : maxSumPerClass.entrySet()) {
                            String classLabel = entry.getKey();
                            double maxSum = entry.getValue();
                            double angleOffset = (maxSum % numAttributes) * angleStep - Math.PI / 2; // Wrap around using modulo

                            // Calculate tick mark position
                            double tickX = centerX + (radius + 10) * Math.cos(angleOffset);
                            double tickY = centerY + (radius + 10) * Math.sin(angleOffset);

                            // Draw the tick mark
                            g2.drawLine(
                                (int) (centerX + radius * Math.cos(angleOffset)),
                                (int) (centerY + radius * Math.sin(angleOffset)),
                                (int) tickX,
                                (int) tickY
                            );
                        }
                    }
                }
            }

            // Draw non-selected rows first
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row) && !hiddenClasses.contains(classLabels.get(row))) {
                    drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, xPoints, yPoints, false);
                }
            }

            // Draw selected rows last (highlighted in yellow)
            for (int row : selectedRows) {
                if (!hiddenClasses.contains(classLabels.get(row))) {
                    // Draw highlight twice as thick by drawing three times
                    drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, xPoints, yPoints, true);
                    drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, xPoints, yPoints, true);
                    drawRow(g2, row, attributePositions, centerX, centerY, radius, angleStep, xPoints, yPoints, true);
                }
            }
        }

        private void drawRow(Graphics2D g2, int row, Point2D.Double[] attributePositions, int centerX, int centerY, int radius, double angleStep, int[] xPoints, int[] yPoints, boolean isSelected) {
            Point2D.Double[] points = new Point2D.Double[numAttributes];
            String classLabel = classLabels.get(row);
            Color color = isSelected ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
        
            // Calculate points
            for (int i = 0; i < numAttributes; i++) {
                double value = data.get(i).get(row);
                if (!dynamicMode) {
                    // Position value within the range [0, 1] for non-dynamic mode
                    double minValue = Collections.min(data.get(i));
                    double maxValue = Collections.max(data.get(i));
                    value = (value - minValue) / (maxValue - minValue);
                    if (usePolygon) {
                        // For polygon mode, interpolate along the straight line between vertices
                        int nextIndex = (i + 1) % numAttributes;
                        double x = xPoints[i] + value * (xPoints[nextIndex] - xPoints[i]);
                        double y = yPoints[i] + value * (yPoints[nextIndex] - yPoints[i]);
                        points[i] = new Point2D.Double(x, y);
                    } else {
                        // For circle mode, use angle-based positioning
                        double angleOffset = value * angleStep;
                        double x = centerX + radius * Math.cos(i * angleStep + angleOffset - Math.PI / 2);
                        double y = centerY + radius * Math.sin(i * angleStep + angleOffset - Math.PI / 2);
                        points[i] = new Point2D.Double(x, y);
                    }
                }
                if (dynamicMode) {
                    if (i > 0 && row > 0) {
                        // sum all previous values with the current value
                        for (int j = 0; j < i; j++) {
                            value += data.get(j).get(row - 1);
                        }
                    }
                    
                    if (usePolygon) {
                        double cumulativeValue = 0;
                        for (int j = 0; j <= i; j++) {
                            cumulativeValue += data.get(j).get(row); // Sum all previous values including current
                        }
                        // For polygon mode, interpolate along the straight line between vertices
                        int currentIndex = i % numAttributes;
                        int nextIndex = (i + 1) % numAttributes; // Wrap around to first vertex
                        double edgeFraction = Math.min(Math.max(cumulativeValue, 0), 1); // Normalize to [0, 1]

                        double x = xPoints[currentIndex] + edgeFraction * (xPoints[nextIndex] - xPoints[currentIndex]);
                        double y = yPoints[currentIndex] + edgeFraction * (yPoints[nextIndex] - yPoints[currentIndex]);
                        points[i] = new Point2D.Double(x, y);
                    } else {
                        // For circle mode, use angle-based positioning
                        double angleOffset = value * angleStep;
                        double x = centerX + radius * Math.cos(angleOffset - Math.PI / 2);
                        double y = centerY + radius * Math.sin(angleOffset - Math.PI / 2);
                        points[i] = new Point2D.Double(x, y);
                    }
                }
            }
        
            // Connect points with Bezier curves
            g2.setColor(color);
            for (int i = 0; i < numAttributes - 1; i++) {
                Point2D.Double p1 = points[i];
                Point2D.Double p2 = points[i + 1];
                
                // Calculate control points towards the center
                double ctrlX1 = centerX + (p1.x - centerX) * (1 - curveHeight/100.0);
                double ctrlY1 = centerY + (p1.y - centerY) * (1 - curveHeight/100.0);
                double ctrlX2 = centerX + (p2.x - centerX) * (1 - curveHeight/100.0);
                double ctrlY2 = centerY + (p2.y - centerY) * (1 - curveHeight/100.0);
                
                CubicCurve2D curve = new CubicCurve2D.Double(
                    p1.x, p1.y,
                    ctrlX1, ctrlY1,
                    ctrlX2, ctrlY2,
                    p2.x, p2.y
                );
                g2.draw(curve);
            }
            
            // Connect last point to first point
            if (!dynamicMode) {
                Point2D.Double p1 = points[numAttributes - 1];
                Point2D.Double p2 = points[0];
                double ctrlX1 = centerX + (p1.x - centerX) * (1 - curveHeight/100.0);
                double ctrlY1 = centerY + (p1.y - centerY) * (1 - curveHeight/100.0);
                double ctrlX2 = centerX + (p2.x - centerX) * (1 - curveHeight/100.0);
                double ctrlY2 = centerY + (p2.y - centerY) * (1 - curveHeight/100.0);
                
                CubicCurve2D curve = new CubicCurve2D.Double(
                    p1.x, p1.y,
                    ctrlX1, ctrlY1,
                    ctrlX2, ctrlY2,
                    p2.x, p2.y
                );
                g2.draw(curve);
            }
            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);  // Move the origin to the point location
                g2.fill(shape);       // Draw the shape at the translated origin
                g2.translate(-points[i].x, -points[i].y); // Move back the origin
            }
        }        
    }
}
