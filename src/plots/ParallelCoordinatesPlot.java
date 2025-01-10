package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import src.utils.LegendUtils;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParallelCoordinatesPlot extends JFrame {

    private final List<List<Double>> data;
    private final List<String> attributeNames;
    private final Map<String, Color> classColors;
    private final Map<String, Shape> classShapes;
    private final List<String> classLabels;
    private final List<Integer> selectedRows;
    private Set<String> hiddenClasses;
    private final Map<String, Boolean> axisDirections;
    private final Map<String, Point2D.Double> axisPositions;
    private final Map<String, Double> axisScales;
    private String draggedAxis = null;
    private final double globalMaxValue;
    private final double globalMinValue;
    private boolean showAttributeLabels = true;
    private boolean showDensity = true;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int AXIS_HEIGHT = 400;

    public ParallelCoordinatesPlot(List<List<Double>> data, List<String> attributeNames,
                                   Map<String, Color> classColors, Map<String, Shape> classShapes,
                                   List<String> classLabels, List<Integer> selectedRows, String datasetName) {
        this.data = data;
        this.attributeNames = new ArrayList<>(attributeNames);
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.hiddenClasses = new HashSet<>();
        this.axisDirections = new HashMap<>();
        this.axisPositions = new HashMap<>();
        this.axisScales = new HashMap<>();

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

        // Initialize axis positions
        int startX = 100;
        int spacing = 150;
        for (int i = 0; i < attributeNames.size(); i++) {
            axisPositions.put(attributeNames.get(i), new Point2D.Double(startX + i * spacing, 100));
            axisScales.put(attributeNames.get(i), 1.0);
        }

        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        ParallelCoordinatesPanel plotPanel = new ParallelCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(attributeNames.size() * 150, 600));

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        JPanel controlPanel = createControlPanel();

        // Add a button to take a screenshot
        JButton screenshotButton = new JButton("Take Screenshot");
        screenshotButton.addActionListener(_ -> {
            ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ParallelCoordinates", datasetName);
        });

        controlPanel.add(screenshotButton);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createControlPanel() {
        // Create a panel to hold the controls for each attribute
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.setBackground(new Color(0xC0C0C0)); // Set background color to c0c0c0
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add a button to toggle the attribute labels
        JToggleButton attributeLabelToggle = new JToggleButton("Show Labels");
        attributeLabelToggle.addActionListener(_ -> {
            showAttributeLabels = attributeLabelToggle.isSelected();
            repaint();
        });

        // Add a button to toggle the density
        JToggleButton densityToggle = new JToggleButton("Toggle Density");
        densityToggle.addActionListener(_ -> {
            showDensity = densityToggle.isSelected();
            repaint();
        });
        densityToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(densityToggle);

        attributeLabelToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(attributeLabelToggle);

        for (String attributeName : attributeNames) {
            // Create a panel for each attribute
            JPanel attributePanel = new JPanel();
            attributePanel.setLayout(new BoxLayout(attributePanel, BoxLayout.Y_AXIS));
            attributePanel.setBorder(BorderFactory.createTitledBorder(attributeName));
            attributePanel.setBackground(new Color(0xF0F0F0)); // Light gray background for attribute panel
            attributePanel.setMaximumSize(new Dimension(150, 100));
    
            // Add a direction toggle button
            JToggleButton axisDirectionToggle = new JToggleButton("\u2B05");
            axisDirectionToggle.setBackground(Color.WHITE);
            axisDirectionToggle.setFocusPainted(false);
            axisDirectionToggle.addActionListener(e -> {
                boolean isToggled = axisDirectionToggle.isSelected();
                axisDirections.put(attributeName, isToggled);
                axisDirectionToggle.setText(isToggled ? "\u27A1" : "\u2B05");
                repaint();
            });
            axisDirectionToggle.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align
            attributePanel.add(axisDirectionToggle);
    
            // Add a scale slider
            JSlider axisScaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
            axisScaleSlider.setBackground(Color.WHITE);
            axisScaleSlider.setPreferredSize(new Dimension(100, 20));
            axisScaleSlider.addChangeListener(e -> {
                int value = axisScaleSlider.getValue();
                axisScales.put(attributeName, value / 100.0);
                // Update the axis size when the scale slider is changed
                axisPositions.get(attributeName).y = 100 + (1 - axisScales.get(attributeName)) * AXIS_HEIGHT;
                repaint();
            });
            JLabel sliderLabel = new JLabel("Scale");
            sliderLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align
            attributePanel.add(sliderLabel);
            attributePanel.add(axisScaleSlider);
    
            // Add the attribute panel to the main control panel
            controlPanel.add(attributePanel);
        }
    
        return controlPanel;
    }

    private class ParallelCoordinatesPanel extends JPanel {
        private final List<String> visualOrder; // Tracks the visual order of the axes
    
        public ParallelCoordinatesPanel() {
            setBackground(new Color(0xC0C0C0));
    
            // Initialize the visual order of the axes (matches attributeNames initially)
            visualOrder = new ArrayList<>(attributeNames);
    
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (String attributeName : visualOrder) {
                        Point2D.Double pos = axisPositions.get(attributeName);
                        if (Math.abs(e.getX() - pos.x) < 10) {
                            draggedAxis = attributeName;
                            break;
                        }
                    }
                }
    
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedAxis != null) {
                        // Reorder the visual order based on the updated X positions
                        visualOrder.sort(Comparator.comparingDouble(attr -> axisPositions.get(attr).x));
                    }
                    draggedAxis = null;
                    repaint();
                }
            });
    
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedAxis != null) {
                        Point2D.Double pos = axisPositions.get(draggedAxis);
                        pos.x = e.getX();
                        repaint();
                    }
                }
            });
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            // Draw the title on white background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), 50); // Assuming 50 as the height for the title
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth("Parallel Coordinates Plot");
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK); // Set text color to black
            g2.drawString("Parallel Coordinates Plot", (getWidth() - titleWidth) / 2, titleHeight);
            
            // Draw the plot on c0c0c0 background
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, 50, getWidth(), getHeight() - 50); // Adjusted to start from below the title
            drawAxes(g2);
            drawData(g2);
        }
        
        private void drawAxes(Graphics2D g2) {
            g2.setColor(Color.BLACK);
    
            for (String attributeName : visualOrder) { // Use visualOrder to draw the axes
                Point2D.Double pos = axisPositions.get(attributeName);
                double scale = axisScales.getOrDefault(attributeName, 1.0); // Get the current scale for this axis
                int scaledHeight = (int) (AXIS_HEIGHT * scale); // Scale the height of the axis
    
                g2.drawLine((int) pos.x, (int) pos.y, (int) pos.x, (int) pos.y + scaledHeight);
    
                // Draw attribute label
                if (showAttributeLabels) {
                    g2.setFont(AXIS_LABEL_FONT);
                    String label = attributeName;
                    int labelWidth = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, (int) (pos.x - labelWidth / 2), (int) (pos.y + scaledHeight + 20));
                }
            }
        }
        
        private void drawData(Graphics2D g2) {
            // Create a map to store line segments and their counts
            Map<LineSegment, Integer> lineSegmentCounts = new HashMap<>();
            
            // First pass: Count overlapping line segments
            countLineSegments(lineSegmentCounts, false); // non-selected lines
            countLineSegments(lineSegmentCounts, true);  // selected lines

            // Find maximum density for normalization
            int maxDensity = lineSegmentCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(1);

            // Second pass: Draw lines with appropriate thickness
            if (!showDensity) {
                drawLines(g2, false);
                drawLines(g2, true);
            } else {
                drawLinesWithDensity(g2, lineSegmentCounts, maxDensity, false);
                drawLinesWithDensity(g2, lineSegmentCounts, maxDensity, true);
            }
        }

        // Add this class to store line segments
        private static class LineSegment {
            final int x1, y1, x2, y2;
            
            LineSegment(Point2D.Double p1, Point2D.Double p2) {
                // Round coordinates to reduce floating point precision issues
                this.x1 = (int) Math.round(p1.x);
                this.y1 = (int) Math.round(p1.y);
                this.x2 = (int) Math.round(p2.x);
                this.y2 = (int) Math.round(p2.y);
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof LineSegment)) return false;
                LineSegment that = (LineSegment) o;
                return x1 == that.x1 && y1 == that.y1 && x2 == that.x2 && y2 == that.y2;
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(x1, y1, x2, y2);
            }
        }

        private void countLineSegments(Map<LineSegment, Integer> lineSegmentCounts, boolean selectedOnly) {
            List<Integer> rowsToProcess = selectedOnly ? selectedRows : 
                IntStream.range(0, data.get(0).size())
                        .filter(i -> !selectedRows.contains(i))
                        .boxed()
                        .collect(Collectors.toList());

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPointsForRow(row);
                
                // Count line segments
                for (int i = 0; i < points.size() - 1; i++) {
                    LineSegment segment = new LineSegment(points.get(i), points.get(i + 1));
                    lineSegmentCounts.merge(segment, 1, Integer::sum);
                }
            }
        }

        private void drawLines(Graphics2D g2, boolean selectedOnly) {
            List<Integer> rowsToProcess = selectedOnly ? selectedRows : 
                IntStream.range(0, data.get(0).size())
                        .filter(i -> !selectedRows.contains(i))
                        .boxed()
                        .collect(Collectors.toList());

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPointsForRow(row);
                Color baseColor = selectedOnly ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                
                // Draw lines with a fixed thickness
                for (int i = 0; i < points.size() - 1; i++) {
                    Point2D.Double p1 = points.get(i);
                    Point2D.Double p2 = points.get(i + 1);
                    
                    g2.setStroke(new BasicStroke(selectedOnly ? 2.0f : 1.0f));
                    g2.setColor(baseColor);
                    g2.draw(new Line2D.Double(p1, p2));
                }

                // Draw class symbols as vertices
                g2.setColor(baseColor);
                for (Point2D.Double point : points) {
                    Shape shape = classShapes.get(classLabel);
                    g2.translate(point.x, point.y);
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }

        private void drawLinesWithDensity(Graphics2D g2, Map<LineSegment, Integer> lineSegmentCounts, int maxDensity, boolean selectedOnly) {
            List<Integer> rowsToProcess = selectedOnly ? selectedRows : 
                IntStream.range(0, data.get(0).size())
                        .filter(i -> !selectedRows.contains(i))
                        .boxed()
                        .collect(Collectors.toList());

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPointsForRow(row);
                Color baseColor = selectedOnly ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                
                // Draw lines with varying thickness based on density
                for (int i = 0; i < points.size() - 1; i++) {
                    Point2D.Double p1 = points.get(i);
                    Point2D.Double p2 = points.get(i + 1);
                    
                    LineSegment segment = new LineSegment(p1, p2);
                    int count = lineSegmentCounts.getOrDefault(segment, 1);
                    
                    // Calculate thickness based on density
                    float normalizedDensity = (float) count / maxDensity;
                    float thickness = 1.0f + (normalizedDensity * 5.0f); // Scale from 1 to 6 pixels
                    
                    // Adjust color alpha based on density
                    Color adjustedColor = new Color(
                        baseColor.getRed(),
                        baseColor.getGreen(),
                        baseColor.getBlue(),
                        Math.min(255, 50 + (int)(205 * normalizedDensity)) // Alpha from 50 to 255
                    );
                    
                    g2.setStroke(new BasicStroke(selectedOnly ? thickness + 1.0f : thickness));
                    g2.setColor(adjustedColor);
                    g2.draw(new Line2D.Double(p1, p2));
                }

                // Draw class symbols as vertices
                g2.setColor(baseColor);
                for (Point2D.Double point : points) {
                    Shape shape = classShapes.get(classLabel);
                    g2.translate(point.x, point.y);
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }

        private List<Point2D.Double> getPointsForRow(int row) {
            List<Point2D.Double> points = new ArrayList<>();
            
            for (String attributeName : visualOrder) {
                int attributeIndex = attributeNames.indexOf(attributeName);
                double value = data.get(attributeIndex).get(row);
                double normalizedValue = (value - globalMinValue) / (globalMaxValue - globalMinValue);

                if (axisDirections.getOrDefault(attributeName, false)) {
                    normalizedValue = 1 - normalizedValue;
                }

                Point2D.Double pos = axisPositions.get(attributeName);
                double scale = axisScales.getOrDefault(attributeName, 1.0);
                int scaledHeight = (int) (AXIS_HEIGHT * scale);
                double y = pos.y + scaledHeight - normalizedValue * scaledHeight;
                points.add(new Point2D.Double(pos.x, y));
            }
            
            return points;
        }
    }
} 
