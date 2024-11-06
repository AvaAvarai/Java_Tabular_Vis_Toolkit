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
import java.util.Comparator;
import src.utils.ScreenshotUtils;

public class ShiftedPairedCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private int numPlots;
    private List<Integer> selectedRows;
    private JTable table;
    private Map<Integer, Point> plotOffsets;
    private Integer draggedPlot;
    private Point dragStartPoint;
    private Point dragStartOffset;
    private ShiftedPairedCoordinatesPanel plotPanel;
    private Map<String, Double> axisScales; // Store scale factor for each axis
    private Map<String, Boolean> axisDirections; // Store direction for each axis (true = normal, false = inverted)
    private double zoomLevel = 1.0; // Added zoom level

    public ShiftedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, int numPlots, List<Integer> selectedRows, String datasetName, JTable table) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.numPlots = numPlots;
        this.selectedRows = selectedRows;
        this.table = table;
        this.plotOffsets = new HashMap<>();
        this.draggedPlot = null;
        this.axisScales = new HashMap<>();
        this.axisDirections = new HashMap<>();

        // Initialize plot offsets and axis properties
        for (int i = 0; i < numPlots; i++) {
            plotOffsets.put(i, new Point(0, 0));
        }
        
        for (String attr : attributeNames) {
            axisScales.put(attr, 1.0);
            axisDirections.put(attr, true);
        }

        setTitle("Shifted Paired Coordinates");
        setSize(1000, 800); // Increased size to accommodate controls
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Create horizontal control panel for axis settings
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add zoom slider
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Zoom"));
        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setPreferredSize(new Dimension(100, 20));
        zoomSlider.addChangeListener(e -> {
            zoomLevel = zoomSlider.getValue() / 100.0;
            plotPanel.setPreferredSize(new Dimension((int)(numPlots * 250 * zoomLevel), (int)(800 * zoomLevel)));
            plotPanel.revalidate();
            plotPanel.repaint();
        });
        zoomPanel.add(new JLabel("Zoom:"));
        zoomPanel.add(zoomSlider);
        controlPanel.add(zoomPanel);
        
        // Add axis controls for each attribute
        for (String attr : attributeNames) {
            JPanel attrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            attrPanel.setBorder(BorderFactory.createTitledBorder(attr));
            
            // Add scale slider
            JSlider scaleSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
            scaleSlider.setPreferredSize(new Dimension(100, 20));
            scaleSlider.addChangeListener(e -> {
                axisScales.put(attr, scaleSlider.getValue() / 100.0);
                plotPanel.repaint();
            });
            attrPanel.add(new JLabel("Scale:"));
            attrPanel.add(scaleSlider);
            
            // Add direction toggle
            JToggleButton directionToggle = new JToggleButton("↑");
            directionToggle.addActionListener(e -> {
                axisDirections.put(attr, !directionToggle.isSelected());
                directionToggle.setText(directionToggle.isSelected() ? "↓" : "↑");
                plotPanel.repaint();
            });
            attrPanel.add(directionToggle);
            
            controlPanel.add(attrPanel);
        }

        JScrollPane controlScroll = new JScrollPane(controlPanel);
        controlScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        controlScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        controlScroll.setPreferredSize(new Dimension(0, 100));
        mainPanel.add(controlScroll, BorderLayout.NORTH);

        plotPanel = new ShiftedPairedCoordinatesPanel();
        int plotHeight = 800;
        int plotWidth = numPlots * 250;

        plotPanel.setPreferredSize(new Dimension(plotWidth, plotHeight));

        // Add mouse listeners for dragging
        plotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int plotWidth = (int)(getWidth() / numPlots * zoomLevel);
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
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    plotOffsets.put(draggedPlot, new Point(dragStartOffset.x + dx, dragStartOffset.y + dy));
                    plotPanel.repaint();
                }
            }
        });

        // Add optimize button
        JButton optimizeButton = new JButton("Optimize Axes");
        optimizeButton.addActionListener(e -> optimizeAxesPlacement());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(optimizeButton);
        controlPanel.add(buttonPanel);

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ShiftedPairedCoordinates", datasetName);
            }
        });

        setFocusable(true);
        requestFocusInWindow();

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void optimizeAxesPlacement() {
        // Calculate correlations between all pairs of attributes
        List<AttributePair> pairs = new ArrayList<>();
        for (int i = 0; i < attributeNames.size(); i++) {
            for (int j = i + 1; j < attributeNames.size(); j++) {
                double correlation = calculateCorrelation(data.get(i), data.get(j));
                double classSeparation = calculateClassSeparation(data.get(i), data.get(j));
                pairs.add(new AttributePair(i, j, correlation, classSeparation));
            }
        }

        // Sort pairs by absolute correlation strength
        Collections.sort(pairs, (a, b) -> Double.compare(Math.abs(b.correlation) + b.classSeparation, 
                                                       Math.abs(a.correlation) + a.classSeparation));

        // Reset plot offsets
        plotOffsets.clear();
        for (int i = 0; i < numPlots; i++) {
            plotOffsets.put(i, new Point(0, 0));
        }

        // Optimize scales and directions based on correlations
        for (AttributePair pair : pairs) {
            String attr1 = attributeNames.get(pair.attr1);
            String attr2 = attributeNames.get(pair.attr2);

            // Set axis directions based on correlation sign
            if (pair.correlation < 0) {
                axisDirections.put(attr1, !axisDirections.get(attr1));
            }

            // Set axis scales based on correlation strength and class separation
            double scale = Math.min(2.0, Math.abs(pair.correlation) + pair.classSeparation);
            axisScales.put(attr1, scale);
            axisScales.put(attr2, scale);
        }

        plotPanel.repaint();
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

    private class ShiftedPairedCoordinatesPanel extends JPanel {
        private static final int TITLE_PADDING = 20;

        public ShiftedPairedCoordinatesPanel() {
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

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            String title = "Shifted Paired Coordinates Plot";
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, titleHeight + TITLE_PADDING, getWidth(), getHeight() - titleHeight - TITLE_PADDING);

            int plotWidth = (int)(getWidth() / numPlots * zoomLevel);
            int plotHeight = (int)((getHeight() - titleHeight - TITLE_PADDING - 50) * zoomLevel);

            for (int i = 0; i < numPlots; i++) {
                Point offset = plotOffsets.get(i);
                int x = (int)(i * plotWidth + offset.x);
                int y = titleHeight + TITLE_PADDING + 10 + offset.y;
                int attrIndex1 = i * 2;
                int attrIndex2 = (i * 2) + 1;
                if (attrIndex2 >= data.size()) {
                    attrIndex2 = attrIndex1;
                }
                drawAxesAndLabels(g2, x, y, plotWidth, plotHeight, attributeNames.get(attrIndex1), attributeNames.get(attrIndex2));
            }

            // Draw rows in the order they are listed in the table
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i); // Use the current sort order
                drawRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                drawScatterPlot(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
            }

            // Draw highlighted rows on top
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                if (selectedRows.contains(row)) {
                    drawHighlightedRow(g2, row, titleHeight + TITLE_PADDING + 10, plotWidth, plotHeight);
                }
            }
        }

        private void drawAxesAndLabels(Graphics2D g2, int x, int y, int width, int height, String xLabel, String yLabel) {
            int plotSize = (int)(Math.min(width, height) - 40);
            int plotX = x + 40;
            int plotY = y + 20;

            double xScale = axisScales.get(xLabel);
            double yScale = axisScales.get(yLabel);
            boolean xDirection = axisDirections.get(xLabel);
            boolean yDirection = axisDirections.get(yLabel);

            g2.setColor(Color.BLACK);
            g2.drawLine(plotX, plotY + plotSize, plotX, plotY + plotSize - (int)(plotSize * yScale)); // Draw vertical axis growing up from origin
            g2.drawLine(plotX, plotY + plotSize, plotX + (int)(plotSize * xScale), plotY + plotSize);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2.setColor(Color.BLACK);
            g2.drawString(xLabel + (xDirection ? " ↑" : " ↓"), plotX + plotSize / 2, plotY + plotSize + 20);
            g2.drawString(yLabel + (yDirection ? " ↑" : " ↓"), plotX - g2.getFontMetrics().stringWidth(yLabel) / 2, plotY - 10);
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
        
                int plotX1 = (int)(i * plotWidth * zoomLevel) + 40 + offset.x;
                int plotSize = (int)(Math.min(plotWidth, plotHeight) - 40);
        
                double normX1 = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY1 = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));
                
                if (!dir1) normX1 = 1 - normX1;
                if (!dir2) normY1 = 1 - normY1;
        
                int x1 = plotX1 + (int) (plotSize * normX1 * scale1);
                int y1 = plotY + plotSize - (int) (plotSize * normY1 * scale2);
        
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
        
                    int plotX2 = (int)((i + 1) * plotWidth * zoomLevel) + 40 + nextOffset.x;
        
                    double normX2 = (data.get(nextAttrIndex1).get(row) - getMin(data.get(nextAttrIndex1))) / (getMax(data.get(nextAttrIndex1)) - getMin(data.get(nextAttrIndex1)));
                    double normY2 = (data.get(nextAttrIndex2).get(row) - getMin(data.get(nextAttrIndex2))) / (getMax(data.get(nextAttrIndex2)) - getMin(data.get(nextAttrIndex2)));
                    
                    if (!nextDir1) normX2 = 1 - normX2;
                    if (!nextDir2) normY2 = 1 - normY2;
        
                    int x2 = plotX2 + (int) (plotSize * normX2 * nextScale1);
                    int y2 = plotY + plotSize - (int) (plotSize * normY2 * nextScale2);
                    
                    g2.setColor(color);
                    g2.drawLine(x1, y1, x2, y2);
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

                int plotX = (int)(i * plotWidth * zoomLevel) + 40 + offset.x;
                int plotSize = (int)(Math.min(plotWidth, plotHeight) - 40);

                double normX = (data.get(attrIndex1).get(row) - getMin(data.get(attrIndex1))) / (getMax(data.get(attrIndex1)) - getMin(data.get(attrIndex1)));
                double normY = (data.get(attrIndex2).get(row) - getMin(data.get(attrIndex2))) / (getMax(data.get(attrIndex2)) - getMin(data.get(attrIndex2)));
                
                if (!dir1) normX = 1 - normX;
                if (!dir2) normY = 1 - normY;

                int px = plotX + (int) (plotSize * normX * scale1);
                int py = plotY + plotSize - (int) (plotSize * normY * scale2);

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
