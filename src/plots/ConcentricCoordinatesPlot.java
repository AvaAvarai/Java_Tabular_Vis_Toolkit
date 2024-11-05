package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConcentricCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private List<Integer> hiddenRows;
    private double globalMaxValue;
    private ConcentricCoordinatesPanel plotPanel;
    private double piAdjustment = 0.05;
    private boolean showLabels = true;
    private boolean closeLoop = true;
    private boolean concentricMode = true;
    private Map<String, Boolean> normalizeAttributes = new HashMap<>();
    private Map<String, Double> attributeRotations = new HashMap<>();
    private Map<String, Boolean> attributeDirections = new HashMap<>();
    private Map<String, Point> axisPositions = new HashMap<>();
    private Map<String, Double> attributeMinValues = new HashMap<>();
    private Map<String, Double> attributeMaxValues = new HashMap<>();
    private String draggedAxis = null;
    private Set<String> hiddenClasses = new HashSet<>();
    private Map<String, JSlider> attributeSliders = new HashMap<>();
    private Map<String, JCheckBox> attributeToggles = new HashMap<>();
    private Map<String, JCheckBox> normalizeToggles = new HashMap<>();

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int TITLE_PADDING = 20;

    public ConcentricCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, List<Integer> hiddenRows, String datasetName) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.hiddenRows = hiddenRows;

        // Initialize rotation values, directions and normalization for each attribute
        for (String attribute : attributeNames) {
            attributeRotations.put(attribute, 0.0);
            attributeDirections.put(attribute, true);
            normalizeAttributes.put(attribute, true);
        }

        // Calculate min and max values for each attribute
        for (int i = 0; i < attributeNames.size(); i++) {
            String attribute = attributeNames.get(i);
            List<Double> attributeValues = data.get(i);
            double min = attributeValues.stream().min(Double::compare).orElse(0.0);
            double max = attributeValues.stream().max(Double::compare).orElse(1.0);
            attributeMinValues.put(attribute, min);
            attributeMaxValues.put(attribute, max);
        }

        // Calculate the global maximum value across all attributes
        this.globalMaxValue = data.stream()
            .flatMap(List::stream)
            .max(Double::compare)
            .orElse(1.0);

        setTitle("Concentric Coordinates Plot");
        setSize(800, 800); // Increased height to accommodate sliders
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        plotPanel = new ConcentricCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(800, 600));

        // Add mouse listeners for axis dragging
        plotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!concentricMode) {
                    Point clickPoint = e.getPoint();
                    // Check if click is near any axis center
                    for (String attribute : attributeNames) {
                        Point axisCenter = axisPositions.get(attribute);
                        if (axisCenter != null) {
                            if (clickPoint.distance(axisCenter) < 20) {
                                draggedAxis = attribute;
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedAxis = null;
            }
        });

        plotPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!concentricMode && draggedAxis != null) {
                    Point newPos = e.getPoint();
                    axisPositions.put(draggedAxis, newPos);
                    plotPanel.repaint();
                }
            }
        });

        // Add the plot panel to a scroll pane
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add a key listener for the space bar to save a screenshot
        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ConcentricCoordinates", datasetName);
            }
        });

        // Create control panel for sliders and toggles
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(Color.WHITE);
        
        // Add PI adjustment slider
        JPanel globalControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        globalControlPanel.setBackground(Color.WHITE);
        
        JLabel sliderLabel = new JLabel("PI Adjustment: ");
        JSlider piSlider = new JSlider(JSlider.HORIZONTAL, 0, 360*2, 5);
        piSlider.setMajorTickSpacing(360);
        piSlider.setMinorTickSpacing(0);
        piSlider.setPaintTicks(true);
        piSlider.setPaintLabels(true);
        // use an array of labels to make sure the labels are aligned properly
        String[] labels = {"0", "2π", "4π"};
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(360, new JLabel("2π"));
        labelTable.put(720, new JLabel("4π"));
        piSlider.setLabelTable(labelTable);
        piSlider.addChangeListener(e -> {
            piAdjustment = piSlider.getValue() / 100.0;
            plotPanel.repaint();
        });

        // Add label toggle button
        JToggleButton labelToggle = new JToggleButton("Show Labels", true);
        labelToggle.addActionListener(e -> {
            showLabels = labelToggle.isSelected();
            plotPanel.repaint();
        });

        // Add loop toggle button
        JToggleButton loopToggle = new JToggleButton("Close Loop", true);
        loopToggle.addActionListener(e -> {
            closeLoop = loopToggle.isSelected();
            plotPanel.repaint();
        });

        // Add concentric mode toggle button
        JToggleButton concentricToggle = new JToggleButton("Concentric Mode", true);
        concentricToggle.addActionListener(e -> {
            concentricMode = concentricToggle.isSelected();
            if (!concentricMode) {
                // Initialize axis positions when switching to non-concentric mode
                int centerX = plotPanel.getWidth() / 2;
                int centerY = plotPanel.getHeight() / 2;
                int maxRadius = Math.min(centerX, centerY) - 50;
                int numAttributes = attributeNames.size();
                int spacing = maxRadius / (numAttributes + 1);
                
                for (int i = 0; i < attributeNames.size(); i++) {
                    int x = centerX + (i - numAttributes/2) * spacing * 2;
                    axisPositions.put(attributeNames.get(i), new Point(x, centerY));
                }
            }
            plotPanel.repaint();
        });

        // Add optimize axes button
        JButton optimizeButton = new JButton("Optimize Axes");
        optimizeButton.addActionListener(e -> {
            if (!concentricMode) {
                optimizeAxesLayout();
                // Update UI controls to match optimized settings
                for (String attribute : attributeNames) {
                    // Update rotation sliders
                    double rotation = attributeRotations.get(attribute);
                    int degrees = (int)(rotation * 180 / Math.PI);
                    attributeSliders.get(attribute).setValue(degrees);
                    
                    // Update direction toggles
                    boolean direction = attributeDirections.get(attribute);
                    attributeToggles.get(attribute).setSelected(!direction);
                }
                plotPanel.repaint();
            }
        });

        globalControlPanel.add(sliderLabel);
        globalControlPanel.add(piSlider);
        globalControlPanel.add(labelToggle);
        globalControlPanel.add(loopToggle);
        globalControlPanel.add(concentricToggle);
        globalControlPanel.add(optimizeButton);
        
        controlPanel.add(globalControlPanel);

        // Add individual attribute rotation sliders, direction toggles, and normalization toggles
        JPanel attributeSlidersPanel = new JPanel();
        attributeSlidersPanel.setLayout(new BoxLayout(attributeSlidersPanel, BoxLayout.Y_AXIS));
        attributeSlidersPanel.setBackground(Color.WHITE);
        attributeSlidersPanel.setBorder(BorderFactory.createTitledBorder("Attribute Controls"));

        for (String attribute : attributeNames) {
            JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            sliderPanel.setBackground(Color.WHITE);
            
            JLabel label = new JLabel(attribute + ": ");
            
            JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
            slider.setPreferredSize(new Dimension(200, 40));
            slider.addChangeListener(e -> {
                attributeRotations.put(attribute, slider.getValue() * Math.PI / 180);
                plotPanel.repaint();
            });
            attributeSliders.put(attribute, slider);
            
            JCheckBox directionToggle = new JCheckBox("Reverse", false);
            directionToggle.addActionListener(e -> {
                attributeDirections.put(attribute, !directionToggle.isSelected());
                plotPanel.repaint();
            });
            attributeToggles.put(attribute, directionToggle);
            
            JCheckBox normalizeToggle = new JCheckBox("Normalize", true);
            normalizeToggle.addActionListener(e -> {
                normalizeAttributes.put(attribute, normalizeToggle.isSelected());
                plotPanel.repaint();
            });
            normalizeToggles.put(attribute, normalizeToggle);
            
            sliderPanel.add(label);
            sliderPanel.add(slider);
            sliderPanel.add(directionToggle);
            sliderPanel.add(normalizeToggle);
            attributeSlidersPanel.add(sliderPanel);
        }

        JScrollPane sliderScrollPane = new JScrollPane(attributeSlidersPanel);
        sliderScrollPane.setPreferredSize(new Dimension(300, 150));
        controlPanel.add(sliderScrollPane);

        // Ensure the JFrame is focusable to capture key events
        setFocusable(true);
        requestFocusInWindow();

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        setContentPane(mainPanel);
    }

    private void optimizeAxesLayout() {
        int centerX = plotPanel.getWidth() / 2;
        int centerY = plotPanel.getHeight() / 2;
        int maxRadius = Math.min(centerX, centerY) - 50;
        int numAttributes = attributeNames.size();
        
        // Calculate correlation matrix between attributes
        double[][] correlations = calculateCorrelationMatrix();
        
        // Create list of attribute indices to optimize
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < numAttributes; i++) {
            indices.add(i);
        }
        
        // Optimize attribute order based on correlations
        optimizeAttributeOrder(indices, correlations);
        
        // Position axes based on optimized order
        for (int i = 0; i < numAttributes; i++) {
            String attribute = attributeNames.get(indices.get(i));
            double angle = 2 * Math.PI * i / numAttributes;
            
            // Set optimized position
            int x = centerX + (int)(maxRadius * 0.6 * Math.cos(angle));
            int y = centerY + (int)(maxRadius * 0.6 * Math.sin(angle));
            axisPositions.put(attribute, new Point(x, y));
            
            // Set optimized rotation
            attributeRotations.put(attribute, angle);
            
            // Set optimized direction based on correlations
            boolean shouldReverse = calculateShouldReverse(indices.get(i), correlations);
            attributeDirections.put(attribute, !shouldReverse);
        }
    }
    
    private double[][] calculateCorrelationMatrix() {
        int n = attributeNames.size();
        double[][] correlations = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                List<Double> attr1 = data.get(i);
                List<Double> attr2 = data.get(j);
                correlations[i][j] = calculateCorrelation(attr1, attr2);
            }
        }
        
        return correlations;
    }
    
    private double calculateCorrelation(List<Double> x, List<Double> y) {
        int n = x.size();
        double sumX = 0, sumY = 0, sumXY = 0;
        double sumX2 = 0, sumY2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
            sumXY += x.get(i) * y.get(i);
            sumX2 += x.get(i) * x.get(i);
            sumY2 += y.get(i) * y.get(i);
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        
        return denominator == 0 ? 0 : numerator / denominator;
    }
    
    private void optimizeAttributeOrder(List<Integer> indices, double[][] correlations) {
        // Simple greedy algorithm to place highly correlated attributes adjacent to each other
        for (int i = 1; i < indices.size(); i++) {
            int bestIndex = i;
            double bestCorrelation = Math.abs(correlations[indices.get(i-1)][indices.get(i)]);
            
            for (int j = i + 1; j < indices.size(); j++) {
                double correlation = Math.abs(correlations[indices.get(i-1)][indices.get(j)]);
                if (correlation > bestCorrelation) {
                    bestCorrelation = correlation;
                    bestIndex = j;
                }
            }
            
            if (bestIndex != i) {
                Collections.swap(indices, i, bestIndex);
            }
        }
    }
    
    private boolean calculateShouldReverse(int attrIndex, double[][] correlations) {
        // Count negative correlations with other attributes
        int negativeCount = 0;
        for (int i = 0; i < correlations.length; i++) {
            if (i != attrIndex && correlations[attrIndex][i] < 0) {
                negativeCount++;
            }
        }
        // Reverse if more than half correlations are negative
        return negativeCount > correlations.length / 2;
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
            colorLabelPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            colorLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hiddenClasses.contains(className)) {
                        hiddenClasses.remove(className);
                        colorLabelPanel.setBackground(Color.WHITE);
                    } else {
                        hiddenClasses.add(className);
                        colorLabelPanel.setBackground(Color.LIGHT_GRAY);
                    }
                    plotPanel.repaint();
                }
            });

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

    private class ConcentricCoordinatesPanel extends JPanel {
        public ConcentricCoordinatesPanel() {
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

            // Draw the title
            String title = "Concentric Coordinates Plot";
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics(TITLE_FONT);
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            // Fill the plot area with light grey background
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, titleHeight + TITLE_PADDING, getWidth(), getHeight() - titleHeight - TITLE_PADDING);

            // Calculate center and radius for the plot
            int centerX = getWidth() / 2;
            int centerY = (getHeight() - titleHeight - TITLE_PADDING) / 2 + titleHeight + TITLE_PADDING;
            int maxRadius = Math.min(centerX, (getHeight() - titleHeight - TITLE_PADDING) / 2) - 50;

            // Draw concentric axes
            if (concentricMode) {
                drawConcentricAxes(g2, centerX, centerY, maxRadius);
            } else {
                drawIndividualAxes(g2, centerX, centerY, maxRadius);
            }

            // Draw the concentric coordinates for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel)) {
                    drawConcentricCoordinates(g2, row, centerX, centerY, maxRadius);
                }
            }

            // Draw attribute labels if enabled
            if (showLabels) {
                drawAttributeLabels(g2, centerX, centerY, maxRadius);
            }

            // Draw highlights for selected rows
            drawHighlights(g2, centerX, centerY, maxRadius);
        }

        private void drawConcentricAxes(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setColor(Color.BLACK);
            int numAttributes = attributeNames.size();
            for (int i = 1; i <= numAttributes; i++) {
                int currentRadius = i * (maxRadius / numAttributes);
                g2.draw(new Ellipse2D.Double(centerX - currentRadius, centerY - currentRadius, 2 * currentRadius, 2 * currentRadius));
            }
        }

        private void drawIndividualAxes(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setColor(Color.BLACK);
            int numAttributes = attributeNames.size();
            int spacing = maxRadius / (numAttributes + 1);
            
            for (int i = 0; i < numAttributes; i++) {
                String attribute = attributeNames.get(i);
                Point pos = axisPositions.getOrDefault(attribute, 
                    new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                g2.draw(new Ellipse2D.Double(pos.x - spacing, pos.y - spacing, spacing * 2, spacing * 2));
            }
        }

        private void drawConcentricCoordinates(Graphics2D g2, int row, int centerX, int centerY, int maxRadius) {
            int numAttributes = attributeNames.size();
            Point2D.Double[] points = new Point2D.Double[numAttributes];

            for (int i = 0; i < numAttributes; i++) {
                double value = data.get(i).get(row);
                double normalizedValue;
                
                String attribute = attributeNames.get(i);
                if (normalizeAttributes.get(attribute)) {
                    double min = attributeMinValues.get(attribute);
                    double max = attributeMaxValues.get(attribute);
                    normalizedValue = (value - min) / (max - min);
                } else {
                    normalizedValue = value / globalMaxValue;
                }
                
                // Apply direction toggle
                if (!attributeDirections.get(attribute)) {
                    normalizedValue = 1.0 - normalizedValue;
                }

                // Apply both global and individual attribute rotations
                double attributeRotation = attributeRotations.get(attributeNames.get(i));
                double angle = -(Math.PI - piAdjustment) / 2 + normalizedValue * 2 * (Math.PI - piAdjustment) + attributeRotation;
                
                if (concentricMode) {
                    int currentRadius = (i + 1) * (maxRadius / numAttributes);
                    double x = centerX + currentRadius * Math.cos(angle);
                    double y = centerY + currentRadius * Math.sin(angle);
                    points[i] = new Point2D.Double(x, y);
                } else {
                    int spacing = maxRadius / (numAttributes + 1);
                    Point pos = axisPositions.getOrDefault(attributeNames.get(i),
                        new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                    double pointX = pos.x + spacing * Math.cos(angle);
                    double pointY = pos.y + spacing * Math.sin(angle);
                    points[i] = new Point2D.Double(pointX, pointY);
                }
            }

            String classLabel = classLabels.get(row);
            Color color = classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);

            // Draw lines connecting the points across the circles
            g2.setStroke(new BasicStroke(1.0f));
            for (int i = 0; i < numAttributes - 1; i++) {
                g2.draw(new Line2D.Double(points[i], points[i + 1]));
            }
            
            if (closeLoop) {
                g2.draw(new Line2D.Double(points[numAttributes - 1], points[0]));
            }

            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                g2.fill(shape);
                g2.translate(-points[i].x, -points[i].y);
            }
        }

        private void drawHighlights(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2.0f));

            for (int row : selectedRows) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) {
                    continue;
                }

                int numAttributes = attributeNames.size();
                Point2D.Double[] points = new Point2D.Double[numAttributes];

                for (int i = 0; i < numAttributes; i++) {
                    double value = data.get(i).get(row);
                    double normalizedValue;
                    
                    String attribute = attributeNames.get(i);
                    if (normalizeAttributes.get(attribute)) {
                        double min = attributeMinValues.get(attribute);
                        double max = attributeMaxValues.get(attribute);
                        normalizedValue = (value - min) / (max - min);
                    } else {
                        normalizedValue = value / globalMaxValue;
                    }
                    
                    // Apply direction toggle
                    if (!attributeDirections.get(attribute)) {
                        normalizedValue = 1.0 - normalizedValue;
                    }

                    double attributeRotation = attributeRotations.get(attributeNames.get(i));
                    double angle = -(Math.PI - piAdjustment) / 2 + normalizedValue * 2 * (Math.PI - piAdjustment) + attributeRotation;
                    
                    if (concentricMode) {
                        int currentRadius = (i + 1) * (maxRadius / numAttributes);
                        double x = centerX + currentRadius * Math.cos(angle);
                        double y = centerY + currentRadius * Math.sin(angle);
                        points[i] = new Point2D.Double(x, y);
                    } else {
                        int spacing = maxRadius / (numAttributes + 1);
                        Point pos = axisPositions.getOrDefault(attributeNames.get(i),
                            new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                        double pointX = pos.x + spacing * Math.cos(angle);
                        double pointY = pos.y + spacing * Math.sin(angle);
                        points[i] = new Point2D.Double(pointX, pointY);
                    }
                }

                for (int i = 0; i < numAttributes - 1; i++) {
                    g2.draw(new Line2D.Double(points[i], points[i + 1]));
                }
                
                if (closeLoop) {
                    g2.draw(new Line2D.Double(points[numAttributes - 1], points[0]));
                }

                for (int i = 0; i < numAttributes; i++) {
                    g2.translate(points[i].x, points[i].y);
                    Shape shape = classShapes.getOrDefault(classLabels.get(row), new Ellipse2D.Double(-4.5, -4.5, 9, 9));
                    g2.fill(shape);
                    g2.translate(-points[i].x, -points[i].y);
                }
            }
        }

        private void drawAttributeLabels(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setFont(AXIS_LABEL_FONT);
            g2.setColor(Color.BLACK);

            int numAttributes = attributeNames.size();
            for (int i = 0; i < numAttributes; i++) {
                if (concentricMode) {
                    int currentRadius = (i + 1) * (maxRadius / numAttributes);
                    
                    // Apply rotation to label position
                    double attributeRotation = attributeRotations.get(attributeNames.get(i));
                    double labelAngle = -(Math.PI / 2) + attributeRotation; // Start from top (12 o'clock)
                    
                    int x = centerX + (int)((currentRadius + 20) * Math.cos(labelAngle));
                    int y = centerY + (int)((currentRadius + 20) * Math.sin(labelAngle));
                    g2.drawString(attributeNames.get(i), x, y);
                } else {
                    int spacing = maxRadius / (numAttributes + 1);
                    Point pos = axisPositions.getOrDefault(attributeNames.get(i),
                        new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                    g2.drawString(attributeNames.get(i), pos.x - 20, pos.y - spacing - 10);
                }
            }
        }
    }
}
