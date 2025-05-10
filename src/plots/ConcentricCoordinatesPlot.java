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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.awt.geom.Path2D;
import src.utils.LegendUtils;
import java.util.Objects;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
    private double piAdjustment = 0.0;
    private boolean showLabels = true;
    private boolean closeLoop = true;
    private double axisGap = 0.0; // Gap between first and last attribute positions
    private boolean concentricMode = true;
    private enum DensityMode {
        NO_DENSITY,
        DENSITY_WITH_OPACITY,
        DENSITY_WITH_THICKNESS
    }
    private DensityMode currentDensityMode = DensityMode.NO_DENSITY;
    private Map<String, Boolean> normalizeAttributes = new HashMap<>();
    private Map<String, Double> attributeRotations = new HashMap<>();
    private Map<String, Boolean> attributeDirections = new HashMap<>();
    private Map<String, Point> axisPositions = new HashMap<>();
    private Map<String, Double> attributeMinValues = new HashMap<>();
    private Map<String, Double> attributeMaxValues = new HashMap<>();
    private Map<String, Double> attributeRadii = new HashMap<>(); // Added for circle sizes
    private String draggedAxis = null;
    private Set<String> hiddenClasses = new HashSet<>();
    private Map<String, JSlider> attributeSliders = new HashMap<>();
    private Map<String, JSlider> radiusSliders = new HashMap<>(); // Added for radius control
    private Map<String, JCheckBox> attributeToggles = new HashMap<>();
    private Map<String, JCheckBox> normalizeToggles = new HashMap<>();
    private double zoomLevel = 1.0;
    private JScrollPane plotScrollPane;
    private boolean showConvexHulls = false;
    
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
    
            // Initialize rotation values, directions, radii and normalization for each attribute
            for (String attribute : attributeNames) {
                attributeRotations.put(attribute, 0.0);
                attributeDirections.put(attribute, true);
                normalizeAttributes.put(attribute, true);
                attributeRadii.put(attribute, 1.0); // Initialize all radii to 1.0
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
    
            setTitle("Circle Coordinates Plot (" + datasetName + ")");
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
            plotScrollPane = new JScrollPane(plotPanel);
            plotScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            plotScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            plotScrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    
            // Add a key listener for the space bar to save a screenshot
            plotScrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
            plotScrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ScreenshotUtils.captureAndSaveScreenshot(plotScrollPane, "ConcentricCoordinates", datasetName);
                }
            });
    
            // Create control panel for sliders and toggles
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            controlPanel.setBackground(Color.WHITE);
            
            // Add PI adjustment slider
            JPanel globalControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            globalControlPanel.setBackground(Color.WHITE);
            
            // Add density mode buttons
            ButtonGroup densityButtonGroup = new ButtonGroup();

            // No density button
            JRadioButton noDensityButton = new JRadioButton("No Density");
            noDensityButton.setSelected(true);
            noDensityButton.addActionListener(e -> {
                currentDensityMode = DensityMode.NO_DENSITY;
                plotPanel.repaint();
            });
            densityButtonGroup.add(noDensityButton);
            globalControlPanel.add(noDensityButton);

            // Density with opacity button
            JRadioButton opacityDensityButton = new JRadioButton("Density with Opacity");
            opacityDensityButton.addActionListener(e -> {
                currentDensityMode = DensityMode.DENSITY_WITH_OPACITY;
                plotPanel.repaint();
            });
            densityButtonGroup.add(opacityDensityButton);
            globalControlPanel.add(opacityDensityButton);

            // Density with thickness button
            JRadioButton thicknessDensityButton = new JRadioButton("Density with Thickness");
            thicknessDensityButton.addActionListener(e -> {
                currentDensityMode = DensityMode.DENSITY_WITH_THICKNESS;
                plotPanel.repaint();
            });
            densityButtonGroup.add(thicknessDensityButton);
            globalControlPanel.add(thicknessDensityButton);
            
            // Add zoom slider
            JLabel zoomLabel = new JLabel("Zoom: ");
            JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
            zoomSlider.setMajorTickSpacing(50);
            zoomSlider.setMinorTickSpacing(10);
            zoomSlider.setPaintTicks(true);
            zoomSlider.setPaintLabels(true);
            Hashtable<Integer, JLabel> zoomLabels = new Hashtable<>();
            zoomLabels.put(50, new JLabel("50%"));
            zoomLabels.put(100, new JLabel("100%"));
            zoomLabels.put(150, new JLabel("150%"));
            zoomLabels.put(200, new JLabel("200%"));
            zoomSlider.setLabelTable(zoomLabels);
            zoomSlider.addChangeListener(e -> {
                zoomLevel = zoomSlider.getValue() / 100.0;
                
                // Calculate center point
                int centerX = plotPanel.getWidth() / 2;
                int centerY = plotPanel.getHeight() / 2;
                
                // Set preferred size centered around the middle
                int newWidth = (int)(800 * zoomLevel);
                int newHeight = (int)(600 * zoomLevel);
                plotPanel.setPreferredSize(new Dimension(newWidth, newHeight));
                
                // Adjust scroll position to keep center point
                JViewport viewport = plotScrollPane.getViewport();
                Point p = viewport.getViewPosition();
                p.x = (newWidth - viewport.getWidth()) / 2;
                p.y = (newHeight - viewport.getHeight()) / 2;
                viewport.setViewPosition(p);
                
                plotPanel.revalidate();
                plotScrollPane.revalidate();
                plotPanel.repaint();
            });
    
            JLabel sliderLabel = new JLabel("PI Adjustment: ");
            JSlider piSlider = new JSlider(JSlider.HORIZONTAL, -360*2, 360*2, 5);
            piSlider.setMajorTickSpacing(360);
            piSlider.setMinorTickSpacing(0);
            piSlider.setPaintTicks(true);
            piSlider.setPaintLabels(true);
            Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
            labelTable.put(-720, new JLabel("-2\u03C0"));
            labelTable.put(-360, new JLabel("-\u03C0")); 
            labelTable.put(0, new JLabel("0"));
            labelTable.put(360, new JLabel("\u03C0"));
            labelTable.put(720, new JLabel("2\u03C0"));
            piSlider.setLabelTable(labelTable);
            piSlider.addChangeListener(e -> {
                piAdjustment = piSlider.getValue() / 100.0;
                plotPanel.repaint();
            });
    
            // Add gap size slider
            JLabel gapLabel = new JLabel("Axis Gap: ");
            JSlider gapSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, 0);
            gapSlider.setMajorTickSpacing(10);
            gapSlider.setMinorTickSpacing(5);
            gapSlider.setPaintTicks(true);
            gapSlider.setPaintLabels(true);
            Hashtable<Integer, JLabel> gapLabels = new Hashtable<>();
            gapLabels.put(0, new JLabel("0%"));
            gapLabels.put(10, new JLabel("10%"));
            gapLabels.put(20, new JLabel("20%"));
            gapLabels.put(30, new JLabel("30%"));
            gapLabels.put(40, new JLabel("40%"));
            gapLabels.put(50, new JLabel("50%"));
            gapSlider.setLabelTable(gapLabels);
            gapSlider.addChangeListener(e -> {
                axisGap = gapSlider.getValue() / 100.0;
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
            JToggleButton concentricToggle = new JToggleButton("Freeform Mode", false);
            concentricToggle.addActionListener(e -> {
                concentricMode = !concentricToggle.isSelected();
                concentricToggle.setText(concentricMode ? "Freeform Mode" : "Concentric Mode");
                if (!concentricMode) {
                    // Initialize axis positions when switching to freeform mode
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
                        
                        // Update radius sliders
                        double radius = attributeRadii.get(attribute);
                        radiusSliders.get(attribute).setValue((int)(radius * 100));
                    }
                    plotPanel.repaint();
                }
            });
    
            // Add global convex hull toggle
            JToggleButton convexHullToggle = new JToggleButton("Show Convex Hulls", false);
            convexHullToggle.addActionListener(e -> {
                showConvexHulls = convexHullToggle.isSelected();
            plotPanel.repaint();
        });

        // Add global normalization toggle
        JToggleButton normalizeAllToggle = new JToggleButton("Normalize All", true);
        normalizeAllToggle.addActionListener(e -> {
            boolean normalizeAll = normalizeAllToggle.isSelected();
            for (String attribute : attributeNames) {
                normalizeAttributes.put(attribute, normalizeAll);
                normalizeToggles.get(attribute).setSelected(normalizeAll);
            }
            plotPanel.repaint();
        });

        globalControlPanel.add(zoomLabel);
        globalControlPanel.add(zoomSlider);
        globalControlPanel.add(sliderLabel);
        globalControlPanel.add(piSlider);
        globalControlPanel.add(gapLabel);
        globalControlPanel.add(gapSlider);
        globalControlPanel.add(labelToggle);
        globalControlPanel.add(loopToggle);
        globalControlPanel.add(concentricToggle);
        globalControlPanel.add(optimizeButton);
        globalControlPanel.add(normalizeAllToggle);
        globalControlPanel.add(convexHullToggle);
        
        controlPanel.add(globalControlPanel);

        // Add individual attribute rotation sliders, radius sliders, direction toggles, and normalization toggles
        JPanel attributeSlidersPanel = new JPanel();
        attributeSlidersPanel.setLayout(new BoxLayout(attributeSlidersPanel, BoxLayout.Y_AXIS));
        attributeSlidersPanel.setBackground(Color.WHITE);
        attributeSlidersPanel.setBorder(BorderFactory.createTitledBorder("Attribute Controls"));

        for (String attribute : attributeNames) {
            JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            sliderPanel.setBackground(Color.WHITE);
            
            JLabel label = new JLabel(attribute + ": ");
            
            // Rotation slider
            JSlider rotationSlider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
            rotationSlider.setPreferredSize(new Dimension(200, 40));
            rotationSlider.addChangeListener(e -> {
                attributeRotations.put(attribute, rotationSlider.getValue() * Math.PI / 180);
                plotPanel.repaint();
            });
            attributeSliders.put(attribute, rotationSlider);
            
            // Radius slider
            JSlider radiusSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
            radiusSlider.setPreferredSize(new Dimension(200, 40));
            radiusSlider.addChangeListener(e -> {
                attributeRadii.put(attribute, radiusSlider.getValue() / 100.0);
                plotPanel.repaint();
            });
            radiusSliders.put(attribute, radiusSlider);
            
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
            sliderPanel.add(new JLabel("Rotation:"));
            sliderPanel.add(rotationSlider);
            sliderPanel.add(new JLabel("Radius:"));
            sliderPanel.add(radiusSlider);
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

        // Add key listener to the frame instead of the panel
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) { // 'A' key for alignment
                    plotPanel.alignHighlightedCases();
                }
            }
        });

        // Add components to main panel
        mainPanel.add(plotScrollPane, BorderLayout.CENTER);
        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        setContentPane(mainPanel);
    }

    private List<Point2D.Double> computeConvexHull(List<Point2D.Double> points) {
        points.sort(Comparator.comparingDouble((Point2D.Double p) -> p.x).thenComparingDouble(p -> p.y));
        List<Point2D.Double> lower = new ArrayList<>();
        for (Point2D.Double p : points) {
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }
        List<Point2D.Double> upper = new ArrayList<>();
        for (int i = points.size() - 1; i >= 0; i--) {
            Point2D.Double p = points.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    private double cross(Point2D.Double o, Point2D.Double a, Point2D.Double b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private void drawConvexHulls(Graphics2D g2, Map<String, List<Point2D.Double>> classPoints) {
        for (Map.Entry<String, List<Point2D.Double>> entry : classPoints.entrySet()) {
            String classLabel = entry.getKey();
            List<Point2D.Double> hull = computeConvexHull(entry.getValue());
            g2.setColor(new Color(classColors.getOrDefault(classLabel, Color.BLACK).getRGB() & 0xFFFFFF | 0x66000000, true));
            Path2D.Double path = new Path2D.Double();
            for (int i = 0; i < hull.size(); i++) {
                Point2D.Double p = hull.get(i);
                if (i == 0) {
                    path.moveTo(p.x, p.y);
                } else {
                    path.lineTo(p.x, p.y);
                }
            }
            path.closePath();
            g2.fill(path);
        }
    }
    
    private List<Point2D.Double> getPolylinePoints(int row, int centerX, int centerY, int maxRadius) {
        List<Point2D.Double> points = new ArrayList<>();
        int numAttributes = attributeNames.size();
        
        for (int i = 0; i < numAttributes; i++) {
            String attribute = attributeNames.get(i);
            double value = data.get(i).get(row);
            double normalizedValue = normalizeAttributes.get(attribute)
                    ? (value - attributeMinValues.get(attribute)) / (attributeMaxValues.get(attribute) - attributeMinValues.get(attribute))
                    : value / globalMaxValue;
            if (!attributeDirections.get(attribute)) {
                normalizedValue = 1.0 - normalizedValue;
            }
            
            // Value determines angle around the circle
            double attributeRotation = attributeRotations.get(attribute);
            
            if (concentricMode) {
                // In concentric mode, apply the gap between attributes
                // Calculate what portion of circle to use (adjust for gap)
                double usedPortion = 1.0 - axisGap;
                // Use normalized value to calculate angle, adjusting for gap
                double angle = (1.5 * Math.PI) + (normalizedValue * usedPortion * 2 * Math.PI) + attributeRotation + piAdjustment;
                
                // Each attribute has its own fixed circle
                double radius = (i + 1) * (maxRadius / numAttributes) * attributeRadii.get(attribute);
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                points.add(new Point2D.Double(x, y));
            } else {
                // In freeform mode, use the axis-specific positions
                Point pos = axisPositions.getOrDefault(attribute, 
                    new Point(centerX + (i - numAttributes/2) * (maxRadius / (numAttributes + 1)) * 2, centerY));
                
                // Use the normalized value to calculate distance from axis center
                double axisRadius = (maxRadius / (numAttributes + 1)) * attributeRadii.get(attribute);
                
                // Apply the gap in the angle calculation
                double usedPortion = 1.0 - axisGap;
                double angle = (1.5 * Math.PI) + (normalizedValue * usedPortion * 2 * Math.PI) + attributeRotation + piAdjustment;
                
                double x = pos.x + axisRadius * Math.cos(angle);
                double y = pos.y + axisRadius * Math.sin(angle);
                points.add(new Point2D.Double(x, y));
            }
        }
        return points;
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
            
            // Set optimized radius based on correlation strength
            double avgCorrelation = calculateAverageCorrelation(indices.get(i), correlations);
            double radius = Math.abs(avgCorrelation); // Scale radius between 0.0 and 1.0
            attributeRadii.put(attribute, radius);
        }
    }
    
    private double calculateAverageCorrelation(int attrIndex, double[][] correlations) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < correlations.length; i++) {
            if (i != attrIndex) {
                sum += Math.abs(correlations[attrIndex][i]);
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
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

    private class ConcentricCoordinatesPanel extends JPanel {
        // Add LineSegment class to track identical segments
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
        
        public ConcentricCoordinatesPanel() {
            setBackground(new Color(0, 0, 0, 0));
        }

        public void alignHighlightedCases() {
            if (selectedRows.isEmpty()) {
                return;
            }

            // Calculate mean values for highlighted cases
            List<Double> meanValues = new ArrayList<>();
            for (int i = 0; i < attributeNames.size(); i++) {
                double sum = 0;
                for (int row : selectedRows) {
                    double value = data.get(i).get(row);
                    String attribute = attributeNames.get(i);
                    if (normalizeAttributes.get(attribute)) {
                        double min = attributeMinValues.get(attribute);
                        double max = attributeMaxValues.get(attribute);
                        value = (value - min) / (max - min);
                    } else {
                        value = value / globalMaxValue;
                    }
                    if (!attributeDirections.get(attribute)) {
                        value = 1.0 - value;
                    }
                    sum += value;
                }
                meanValues.add(sum / selectedRows.size());
            }

            // Calculate required rotations to align means vertically
            for (int i = 0; i < attributeNames.size(); i++) {
                String attribute = attributeNames.get(i);
                double meanValue = meanValues.get(i);
                double currentAngle = (1.5 * Math.PI) + (meanValue * 2 * Math.PI) + attributeRotations.get(attribute) + piAdjustment;
                double targetAngle = 1.5 * Math.PI; // Straight up
                double rotationAdjustment = targetAngle - currentAngle;
                
                // Update rotation
                double newRotation = attributeRotations.get(attribute) + rotationAdjustment;
                attributeRotations.put(attribute, newRotation);
                
                // Update rotation slider if it exists
                JSlider rotationSlider = attributeSliders.get(attribute);
                if (rotationSlider != null) {
                    int degrees = (int)(newRotation * 180 / Math.PI);
                    rotationSlider.setValue(degrees);
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Get the center point of the viewport
            Rectangle viewRect = plotScrollPane.getViewport().getViewRect();
            int centerX = viewRect.x + viewRect.width / 2;
            int centerY = viewRect.y + viewRect.height / 2;
            
            // Translate to center before scaling
            g2.translate(centerX, centerY);
            g2.scale(zoomLevel, zoomLevel);
            g2.translate(-centerX, -centerY);

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
            centerX = getWidth() / 2;
            centerY = (getHeight() - titleHeight - TITLE_PADDING) / 2 + titleHeight + TITLE_PADDING;
            int maxRadius = Math.min(centerX, (getHeight() - titleHeight - TITLE_PADDING) / 2) - 50;

            // Draw concentric axes
            if (concentricMode) {
                drawConcentricAxes(g2, centerX, centerY, maxRadius);
            } else {
                drawIndividualAxes(g2, centerX, centerY, maxRadius);
            }

            // Check if we should use density visualization
            if (currentDensityMode != DensityMode.NO_DENSITY) {
                drawWithDensity(g2, centerX, centerY, maxRadius);
            } else {
                // Sort rows to draw benign on top of malignant
                List<Integer> sortedRows = new ArrayList<>();
                List<Integer> benignRows = new ArrayList<>();
                List<Integer> malignantRows = new ArrayList<>();
                List<Integer> otherRows = new ArrayList<>();
                
                for (int row = 0; row < data.get(0).size(); row++) {
                    if (hiddenRows.contains(row)) continue;
                    
                    String classLabel = classLabels.get(row);
                    if (hiddenClasses.contains(classLabel)) continue;
                    
                    if (classLabel.equalsIgnoreCase("benign")) {
                        benignRows.add(row);
                    } else if (classLabel.equalsIgnoreCase("malignant")) {
                        malignantRows.add(row);
                    } else {
                        otherRows.add(row);
                    }
                }
                
                // Draw in order: malignant, other, benign (so benign appears on top)
                for (int row : malignantRows) {
                    drawConcentricCoordinates(g2, row, centerX, centerY, maxRadius);
                }
                for (int row : otherRows) {
                    drawConcentricCoordinates(g2, row, centerX, centerY, maxRadius);
                }
                for (int row : benignRows) {
                    drawConcentricCoordinates(g2, row, centerX, centerY, maxRadius);
                }
            }
            
            if (showConvexHulls) {
                Map<String, List<Point2D.Double>> classPoints = new HashMap<>();
                for (int row = 0; row < data.get(0).size(); row++) {
                    String classLabel = classLabels.get(row);
                    if (!hiddenClasses.contains(classLabel)) {
                        classPoints.computeIfAbsent(classLabel, k -> new ArrayList<>()).addAll(getPolylinePoints(row, centerX, centerY, maxRadius));
                    }
                }
                drawConvexHulls(g2, classPoints);
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
            for (int i = 0; i < numAttributes; i++) {
                String attribute = attributeNames.get(i);
                double radius = attributeRadii.get(attribute);
                int currentRadius = (int)((i + 1) * (maxRadius / numAttributes) * radius);
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
                double radius = attributeRadii.get(attribute);
                int adjustedSpacing = (int)(spacing * radius);
                g2.draw(new Ellipse2D.Double(pos.x - adjustedSpacing, pos.y - adjustedSpacing, adjustedSpacing * 2, adjustedSpacing * 2));
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

                // Value determines angle around the circle
                double attributeRotation = attributeRotations.get(attribute);
                
                // Apply the gap adjustment to the angle calculation
                double usedPortion = 1.0 - axisGap;
                double angle = (1.5 * Math.PI) + (normalizedValue * usedPortion * 2 * Math.PI) + attributeRotation + piAdjustment;
                
                if (concentricMode) {
                    // Each attribute has its own fixed circle
                    int currentRadius = (int)((i + 1) * (maxRadius / numAttributes) * attributeRadii.get(attribute));
                    double x = centerX + currentRadius * Math.cos(angle);
                    double y = centerY + currentRadius * Math.sin(angle);
                    points[i] = new Point2D.Double(x, y);
                } else {
                    int spacing = maxRadius / (numAttributes + 1);
                    Point pos = axisPositions.getOrDefault(attributeNames.get(i),
                        new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                    double radius = attributeRadii.get(attribute);
                    int adjustedSpacing = (int)(spacing * radius);
                    double pointX = pos.x + adjustedSpacing * Math.cos(angle);
                    double pointY = pos.y + adjustedSpacing * Math.sin(angle);
                    points[i] = new Point2D.Double(pointX, pointY);
                }
            }

            String classLabel = classLabels.get(row);
            Color color = classColors.getOrDefault(classLabel, Color.BLACK);
            
            // Choose drawing method based on density mode
            if (currentDensityMode == DensityMode.NO_DENSITY) {
                drawStandardLines(g2, points, color, numAttributes);
            } else {
                // For density modes, the actual drawing is done in separate methods
                return;
            }

            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                g2.fill(shape);
                g2.translate(-points[i].x, -points[i].y);
            }
        }
        
        private void drawStandardLines(Graphics2D g2, Point2D.Double[] points, Color color, int numAttributes) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.0f));

            // Draw lines connecting the points across the circles
            for (int i = 0; i < numAttributes - 1; i++) {
                g2.draw(new Line2D.Double(points[i], points[i + 1]));
            }
            
            if (closeLoop) {
                Point2D.Double first = points[0];
                Point2D.Double last = points[numAttributes - 1];
                
                // Check if we should draw with a gap
                if (axisGap > 0) {
                    // Calculate direction vector
                    double dx = first.x - last.x;
                    double dy = first.y - last.y;
                    double length = Math.sqrt(dx * dx + dy * dy);
                    
                    if (length > 0) {
                        // Normalize and scale by gap size
                        dx /= length;
                        dy /= length;
                        
                        // Calculate new endpoints with gap
                        Point2D.Double gapStart = new Point2D.Double(
                            last.x + dx * length * axisGap / 2,
                            last.y + dy * length * axisGap / 2
                        );
                        
                        Point2D.Double gapEnd = new Point2D.Double(
                            first.x - dx * length * axisGap / 2,
                            first.y - dy * length * axisGap / 2
                        );
                        
                        // Draw the two segments of the closing line with a gap
                        g2.draw(new Line2D.Double(last, gapStart));
                        g2.draw(new Line2D.Double(gapEnd, first));
                    }
                } else {
                    // No gap, draw direct line
                    g2.draw(new Line2D.Double(last, first));
                }
            }
        }

        private void drawHighlights(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            // Skip if there are no selected rows or if we're in density mode (highlights already handled there)
            if (selectedRows.isEmpty() || currentDensityMode != DensityMode.NO_DENSITY) {
                return;
            }
            
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2.0f));

            for (int row : selectedRows) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) {
                    continue;
                }

                List<Point2D.Double> points = getPolylinePoints(row, centerX, centerY, maxRadius);

                // Draw lines connecting the points
                for (int i = 0; i < points.size() - 1; i++) {
                    g2.draw(new Line2D.Double(points.get(i), points.get(i + 1)));
                }
                
                // Draw closing line if needed
                if (closeLoop && points.size() > 1) {
                    g2.draw(new Line2D.Double(points.get(points.size() - 1), points.get(0)));
                }

                // Draw point shapes
                for (Point2D.Double point : points) {
                    g2.translate(point.x, point.y);
                    Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-4.5, -4.5, 9, 9));
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }

        private void drawAttributeLabels(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setFont(AXIS_LABEL_FONT);
            g2.setColor(Color.BLACK);

            int numAttributes = attributeNames.size();
            for (int i = 0; i < numAttributes; i++) {
                if (concentricMode) {
                    // For each attribute circle, draw its label at the top of the circle
                    int currentRadius = (i + 1) * (maxRadius / numAttributes);
                    double radius = attributeRadii.get(attributeNames.get(i));
                    int adjustedRadius = (int)(currentRadius * radius);
                    
                    // Place label at the top of each circle
                    double labelAngle = (1.5 * Math.PI) + attributeRotations.get(attributeNames.get(i)) + piAdjustment;
                    
                    int x = centerX + (int)(adjustedRadius * Math.cos(labelAngle));
                    int y = centerY + (int)(adjustedRadius * Math.sin(labelAngle));
                    
                    // Center the text
                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(attributeNames.get(i));
                    int textHeight = fm.getHeight();
                    x -= textWidth / 2;
                    y -= 5;
                    
                    g2.drawString(attributeNames.get(i), x, y);
                } else {
                    int spacing = maxRadius / (numAttributes + 1);
                    Point pos = axisPositions.getOrDefault(attributeNames.get(i),
                        new Point(centerX + (i - numAttributes/2) * spacing * 2, centerY));
                    g2.drawString(attributeNames.get(i), pos.x - 20, pos.y - spacing - 10);
                }
            }
        }

        private void drawWithDensity(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            // Create a map to store line segments and their counts
            Map<LineSegment, Integer> lineSegmentCounts = new HashMap<>();
            
            // First pass: Count overlapping line segments
            countLineSegments(lineSegmentCounts, false, centerX, centerY, maxRadius); // non-selected lines
            countLineSegments(lineSegmentCounts, true, centerX, centerY, maxRadius);  // selected lines

            // Find maximum density for normalization
            int maxDensity = lineSegmentCounts.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(1);

            // Draw the lines with appropriate density visualization
            if (currentDensityMode == DensityMode.DENSITY_WITH_OPACITY) {
                drawLinesWithOpacity(g2, lineSegmentCounts, maxDensity, false, centerX, centerY, maxRadius);
                drawLinesWithOpacity(g2, lineSegmentCounts, maxDensity, true, centerX, centerY, maxRadius);
            } else if (currentDensityMode == DensityMode.DENSITY_WITH_THICKNESS) {
                drawLinesWithThickness(g2, lineSegmentCounts, maxDensity, false, centerX, centerY, maxRadius);
                drawLinesWithThickness(g2, lineSegmentCounts, maxDensity, true, centerX, centerY, maxRadius);
            }
        }

        private void countLineSegments(Map<LineSegment, Integer> lineSegmentCounts, boolean selectedOnly, int centerX, int centerY, int maxRadius) {
            List<Integer> rowsToProcess;
            if (selectedOnly) {
                rowsToProcess = selectedRows;
            } else {
                rowsToProcess = new ArrayList<>();
                for (int i = 0; i < data.get(0).size(); i++) {
                    if (!selectedRows.contains(i) && !hiddenRows.contains(i)) {
                        rowsToProcess.add(i);
                    }
                }
            }

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPolylinePoints(row, centerX, centerY, maxRadius);
                
                // Count line segments
                for (int i = 0; i < points.size() - 1; i++) {
                    LineSegment segment = new LineSegment(points.get(i), points.get(i + 1));
                    lineSegmentCounts.put(segment, lineSegmentCounts.getOrDefault(segment, 0) + 1);
                }
                
                // Count the closing segment if needed
                if (closeLoop && points.size() > 1) {
                    LineSegment closingSegment = new LineSegment(points.get(points.size() - 1), points.get(0));
                    lineSegmentCounts.put(closingSegment, lineSegmentCounts.getOrDefault(closingSegment, 0) + 1);
                }
            }
        }

        private void drawLinesWithOpacity(Graphics2D g2, Map<LineSegment, Integer> lineSegmentCounts, int maxDensity, 
                                          boolean selectedOnly, int centerX, int centerY, int maxRadius) {
            List<Integer> rowsToProcess;
            if (selectedOnly) {
                rowsToProcess = selectedRows;
            } else {
                rowsToProcess = new ArrayList<>();
                for (int i = 0; i < data.get(0).size(); i++) {
                    if (!selectedRows.contains(i) && !hiddenRows.contains(i)) {
                        rowsToProcess.add(i);
                    }
                }
            }
            
            // THIS IS A HACK WE SHOULD FIX THIS TO DRAW THE SMALL SYMBOLS ON TOP OF THE LARGER ONES
            // Sort rows to process so that benign classes are drawn after malignant ones
            // This ensures benign classes appear on top of malignant ones
            Collections.sort(rowsToProcess, new Comparator<Integer>() {
                @Override
                public int compare(Integer row1, Integer row2) {
                    String class1 = classLabels.get(row1);
                    String class2 = classLabels.get(row2);
                    
                    // If one is "benign" and the other is "malignant", put benign last (on top)
                    if (class1.equalsIgnoreCase("benign") && class2.equalsIgnoreCase("malignant")) {
                        return 1; // benign comes after malignant
                    } else if (class1.equalsIgnoreCase("malignant") && class2.equalsIgnoreCase("benign")) {
                        return -1; // malignant comes before benign
                    } else {
                        return class1.compareTo(class2); // alphabetical for other classes
                    }
                }
            });

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPolylinePoints(row, centerX, centerY, maxRadius);
                Color baseColor = selectedOnly ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                
                // Draw lines with opacity based on density
                for (int i = 0; i < points.size() - 1; i++) {
                    Point2D.Double p1 = points.get(i);
                    Point2D.Double p2 = points.get(i + 1);
                    
                    LineSegment segment = new LineSegment(p1, p2);
                    int count = lineSegmentCounts.getOrDefault(segment, 1);
                    
                    // Calculate opacity based on density
                    float normalizedDensity = (float) count / maxDensity;
                    int alpha = (int) (normalizedDensity * 255); // Scale from 0 to 255
                    Color adjustedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
                    
                    g2.setStroke(new BasicStroke(selectedOnly ? 2.0f : 1.0f));
                    g2.setColor(adjustedColor);
                    g2.draw(new Line2D.Double(p1, p2));
                }
                
                // Draw closing line if needed
                if (closeLoop && points.size() > 1) {
                    Point2D.Double first = points.get(0);
                    Point2D.Double last = points.get(points.size() - 1);
                    
                    if (axisGap <= 0.0) {
                        // No gap, draw direct line
                        LineSegment segment = new LineSegment(last, first);
                        int count = lineSegmentCounts.getOrDefault(segment, 1);
                        
                        float normalizedDensity = (float) count / maxDensity;
                        int alpha = (int) (normalizedDensity * 255);
                        Color adjustedColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
                        
                        g2.setStroke(new BasicStroke(selectedOnly ? 2.0f : 1.0f));
                        g2.setColor(adjustedColor);
                        g2.draw(new Line2D.Double(last, first));
                    } else {
                        // Calculate direction vector
                        double dx = first.x - last.x;
                        double dy = first.y - last.y;
                        double length = Math.sqrt(dx * dx + dy * dy);
                        
                        if (length > 0) {
                            // Normalize and scale by gap size
                            dx /= length;
                            dy /= length;
                            
                            // Calculate new endpoints with gap
                            Point2D.Double gapStart = new Point2D.Double(
                                last.x + dx * length * axisGap / 2,
                                last.y + dy * length * axisGap / 2
                            );
                            
                            Point2D.Double gapEnd = new Point2D.Double(
                                first.x - dx * length * axisGap / 2,
                                first.y - dy * length * axisGap / 2
                            );
                            
                            // Draw the two segments with appropriate opacity
                            LineSegment segment1 = new LineSegment(last, gapStart);
                            LineSegment segment2 = new LineSegment(gapEnd, first);
                            
                            int count1 = lineSegmentCounts.getOrDefault(segment1, 1);
                            int count2 = lineSegmentCounts.getOrDefault(segment2, 1);
                            
                            float normalizedDensity1 = (float) count1 / maxDensity;
                            float normalizedDensity2 = (float) count2 / maxDensity;
                            
                            Color adjustedColor1 = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 
                                                          (int)(normalizedDensity1 * 255));
                            Color adjustedColor2 = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 
                                                          (int)(normalizedDensity2 * 255));
                            
                            g2.setStroke(new BasicStroke(selectedOnly ? 2.0f : 1.0f));
                            g2.setColor(adjustedColor1);
                            g2.draw(new Line2D.Double(last, gapStart));
                            
                            g2.setColor(adjustedColor2);
                            g2.draw(new Line2D.Double(gapEnd, first));
                        }
                    }
                }

                // Draw the shapes at the points with full opacity
                // Use the original base color (full opacity) for vertices
                Color fullOpacityColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255);
                g2.setColor(fullOpacityColor);
                for (Point2D.Double point : points) {
                    Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                    g2.translate(point.x, point.y);
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }

        private void drawLinesWithThickness(Graphics2D g2, Map<LineSegment, Integer> lineSegmentCounts, int maxDensity, 
                                            boolean selectedOnly, int centerX, int centerY, int maxRadius) {
            List<Integer> rowsToProcess;
            if (selectedOnly) {
                rowsToProcess = selectedRows;
            } else {
                rowsToProcess = new ArrayList<>();
                for (int i = 0; i < data.get(0).size(); i++) {
                    if (!selectedRows.contains(i) && !hiddenRows.contains(i)) {
                        rowsToProcess.add(i);
                    }
                }
            }

            // THIS IS A HACK WE SHOULD FIX THIS TO DRAW THE SMALL SYMBOLS ON TOP OF THE LARGER ONES
            // Sort rows to process so that benign classes are drawn after malignant ones
            // This ensures benign classes appear on top of malignant ones
            Collections.sort(rowsToProcess, new Comparator<Integer>() {
                @Override
                public int compare(Integer row1, Integer row2) {
                    String class1 = classLabels.get(row1);
                    String class2 = classLabels.get(row2);
                    
                    // If one is "benign" and the other is "malignant", put benign last (on top)
                    if (class1.equalsIgnoreCase("benign") && class2.equalsIgnoreCase("malignant")) {
                        return 1; // benign comes after malignant
                    } else if (class1.equalsIgnoreCase("malignant") && class2.equalsIgnoreCase("benign")) {
                        return -1; // malignant comes before benign
                    } else {
                        return class1.compareTo(class2); // alphabetical for other classes
                    }
                }
            });

            for (int row : rowsToProcess) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;

                List<Point2D.Double> points = getPolylinePoints(row, centerX, centerY, maxRadius);
                Color baseColor = selectedOnly ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                
                // Draw lines with thickness based on density
                for (int i = 0; i < points.size() - 1; i++) {
                    Point2D.Double p1 = points.get(i);
                    Point2D.Double p2 = points.get(i + 1);
                    
                    LineSegment segment = new LineSegment(p1, p2);
                    int count = lineSegmentCounts.getOrDefault(segment, 1);
                    
                    // Calculate thickness based on density, starting from a minimum thickness
                    float normalizedDensity = (float) count / maxDensity;
                    float thickness = 0.5f + (normalizedDensity * 9.5f); // Scale from 0.5 to 10.0 pixels
                    if (selectedOnly) thickness += 1.0f; // Make selected lines slightly thicker
                    
                    g2.setStroke(new BasicStroke(thickness));
                    g2.setColor(baseColor);
                    g2.draw(new Line2D.Double(p1, p2));
                }
                
                // Draw closing line if needed
                if (closeLoop && points.size() > 1) {
                    Point2D.Double first = points.get(0);
                    Point2D.Double last = points.get(points.size() - 1);
                    
                    if (axisGap <= 0.0) {
                        // No gap, draw direct line
                        LineSegment segment = new LineSegment(last, first);
                        int count = lineSegmentCounts.getOrDefault(segment, 1);
                        
                        float normalizedDensity = (float) count / maxDensity;
                        float thickness = 0.5f + (normalizedDensity * 9.5f);
                        if (selectedOnly) thickness += 1.0f;
                        
                        g2.setStroke(new BasicStroke(thickness));
                        g2.setColor(baseColor);
                        g2.draw(new Line2D.Double(last, first));
                    } else {
                        // Calculate direction vector
                        double dx = first.x - last.x;
                        double dy = first.y - last.y;
                        double length = Math.sqrt(dx * dx + dy * dy);
                        
                        if (length > 0) {
                            // Normalize and scale by gap size
                            dx /= length;
                            dy /= length;
                            
                            // Calculate new endpoints with gap
                            Point2D.Double gapStart = new Point2D.Double(
                                last.x + dx * length * axisGap / 2,
                                last.y + dy * length * axisGap / 2
                            );
                            
                            Point2D.Double gapEnd = new Point2D.Double(
                                first.x - dx * length * axisGap / 2,
                                first.y - dy * length * axisGap / 2
                            );
                            
                            // Draw the two segments with appropriate thickness
                            LineSegment segment1 = new LineSegment(last, gapStart);
                            LineSegment segment2 = new LineSegment(gapEnd, first);
                            
                            int count1 = lineSegmentCounts.getOrDefault(segment1, 1);
                            int count2 = lineSegmentCounts.getOrDefault(segment2, 1);
                            
                            float normalizedDensity1 = (float) count1 / maxDensity;
                            float normalizedDensity2 = (float) count2 / maxDensity;
                            
                            float thickness1 = 0.5f + (normalizedDensity1 * 9.5f);
                            float thickness2 = 0.5f + (normalizedDensity2 * 9.5f);
                            if (selectedOnly) {
                                thickness1 += 1.0f;
                                thickness2 += 1.0f;
                            }
                            
                            g2.setColor(baseColor);
                            g2.setStroke(new BasicStroke(thickness1));
                            g2.draw(new Line2D.Double(last, gapStart));
                            
                            g2.setStroke(new BasicStroke(thickness2));
                            g2.draw(new Line2D.Double(gapEnd, first));
                        }
                    }
                }

                // Draw the shapes at the points
                g2.setColor(baseColor);
                for (Point2D.Double point : points) {
                    Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                    g2.translate(point.x, point.y);
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }
    }
}
