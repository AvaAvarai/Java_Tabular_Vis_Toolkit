package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import src.utils.ScreenshotUtils;
import src.utils.LegendUtils;

public class ShiftedPairedCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private int numPlots;
    private List<Integer> selectedRows;
    private JTable table;
    private Map<Integer, Point> plotOffsets; // Stores x,y offsets for each plot
    private Integer draggedPlot;
    private Point dragStartPoint;
    private Point dragStartOffset;
    private ShiftedPairedCoordinatesPanel plotPanel;
    private Map<String, Double> axisScales;
    private Map<String, Boolean> axisDirections;
    private double zoomLevel = 0.5;
    private Set<String> hiddenClasses; // Track which classes are hidden
    private boolean showSlopes = false; // Toggle for slope visualization
    private Map<Integer, Double> slopeValues; // Store calculated slopes for each line segment
    private JScrollPane mainScrollPane; // Store reference to main scroll pane
    private boolean showAttributeLabels = true; // Toggle for displaying attribute labels
    private Color backgroundColor;
    private boolean showPolylines = true; // Toggle for displaying polylines

    public ShiftedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, int numPlots, List<Integer> selectedRows, String datasetName, JTable table, Color backgroundColor) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.numPlots = numPlots;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.table = table;
        this.plotOffsets = new HashMap<>();
        this.draggedPlot = null;
        this.axisScales = new HashMap<>();
        this.axisDirections = new HashMap<>();
        this.hiddenClasses = new HashSet<>();
        this.slopeValues = new HashMap<>();

        // Initialize plot offsets and axis properties
        for (int i = 0; i < numPlots; i++) {
            plotOffsets.put(i, new Point(0, 0)); // Initialize with no offset
        }
        
        for (String attr : attributeNames) {
            axisScales.put(attr, 1.0);
            axisDirections.put(attr, true);
        }

        setTitle("Shifted Paired Coordinates");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Create horizontal control panel for axis settings
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add slope visualization toggle
        JToggleButton slopeToggle = new JToggleButton("Show Slopes");
        slopeToggle.addActionListener(e -> {
            showSlopes = slopeToggle.isSelected();
            plotPanel.repaint();
        });
        controlPanel.add(slopeToggle);
        
        // Add zoom slider
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setPreferredSize(new Dimension(100, 20));
        zoomSlider.addChangeListener(e -> {
            zoomLevel = zoomSlider.getValue() / 100.0;
            plotPanel.repaint();
        });
        zoomPanel.add(new JLabel("Zoom:"));
        zoomPanel.add(zoomSlider);
        controlPanel.add(zoomPanel);

        // Add attribute labels toggle
        JToggleButton attributeLabelsToggle = new JToggleButton("Show Labels");
        attributeLabelsToggle.addActionListener(e -> {
            showAttributeLabels = attributeLabelsToggle.isSelected();
            plotPanel.repaint();
        });
        controlPanel.add(attributeLabelsToggle);
        
        // Add polylines toggle
        JToggleButton polylinesToggle = new JToggleButton("Show Polylines");
        polylinesToggle.setSelected(true); // Default to showing polylines
        polylinesToggle.addActionListener(e -> {
            showPolylines = polylinesToggle.isSelected();
            plotPanel.repaint();
        });
        controlPanel.add(polylinesToggle);

        // Add axis controls for each plot (pair of attributes)
        for (int i = 0; i < (attributeNames.size() + 1) / 2; i++) {
            final int plotIndex = i;
            int attr1Index = i * 2;
            int attr2Index = i * 2 + 1;
            
            String attr1Name = attributeNames.get(attr1Index);
            String attr2Name = (attr2Index < attributeNames.size()) ? attributeNames.get(attr2Index) : attr1Name;
            
            JPanel plotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            plotPanel.setBorder(BorderFactory.createTitledBorder("Plot " + (i + 1) + ": " + attr1Name + " / " + attr2Name));
            
            // Add scale slider for first attribute
            JSlider scale1Slider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
            scale1Slider.setPreferredSize(new Dimension(100, 20));
            scale1Slider.addChangeListener(e -> {
                axisScales.put(attr1Name, scale1Slider.getValue() / 100.0);
                plotPanel.repaint();
            });
            plotPanel.add(new JLabel(attr1Name + " Scale:"));
            plotPanel.add(scale1Slider);
            
            // Add direction toggle for first attribute
            JToggleButton direction1Toggle = new JToggleButton("\u2191");
            direction1Toggle.addActionListener(e -> {
                axisDirections.put(attr1Name, !direction1Toggle.isSelected());
                direction1Toggle.setText(direction1Toggle.isSelected() ? "\u2193" : "\u2191");
                plotPanel.repaint();
            });
            plotPanel.add(direction1Toggle);
            
            // Add scale slider for second attribute (if different)
            if (attr2Index < attributeNames.size()) {
                JSlider scale2Slider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
                scale2Slider.setPreferredSize(new Dimension(100, 20));
                scale2Slider.addChangeListener(e -> {
                    axisScales.put(attr2Name, scale2Slider.getValue() / 100.0);
                    plotPanel.repaint();
                });
                plotPanel.add(new JLabel(attr2Name + " Scale:"));
                plotPanel.add(scale2Slider);
                
                // Add direction toggle for second attribute
                JToggleButton direction2Toggle = new JToggleButton("\u2191");
                direction2Toggle.addActionListener(e -> {
                    axisDirections.put(attr2Name, !direction2Toggle.isSelected());
                    direction2Toggle.setText(direction2Toggle.isSelected() ? "\u2193" : "\u2191");
                    plotPanel.repaint();
                });
                plotPanel.add(direction2Toggle);
            }
            
            // Add X position slider for this plot
            JSlider xPosSlider = new JSlider(JSlider.HORIZONTAL, -3000, 3000, 0);
            xPosSlider.setPreferredSize(new Dimension(100, 20));
            xPosSlider.addChangeListener(e -> {
                Point currentOffset = plotOffsets.getOrDefault(plotIndex, new Point(0, 0));
                plotOffsets.put(plotIndex, new Point(xPosSlider.getValue(), currentOffset.y));
                ShiftedPairedCoordinatesPlot.this.plotPanel.repaint();
            });
            plotPanel.add(new JLabel("X:"));
            plotPanel.add(xPosSlider);
            
            // Add Y position slider for this plot
            JSlider yPosSlider = new JSlider(JSlider.HORIZONTAL, -3000, 3000, 0);
            yPosSlider.setPreferredSize(new Dimension(100, 20));
            yPosSlider.addChangeListener(e -> {
                Point currentOffset = plotOffsets.getOrDefault(plotIndex, new Point(0, 0));
                plotOffsets.put(plotIndex, new Point(currentOffset.x, yPosSlider.getValue()));
                ShiftedPairedCoordinatesPlot.this.plotPanel.repaint();
            });
            plotPanel.add(new JLabel("Y:"));
            plotPanel.add(yPosSlider);
            
            controlPanel.add(plotPanel);
        }

        JScrollPane controlScroll = new JScrollPane(controlPanel);
        controlScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        controlScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        controlScroll.setPreferredSize(new Dimension(0, 100));
        mainPanel.add(controlScroll, BorderLayout.NORTH);

        plotPanel = new ShiftedPairedCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(numPlots * 250, 800));

        // Add mouse listeners for dragging
        plotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int plotWidth = getWidth() / numPlots;
                draggedPlot = e.getX() / plotWidth;
                dragStartPoint = e.getPoint();
                dragStartOffset = plotOffsets.get(draggedPlot);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedPlot = null;
                dragStartPoint = null;
                dragStartOffset = null;
            }
        });

        plotPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedPlot != null && dragStartPoint != null && dragStartOffset != null) {
                    // Allow both x and y movement
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    plotOffsets.put(draggedPlot, new Point(dragStartOffset.x + dx, dragStartOffset.y + dy));
                    plotPanel.repaint();
                }
            }
        });

        // Add optimize button and screenshot button
        JButton optimizeButton = new JButton("Optimize Axes");
        optimizeButton.addActionListener(e -> optimizeAxesPlacement());
        
        JButton screenshotButton = new JButton("Take Screenshot");
        screenshotButton.addActionListener(e -> {
            ScreenshotUtils.captureAndSaveScreenshot(mainScrollPane, "ShiftedPairedCoordinates", datasetName);
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(optimizeButton);
        buttonPanel.add(screenshotButton);
        controlPanel.add(buttonPanel);

        mainScrollPane = new JScrollPane(plotPanel);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add spacebar shortcut to save screenshot of whole plot
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        getRootPane().getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(mainScrollPane, "ShiftedPairedCoordinates", datasetName);
            }
        });

        setFocusable(true);
        requestFocusInWindow();

        mainPanel.add(mainScrollPane, BorderLayout.CENTER);
        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void optimizeAxesPlacement() {
        // Calculate correlations and class separations between all pairs of attributes
        List<AttributePair> pairs = new ArrayList<>();
        for (int i = 0; i < attributeNames.size(); i++) {
            for (int j = i + 1; j < attributeNames.size(); j++) {
                double correlation = calculateCorrelation(data.get(i), data.get(j));
                double classSeparation = calculateClassSeparation(data.get(i), data.get(j));
                pairs.add(new AttributePair(i, j, correlation, classSeparation));
            }
        }

        // Sort pairs by absolute correlation strength and class separation
        Collections.sort(pairs, (a, b) -> Double.compare(Math.abs(b.correlation) + b.classSeparation, 
                                                       Math.abs(a.correlation) + a.classSeparation));

        // Reset plot offsets and create a list of used attributes
        plotOffsets.clear();
        Set<Integer> usedAttributes = new HashSet<>();
        List<Integer> attributeOrder = new ArrayList<>();

        // Place pairs with strongest relationships first
        for (AttributePair pair : pairs) {
            if (!usedAttributes.contains(pair.attr1) && !usedAttributes.contains(pair.attr2)) {
                attributeOrder.add(pair.attr1);
                attributeOrder.add(pair.attr2);
                usedAttributes.add(pair.attr1);
                usedAttributes.add(pair.attr2);
            }
        }

        // Add any remaining attributes
        for (int i = 0; i < attributeNames.size(); i++) {
            if (!usedAttributes.contains(i)) {
                attributeOrder.add(i);
            }
        }

        // If odd number of attributes, duplicate the last one
        if (attributeOrder.size() % 2 != 0) {
            attributeOrder.add(attributeOrder.get(attributeOrder.size() - 1));
        }

        // Optimize scales, directions, and positions based on relationships
        for (int i = 0; i < attributeOrder.size() - 1; i += 2) {
            int attr1 = attributeOrder.get(i);
            int attr2 = attributeOrder.get(i + 1);
            int plotIndex = i / 2;

            // Calculate optimal position
            int yOffset = (int)(50 * Math.sin(plotIndex * Math.PI / 2));
            plotOffsets.put(plotIndex, new Point(0, yOffset));

            // Set scales based on data distribution and relationships
            String attrName1 = attributeNames.get(attr1);
            String attrName2 = attributeNames.get(attr2);
            double correlation = calculateCorrelation(data.get(attr1), data.get(attr2));
            double separation = calculateClassSeparation(data.get(attr1), data.get(attr2));

            // Scale based on both correlation and class separation from 0.0 to 1.0
            double scale = Math.min(1.0, Math.abs(correlation) + separation);
            axisScales.put(attrName1, scale);
            axisScales.put(attrName2, scale);

            // Set directions based on correlation sign
            axisDirections.put(attrName1, correlation >= 0);
            axisDirections.put(attrName2, true);
        }

        plotPanel.repaint();
        
        // Request focus after optimization to ensure keyboard shortcuts still work
        requestFocusInWindow();
    }

    private double calculateCorrelation(List<Double> x, List<Double> y) {
        double sumX = 0, sumY = 0, sumXY = 0;
        double sumX2 = 0, sumY2 = 0;
        int n = x.size();

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

    private double calculateClassSeparation(List<Double> x, List<Double> y) {
        // Calculate mean and variance for each class
        Map<String, List<Double>> xByClass = new HashMap<>();
        Map<String, List<Double>> yByClass = new HashMap<>();
        
        for (int i = 0; i < x.size(); i++) {
            String classLabel = classLabels.get(i);
            xByClass.computeIfAbsent(classLabel, k -> new ArrayList<>()).add(x.get(i));
            yByClass.computeIfAbsent(classLabel, k -> new ArrayList<>()).add(y.get(i));
        }

        double totalSeparation = 0;
        int comparisons = 0;

        // Calculate separation between each pair of classes
        List<String> classes = new ArrayList<>(xByClass.keySet());
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                String class1 = classes.get(i);
                String class2 = classes.get(j);
                
                double xSep = calculateMeanSeparation(xByClass.get(class1), xByClass.get(class2));
                double ySep = calculateMeanSeparation(yByClass.get(class1), yByClass.get(class2));
                
                totalSeparation += Math.sqrt(xSep * xSep + ySep * ySep);
                comparisons++;
            }
        }

        return comparisons > 0 ? totalSeparation / comparisons : 0;
    }

    private double calculateMeanSeparation(List<Double> values1, List<Double> values2) {
        double mean1 = values1.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double mean2 = values2.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double var1 = calculateVariance(values1, mean1);
        double var2 = calculateVariance(values2, mean2);
        
        // Return normalized separation
        return Math.abs(mean1 - mean2) / Math.sqrt(var1 + var2 + 1e-10);
    }

    private double calculateVariance(List<Double> values, double mean) {
        return values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
    }

    private static class AttributePair {
        int attr1;
        int attr2;
        double correlation;
        double classSeparation;

        AttributePair(int attr1, int attr2, double correlation, double classSeparation) {
            this.attr1 = attr1;
            this.attr2 = attr2;
            this.correlation = correlation;
            this.classSeparation = classSeparation;
        }
    }

    private class ShiftedPairedCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20;

        public ShiftedPairedCoordinatesPanel() {
            setBackground(backgroundColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(zoomLevel, zoomLevel);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, (int)(getWidth()/zoomLevel), (int)(getHeight()/zoomLevel));

            String title = "Shifted Paired Coordinates Plot";
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            g2.setColor(backgroundColor);
            g2.fillRect(0, titleHeight + TITLE_PADDING, (int)(getWidth()/zoomLevel), (int)((getHeight() - titleHeight - TITLE_PADDING)/zoomLevel));

            int plotWidth = getWidth() / numPlots;
            int plotHeight = getHeight() - titleHeight - TITLE_PADDING - 50;

            // Draw axes first
            for (int i = 0; i < numPlots; i++) {
                Point offset = plotOffsets.get(i);
                int x = i * plotWidth + offset.x;
                int y = titleHeight + TITLE_PADDING + 10 + offset.y;
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }
                drawAxesAndLabels(g2, x, y, plotWidth, plotHeight, attributeNames.get(attrIndex1), attributeNames.get(attrIndex2));
            }

            // Draw non-highlighted rows first
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!selectedRows.contains(row) && !hiddenClasses.contains(classLabel)) {
                    drawRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                    drawScatterPlot(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                }
            }

            // Draw highlighted rows last
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (selectedRows.contains(row) && !hiddenClasses.contains(classLabel)) {
                    drawHighlightedRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                    drawScatterPlot(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                }
            }
        }

        private void drawAxesAndLabels(Graphics2D g2, int x, int y, int width, int height, String xLabel, String yLabel) {
            int plotSize = Math.min(width, height) - 40;
            int plotX = x + 40;
            int plotY = y + 20;

            double xScale = axisScales.get(xLabel);
            double yScale = axisScales.get(yLabel);
            boolean xDirection = axisDirections.get(xLabel);
            boolean yDirection = axisDirections.get(yLabel);

            g2.setColor(Color.BLACK);
            g2.drawLine(plotX, plotY + plotSize, plotX, plotY + plotSize - (int)(plotSize * yScale)); // Draw vertical axis growing up from origin
            g2.drawLine(plotX, plotY + plotSize, plotX + (int)(plotSize * xScale), plotY + plotSize);
            g2.drawLine(plotX, plotY, plotX + plotSize, plotY);
            g2.drawLine(plotX + plotSize, plotY, plotX + plotSize, plotY + plotSize);

            if (showAttributeLabels) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g2.setColor(Color.BLACK);
                g2.drawString(xLabel + (xDirection ? " \u2191" : " \u2193"), plotX + plotSize / 2, plotY + plotSize + 20);
                g2.drawString(yLabel + (yDirection ? " \u2191" : " \u2193"), plotX - g2.getFontMetrics().stringWidth(yLabel) / 2, plotY - 10);
            }
        }

        private void drawRow(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            drawRowHelper(g2, row, plotY, plotWidth, plotHeight, false);
        }

        private void drawHighlightedRow(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            drawRowHelper(g2, row, plotY, plotWidth, plotHeight, true);
        }

        private void drawRowHelper(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight, boolean isHighlighted) {
            int numAttributes = attributeNames.size();
            String classLabel = classLabels.get(row);
            Color color = isHighlighted ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            Stroke originalStroke = g2.getStroke();
            g2.setStroke(isHighlighted ? new BasicStroke(2) : originalStroke);
        
            for (int i = 0; i < numPlots; i++) {
                Point offset = plotOffsets.get(i);
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= numAttributes) {
                    attrIndex2 = attrIndex1;
                }
        
                String attr1 = attributeNames.get(attrIndex1);
                String attr2 = attributeNames.get(attrIndex2);
                double scale1 = axisScales.get(attr1);
                double scale2 = axisScales.get(attr2);
                boolean dir1 = axisDirections.get(attr1);
                boolean dir2 = axisDirections.get(attr2);
        
                int plotX1 = i * plotWidth + 40 + offset.x;
                int plotSize = Math.min(plotWidth, plotHeight) - 40;
        
                double normX1 = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY1 = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));
                
                if (!dir1) normX1 = 1 - normX1;
                if (!dir2) normY1 = 1 - normY1;
        
                int x1 = plotX1 + (int) (plotSize * normX1 * scale1);
                int y1 = plotY + plotSize - (int) (plotSize * normY1 * scale2) + offset.y + 20;
        
                if (i + 1 < numPlots) {
                    Point nextOffset = plotOffsets.get(i + 1);
                    int nextAttrIndex1 = (i + 1) * 2;
                    int nextAttrIndex2 = (i + 1) * 2 + 1;
                    if (nextAttrIndex2 >= numAttributes) {
                        nextAttrIndex2 = nextAttrIndex1;
                    }
        
                    String nextAttr1 = attributeNames.get(nextAttrIndex1);
                    String nextAttr2 = attributeNames.get(nextAttrIndex2);
                    double nextScale1 = axisScales.get(nextAttr1);
                    double nextScale2 = axisScales.get(nextAttr2);
                    boolean nextDir1 = axisDirections.get(nextAttr1);
                    boolean nextDir2 = axisDirections.get(nextAttr2);
        
                    int plotX2 = (i + 1) * plotWidth + 40 + nextOffset.x;
        
                    double normX2 = (data.get(nextAttrIndex1).get(row) - getMin(data.get(nextAttrIndex1))) / (getMax(data.get(nextAttrIndex1)) - getMin(data.get(nextAttrIndex1)));
                    double normY2 = (data.get(nextAttrIndex2).get(row) - getMin(data.get(nextAttrIndex2))) / (getMax(data.get(nextAttrIndex2)) - getMin(data.get(nextAttrIndex2)));
                    
                    if (!nextDir1) normX2 = 1 - normX2;
                    if (!nextDir2) normY2 = 1 - normY2;
        
                    int x2 = plotX2 + (int) (plotSize * normX2 * nextScale1);
                    int y2 = plotY + plotSize - (int) (plotSize * normY2 * nextScale2) + nextOffset.y + 20;
                    
                    // Calculate slope between points
                    double slope = (y2 - y1) / (double)(x2 - x1);
                    int lineKey = row * numPlots + i;
                    slopeValues.put(lineKey, slope);
                    
                    if (showPolylines) {
                        if (showSlopes) {
                            // Map slope to color (red for positive, blue for negative)
                            float hue = slope > 0 ? 0.0f : 0.6f; // Red or blue
                            float saturation = Math.min(1.0f, Math.abs((float)slope) / 5.0f);
                            Color slopeColor = Color.getHSBColor(hue, saturation, 1.0f);
                            g2.setColor(slopeColor);
                        } else {
                            g2.setColor(color);
                        }
                        
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
            }
            g2.setStroke(originalStroke);
        }

        private void drawScatterPlot(Graphics2D g2, int row, int plotY, int plotWidth, int plotHeight) {
            for (int i = 0; i < numPlots; i++) {
                Point offset = plotOffsets.get(i);
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }

                String attr1 = attributeNames.get(attrIndex1);
                String attr2 = attributeNames.get(attrIndex2);
                double scale1 = axisScales.get(attr1);
                double scale2 = axisScales.get(attr2);
                boolean dir1 = axisDirections.get(attr1);
                boolean dir2 = axisDirections.get(attr2);

                int plotX = i * plotWidth + 40 + offset.x;
                int plotSize = Math.min(plotWidth, plotHeight) - 40;

                double normX = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));
                
                if (!dir1) normX = 1 - normX;
                if (!dir2) normY = 1 - normY;

                int px = plotX + (int) (plotSize * normX * scale1);
                int py = plotY + plotSize - (int) (plotSize * normY * scale2) + offset.y + 20;

                String classLabel = classLabels.get(row);
                Color color = selectedRows.contains(row) ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));

                g2.setColor(color);
                g2.translate(px, py);
                g2.fill(shape);
                g2.translate(-px, -py);
            }
        }

        private double getMin(List<Double> data) {
            return data.stream().min(Double::compare).orElse(Double.NaN);
        }

        private double getMax(List<Double> data) {
            return data.stream().max(Double::compare).orElse(Double.NaN);
        }
    }
}