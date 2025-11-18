package src.plots;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import src.utils.LegendUtils;

public class CollocatedPairedCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private JTable table;
    private Set<String> hiddenClasses;
    private boolean normalizeVectors = true;
    private double unitVectorMultiplier = 1.0; // Default multiplier
    private CollocatedPairedCoordinatesPanel plotPanel;
    private JLabel multiplierLabel; // Label to show current multiplier value
    private JPanel sliderPanel; // Store reference to the slider panel
    private Color backgroundColor;
    private float polylineThickness;
    private boolean drawBoxes = false;
    private boolean hideContainedCases = false;

    public CollocatedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, String datasetName, JTable table, Color backgroundColor, float polylineThickness) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.polylineThickness = polylineThickness;
        this.table = table;
        this.hiddenClasses = new HashSet<>();

        setTitle("Collocated Paired Coordinates Plot");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        
        // Add slider panel for unit vector length control
        sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sliderPanel.add(new JLabel("Case Scaling: "));
        
        // Create slider for controlling unit vector length in a more focused range (0-0.25)
        JSlider unitVectorSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        unitVectorSlider.setMajorTickSpacing(25);
        unitVectorSlider.setMinorTickSpacing(5);
        unitVectorSlider.setPaintTicks(true);
        unitVectorSlider.setPaintLabels(true);
        
        // Create custom labels for the slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0.00"));
        labelTable.put(25, new JLabel("0.25"));
        labelTable.put(50, new JLabel("0.50"));
        labelTable.put(75, new JLabel("0.75"));
        labelTable.put(100, new JLabel("1.00"));
        unitVectorSlider.setLabelTable(labelTable);
        
        multiplierLabel = new JLabel(String.format("%.2fx", unitVectorSlider.getValue() / 100.0));
        
        unitVectorSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Map the 0-100 range to 0-1.0 for full range from zero to original length
                unitVectorMultiplier = unitVectorSlider.getValue() / 100.0;
                multiplierLabel.setText(String.format("%.2fx", unitVectorMultiplier));
                if (plotPanel != null) {
                    plotPanel.repaint();
                }
            }
        });
        
        sliderPanel.add(unitVectorSlider);
        sliderPanel.add(multiplierLabel);
        controlPanel.add(sliderPanel);
        
        // Add "Draw Boxes" button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton drawBoxesButton = new JButton("Draw Boxes");
        drawBoxesButton.addActionListener(e -> {
            drawBoxes = !drawBoxes;
            drawBoxesButton.setText(drawBoxes ? "Hide Boxes" : "Draw Boxes");
            if (plotPanel != null) {
                plotPanel.repaint();
            }
        });
        buttonPanel.add(drawBoxesButton);
        
        // Add "Hide Contained Cases" button
        JButton hideContainedButton = new JButton("Hide Contained Cases");
        hideContainedButton.addActionListener(e -> {
            hideContainedCases = !hideContainedCases;
            hideContainedButton.setText(hideContainedCases ? "Show Contained Cases" : "Hide Contained Cases");
            if (plotPanel != null) {
                plotPanel.repaint();
            }
        });
        buttonPanel.add(hideContainedButton);
        controlPanel.add(buttonPanel);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        plotPanel = new CollocatedPairedCoordinatesPanel();
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private class CollocatedPairedCoordinatesPanel extends JPanel {
        // Cached values for vector normalization
        private double minVectorLength = Double.MAX_VALUE;
        private double maxVectorLength = Double.MIN_VALUE;
        
        public CollocatedPairedCoordinatesPanel() {
            setBackground(backgroundColor);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null || data.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);  

            int width = getWidth();
            int height = getHeight();
            int padding = 50;
            int plotSize = Math.min(width, height) - 2 * padding;

            g2.setColor(Color.BLACK);
            g2.drawRect(padding, padding, plotSize, plotSize);
            
            // Calculate min/max vector lengths if normalizing
            if (normalizeVectors) {
                calculateVectorLengthRange();
            }

            // Get set of rows that should be hidden if hideContainedCases is enabled
            Set<Integer> hiddenRows = new HashSet<>();
            if (hideContainedCases && drawBoxes) {
                hiddenRows = getContainedRows(padding, plotSize);
            }

            // Draw non-selected lines first
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel) && !selectedRows.contains(row) && !hiddenRows.contains(row)) {
                    drawPolyline(g2, row, padding, plotSize, false);
                }
            }

            // Draw selected lines last to highlight them
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel) && selectedRows.contains(row) && !hiddenRows.contains(row)) {
                    drawPolyline(g2, row, padding, plotSize, true);
                }
            }
            
            // Draw boxes if enabled
            if (drawBoxes) {
                drawBoundingBoxes(g2, padding, plotSize);
            }
        }
        
        private void calculateVectorLengthRange() {
            minVectorLength = Double.MAX_VALUE;
            maxVectorLength = Double.MIN_VALUE;
            
            for (int row = 0; row < data.get(0).size(); row++) {
                List<Point> points = new ArrayList<>();
                int numAttributes = attributeNames.size();

                // Loop over attributes in pairs, but handle odd number of attributes
                for (int i = 0; i < numAttributes; i += 2) {
                    double xValue = normalize(data.get(i).get(row), i);
                    
                    // If this is the last attribute and it's odd, duplicate the value for y
                    double yValue;
                    if (i == numAttributes - 1) {
                        // Odd number of attributes, duplicate the last one
                        yValue = xValue;
                    } else {
                        // Normal case, use the next attribute
                        yValue = normalize(data.get(i + 1).get(row), i + 1);
                    }
                    
                    int x = (int) (xValue * 100); // Use a scale factor for calculations
                    int y = (int) (yValue * 100);
                    points.add(new Point(x, y));
                }
                
                if (points.size() < 2) continue;
                
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    double vectorLength = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
                    
                    minVectorLength = Math.min(minVectorLength, vectorLength);
                    maxVectorLength = Math.max(maxVectorLength, vectorLength);
                }
            }
            
            // Ensure we don't divide by zero if all vectors are the same length
            if (Math.abs(maxVectorLength - minVectorLength) < 0.0001) {
                maxVectorLength = minVectorLength + 1.0;
            }
        }

        private void drawPolyline(Graphics2D g2, int row, int padding, int plotSize, boolean isSelected) {
            List<Point> arrowStarts = new ArrayList<>();
            List<Point> arrowEnds = new ArrayList<>();
            drawPolylineWithPoints(g2, row, padding, plotSize, isSelected, arrowStarts, arrowEnds);
        }
        
        private void drawPolylineWithPoints(Graphics2D g2, int row, int padding, int plotSize, boolean isSelected, List<Point> arrowStarts, List<Point> arrowEnds) {
            List<Point> points = new ArrayList<>();
            int numAttributes = attributeNames.size();

            // Loop over attributes in pairs, but handle odd number of attributes
            for (int i = 0; i < numAttributes; i += 2) {
                double xValue = normalize(data.get(i).get(row), i);
                
                // If this is the last attribute and it's odd, duplicate the value for y
                double yValue;
                if (i == numAttributes - 1) {
                    // Odd number of attributes, duplicate the last one
                    yValue = xValue;
                } else {
                    // Normal case, use the next attribute
                    yValue = normalize(data.get(i + 1).get(row), i + 1);
                }
                
                int x = padding + (int) (xValue * plotSize);
                int y = padding + (int) ((1 - yValue) * plotSize);
                points.add(new Point(x, y));
            }

            if (points.size() < 2) return;

            String classLabel = classLabels.get(row);
            Color color = isSelected ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(polylineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            
            if (normalizeVectors) {
                // For normalized vectors, create a connected chain
                Point currentStartPoint = points.get(0); // Start with the first point
                
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    
                    // Calculate the vector direction and length
                    double dx = p2.x - p1.x;
                    double dy = p2.y - p1.y;
                    double vectorLength = Math.sqrt(dx * dx + dy * dy);
                    
                    // Skip drawing if vector is too small
                    if (vectorLength < 0.001) continue;
                    
                    // Track arrow start point
                    arrowStarts.add(new Point(currentStartPoint.x, currentStartPoint.y));
                    
                    // Use original direction but scale by the multiplier directly
                    // We're not using min/max normalization anymore, just direct scaling
                    double scaledLength = vectorLength * unitVectorMultiplier;
                    
                    // Calculate the new endpoint using the scaled length
                    double nx = currentStartPoint.x + (dx / vectorLength) * scaledLength;
                    double ny = currentStartPoint.y + (dy / vectorLength) * scaledLength;
                    
                    // Ensure the endpoint stays within the plot boundaries
                    nx = Math.max(padding, Math.min(padding + plotSize, nx));
                    ny = Math.max(padding, Math.min(padding + plotSize, ny));
                    
                    // Calculate arrow base point (arrowSize back from the end) to hide line end cap
                    double arrowSize = Math.max(6.0, polylineThickness * 5.0);
                    double lineAngle = Math.atan2(ny - currentStartPoint.y, nx - currentStartPoint.x);
                    // Use 0.7x arrowSize to move arrow closer to the polyline
                    double arrowBaseX = nx - arrowSize * 0.7 * Math.cos(lineAngle);
                    double arrowBaseY = ny - arrowSize * 0.7 * Math.sin(lineAngle);
                    
                    // Draw the normalized line segment to the arrow base (so arrow covers the end cap)
                    g2.drawLine(currentStartPoint.x, currentStartPoint.y, (int)arrowBaseX, (int)arrowBaseY);
                    
                    // Draw arrow at the end of the normalized line
                    // If multiplier is 0, use a small fixed length for the arrow to show direction
                    if (unitVectorMultiplier == 0) {
                        // Use a small fixed length (5 pixels) to show direction
                        double arrowLength = 5.0;
                        double arrowAngle = Math.atan2(dy, dx);
                        double arrowX = currentStartPoint.x + (dx / vectorLength) * arrowLength;
                        double arrowY = currentStartPoint.y + (dy / vectorLength) * arrowLength;
                        // Use 0.7x arrowSize to move arrow closer to the polyline
                        double arrowBaseX0 = arrowX - arrowSize * 0.7 * Math.cos(arrowAngle);
                        double arrowBaseY0 = arrowY - arrowSize * 0.7 * Math.sin(arrowAngle);
                        g2.drawLine(currentStartPoint.x, currentStartPoint.y, (int)arrowBaseX0, (int)arrowBaseY0);
                        drawArrow(g2, (int)arrowBaseX0, (int)arrowBaseY0, (int)arrowX, (int)arrowY);
                        // Track arrow end point
                        arrowEnds.add(new Point((int)arrowX, (int)arrowY));
                    } else {
                        drawArrow(g2, (int)arrowBaseX, (int)arrowBaseY, (int)nx, (int)ny);
                        // Track arrow end point
                        arrowEnds.add(new Point((int)nx, (int)ny));
                    }
                    
                    // Update the start point for the next vector
                    currentStartPoint = new Point((int)nx, (int)ny);
                }
            } else {
                // For non-normalized vectors, draw as before
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    
                    // Track arrow start point
                    arrowStarts.add(new Point(p1.x, p1.y));
                    
                    // Calculate arrow base point (arrowSize back from the end) to hide line end cap
                    double arrowSize = Math.max(6.0, polylineThickness * 5.0);
                    double lineAngle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
                    // Use 0.7x arrowSize to move arrow closer to the polyline
                    double arrowBaseX = p2.x - arrowSize * 0.7 * Math.cos(lineAngle);
                    double arrowBaseY = p2.y - arrowSize * 0.7 * Math.sin(lineAngle);
                    
                    // Draw the regular line segment to the arrow base (so arrow covers the end cap)
                    g2.drawLine(p1.x, p1.y, (int)arrowBaseX, (int)arrowBaseY);
                    
                    // Draw arrow at the end of the line
                    drawArrow(g2, (int)arrowBaseX, (int)arrowBaseY, p2.x, p2.y);
                    
                    // Track arrow end point
                    arrowEnds.add(new Point(p2.x, p2.y));
                }
            }
        }
        
        private void collectArrowPoints(int row, int padding, int plotSize, List<Point> arrowStarts, List<Point> arrowEnds) {
            List<Point> points = new ArrayList<>();
            int numAttributes = attributeNames.size();

            // Loop over attributes in pairs, but handle odd number of attributes
            for (int i = 0; i < numAttributes; i += 2) {
                double xValue = normalize(data.get(i).get(row), i);
                
                // If this is the last attribute and it's odd, duplicate the value for y
                double yValue;
                if (i == numAttributes - 1) {
                    // Odd number of attributes, duplicate the last one
                    yValue = xValue;
                } else {
                    // Normal case, use the next attribute
                    yValue = normalize(data.get(i + 1).get(row), i + 1);
                }
                
                int x = padding + (int) (xValue * plotSize);
                int y = padding + (int) ((1 - yValue) * plotSize);
                points.add(new Point(x, y));
            }

            if (points.size() < 2) return;
            
            if (normalizeVectors) {
                // For normalized vectors, create a connected chain
                Point currentStartPoint = points.get(0);
                
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    
                    // Calculate the vector direction and length
                    double dx = p2.x - p1.x;
                    double dy = p2.y - p1.y;
                    double vectorLength = Math.sqrt(dx * dx + dy * dy);
                    
                    // Skip if vector is too small
                    if (vectorLength < 0.001) continue;
                    
                    // Track arrow start point
                    arrowStarts.add(new Point(currentStartPoint.x, currentStartPoint.y));
                    
                    // Calculate the new endpoint using the scaled length
                    double scaledLength = vectorLength * unitVectorMultiplier;
                    double nx = currentStartPoint.x + (dx / vectorLength) * scaledLength;
                    double ny = currentStartPoint.y + (dy / vectorLength) * scaledLength;
                    
                    // Ensure the endpoint stays within the plot boundaries
                    nx = Math.max(padding, Math.min(padding + plotSize, nx));
                    ny = Math.max(padding, Math.min(padding + plotSize, ny));
                    
                    // Track arrow end point
                    if (unitVectorMultiplier == 0) {
                        double arrowLength = 5.0;
                        double arrowX = currentStartPoint.x + (dx / vectorLength) * arrowLength;
                        double arrowY = currentStartPoint.y + (dy / vectorLength) * arrowLength;
                        arrowEnds.add(new Point((int)arrowX, (int)arrowY));
                    } else {
                        arrowEnds.add(new Point((int)nx, (int)ny));
                    }
                    
                    // Update the start point for the next vector
                    currentStartPoint = new Point((int)nx, (int)ny);
                }
            } else {
                // For non-normalized vectors
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    
                    // Track arrow start and end points
                    arrowStarts.add(new Point(p1.x, p1.y));
                    arrowEnds.add(new Point(p2.x, p2.y));
                }
            }
        }
        
        // Helper class to represent a point with its class
        private static class PointWithClass {
            Point point;
            String className;
            
            PointWithClass(Point point, String className) {
                this.point = point;
                this.className = className;
            }
        }
        
        private Set<Integer> getContainedRows(int padding, int plotSize) {
            Set<Integer> hiddenRows = new HashSet<>();
            
            // First, get all outermost boxes (same logic as drawBoundingBoxes)
            List<BoundingBox> outerBoxes = getAllOutermostBoxes(padding, plotSize);
            
            if (outerBoxes.isEmpty()) {
                return hiddenRows;
            }
            
            // For each row, check if it should be hidden
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String rowClass = classLabels.get(row);
                
                if (hiddenClasses.contains(rowClass)) {
                    continue; // Already hidden by class
                }
                
                // Collect all arrow points for this row
                List<Point> rowArrowStarts = new ArrayList<>();
                List<Point> rowArrowEnds = new ArrayList<>();
                collectArrowPoints(row, padding, plotSize, rowArrowStarts, rowArrowEnds);
                
                if (rowArrowStarts.isEmpty() && rowArrowEnds.isEmpty()) {
                    continue; // No points to check
                }
                
                // Check if all arrow points are contained within any box
                for (BoundingBox box : outerBoxes) {
                    // Skip if this row's class is the same as the box's class (boxes are now per-class)
                    if (rowClass.equals(box.class1)) {
                        continue; // Don't hide rows that form the boxes
                    }
                    
                    // Check if all arrow starts are contained
                    boolean allStartsContained = true;
                    for (Point p : rowArrowStarts) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allStartsContained = false;
                            break;
                        }
                    }
                    
                    // Check if all arrow ends are contained
                    boolean allEndsContained = true;
                    for (Point p : rowArrowEnds) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allEndsContained = false;
                            break;
                        }
                    }
                    
                    // If all points are contained, hide this row
                    if (allStartsContained && allEndsContained && (!rowArrowStarts.isEmpty() || !rowArrowEnds.isEmpty())) {
                        hiddenRows.add(row);
                        break; // Found a box that contains it, no need to check others
                    }
                }
            }
            
            return hiddenRows;
        }
        
        private List<BoundingBox> getAllOutermostBoxes(int padding, int plotSize) {
            // Map to store arrow start and end points with their classes for each class
            Map<String, List<PointWithClass>> classArrowStarts = new java.util.HashMap<>();
            Map<String, List<PointWithClass>> classArrowEnds = new java.util.HashMap<>();
            
            // Collect arrow points for each class
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                
                if (hiddenClasses.contains(classLabel)) continue;
                
                List<Point> arrowStarts = new ArrayList<>();
                List<Point> arrowEnds = new ArrayList<>();
                
                // Collect points without drawing
                collectArrowPoints(row, padding, plotSize, arrowStarts, arrowEnds);
                
                if (!arrowStarts.isEmpty()) {
                    List<PointWithClass> startsWithClass = classArrowStarts.computeIfAbsent(classLabel, k -> new ArrayList<>());
                    for (Point p : arrowStarts) {
                        startsWithClass.add(new PointWithClass(p, classLabel));
                    }
                }
                if (!arrowEnds.isEmpty()) {
                    List<PointWithClass> endsWithClass = classArrowEnds.computeIfAbsent(classLabel, k -> new ArrayList<>());
                    for (Point p : arrowEnds) {
                        endsWithClass.add(new PointWithClass(p, classLabel));
                    }
                }
            }
            
            // Count cases per class to determine which classes have more than one case
            Map<String, Integer> classCounts = new java.util.HashMap<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel)) {
                    classCounts.put(classLabel, classCounts.getOrDefault(classLabel, 0) + 1);
                }
            }
            
            // Get all unique class labels that have more than one case
            Set<String> allClasses = new HashSet<>(classArrowStarts.keySet());
            allClasses.addAll(classArrowEnds.keySet());
            List<String> classList = new ArrayList<>();
            for (String className : allClasses) {
                if (classCounts.getOrDefault(className, 0) > 1) {
                    classList.add(className);
                }
            }
            java.util.Collections.sort(classList); // Sort for consistency
            
            // Collect all boxes first - one box per class (not per class pair)
            List<BoundingBox> allBoxes = new ArrayList<>();
            
            for (String className : classList) {
                // Get arrow starts for this class only
                List<PointWithClass> classStarts = classArrowStarts.getOrDefault(className, new ArrayList<>());
                
                // Get arrow ends for this class only
                List<PointWithClass> classEnds = classArrowEnds.getOrDefault(className, new ArrayList<>());
                
                // Create box for arrow starts of this class
                if (!classStarts.isEmpty()) {
                    int minXStart = classStarts.stream().mapToInt(p -> p.point.x).min().orElse(0);
                    int maxXStart = classStarts.stream().mapToInt(p -> p.point.x).max().orElse(0);
                    int minYStart = classStarts.stream().mapToInt(p -> p.point.y).min().orElse(0);
                    int maxYStart = classStarts.stream().mapToInt(p -> p.point.y).max().orElse(0);
                    
                    // Find bottom-left most and top-right most points
                    PointWithClass bottomLeft = findBottomLeftPoint(classStarts, minXStart, maxYStart);
                    PointWithClass topRight = findTopRightPoint(classStarts, maxXStart, minYStart);
                    
                    Color bottomLeftColor = classColors.getOrDefault(bottomLeft != null ? bottomLeft.className : className, Color.BLACK);
                    Color topRightColor = classColors.getOrDefault(topRight != null ? topRight.className : className, Color.BLACK);
                    
                    allBoxes.add(new BoundingBox(minXStart, minYStart, maxXStart, maxYStart, className, className, true, 
                        new Color(bottomLeftColor.getRed(), bottomLeftColor.getGreen(), bottomLeftColor.getBlue(), 150),
                        new Color(topRightColor.getRed(), topRightColor.getGreen(), topRightColor.getBlue(), 150)));
                }
                
                // Create box for arrow ends of this class
                if (!classEnds.isEmpty()) {
                    int minXEnd = classEnds.stream().mapToInt(p -> p.point.x).min().orElse(0);
                    int maxXEnd = classEnds.stream().mapToInt(p -> p.point.x).max().orElse(0);
                    int minYEnd = classEnds.stream().mapToInt(p -> p.point.y).min().orElse(0);
                    int maxYEnd = classEnds.stream().mapToInt(p -> p.point.y).max().orElse(0);
                    
                    // Find bottom-left most and top-right most points
                    PointWithClass bottomLeft = findBottomLeftPoint(classEnds, minXEnd, maxYEnd);
                    PointWithClass topRight = findTopRightPoint(classEnds, maxXEnd, minYEnd);
                    
                    Color bottomLeftColor = classColors.getOrDefault(bottomLeft != null ? bottomLeft.className : className, Color.BLACK);
                    Color topRightColor = classColors.getOrDefault(topRight != null ? topRight.className : className, Color.BLACK);
                    
                    allBoxes.add(new BoundingBox(minXEnd, minYEnd, maxXEnd, maxYEnd, className, className, false,
                        new Color(bottomLeftColor.getRed(), bottomLeftColor.getGreen(), bottomLeftColor.getBlue(), 150),
                        new Color(topRightColor.getRed(), topRightColor.getGreen(), topRightColor.getBlue(), 150)));
                }
            }
            
            // First, identify outermost boxes (not contained in any other box)
            List<BoundingBox> outerBoxes = new ArrayList<>();
            for (BoundingBox box : allBoxes) {
                boolean isContained = false;
                for (BoundingBox other : allBoxes) {
                    if (box != other && box.isContainedIn(other)) {
                        isContained = true;
                        break;
                    }
                }
                if (!isContained) {
                    outerBoxes.add(box);
                }
            }
            
            // Now ensure all cases are contained within at least one box
            // Check each row to see if its arrow points are contained by any box
            Set<Integer> rowsNeedingBoxes = new HashSet<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String rowClass = classLabels.get(row);
                
                if (hiddenClasses.contains(rowClass)) {
                    continue;
                }
                
                // Collect arrow points for this row
                List<Point> rowArrowStarts = new ArrayList<>();
                List<Point> rowArrowEnds = new ArrayList<>();
                collectArrowPoints(row, padding, plotSize, rowArrowStarts, rowArrowEnds);
                
                if (rowArrowStarts.isEmpty() && rowArrowEnds.isEmpty()) {
                    continue;
                }
                
                // Check if this row's points are contained by any outer box
                boolean isContained = false;
                for (BoundingBox box : outerBoxes) {
                    // Skip if this row's class is the same as the box's class
                    if (rowClass.equals(box.class1)) {
                        continue;
                    }
                    
                    // Check if all arrow starts are contained
                    boolean allStartsContained = true;
                    for (Point p : rowArrowStarts) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allStartsContained = false;
                            break;
                        }
                    }
                    
                    // Check if all arrow ends are contained
                    boolean allEndsContained = true;
                    for (Point p : rowArrowEnds) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allEndsContained = false;
                            break;
                        }
                    }
                    
                    if (allStartsContained && allEndsContained && (!rowArrowStarts.isEmpty() || !rowArrowEnds.isEmpty())) {
                        isContained = true;
                        break;
                    }
                }
                
                if (!isContained) {
                    rowsNeedingBoxes.add(row);
                }
            }
            
            // For rows that aren't contained, find boxes that can contain them
            // and add those boxes if they're not already in outerBoxes
            for (Integer row : rowsNeedingBoxes) {
                String rowClass = classLabels.get(row);
                
                // Collect arrow points for this row
                List<Point> rowArrowStarts = new ArrayList<>();
                List<Point> rowArrowEnds = new ArrayList<>();
                collectArrowPoints(row, padding, plotSize, rowArrowStarts, rowArrowEnds);
                
                // Find boxes that contain this row's points
                for (BoundingBox box : allBoxes) {
                    if (outerBoxes.contains(box)) {
                        continue; // Already included
                    }
                    
                    // Skip if this row's class is the same as the box's class
                    if (rowClass.equals(box.class1)) {
                        continue;
                    }
                    
                    // Check if all arrow starts are contained
                    boolean allStartsContained = true;
                    for (Point p : rowArrowStarts) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allStartsContained = false;
                            break;
                        }
                    }
                    
                    // Check if all arrow ends are contained
                    boolean allEndsContained = true;
                    for (Point p : rowArrowEnds) {
                        if (p.x < box.minX || p.x > box.maxX || p.y < box.minY || p.y > box.maxY) {
                            allEndsContained = false;
                            break;
                        }
                    }
                    
                    if (allStartsContained && allEndsContained && (!rowArrowStarts.isEmpty() || !rowArrowEnds.isEmpty())) {
                        outerBoxes.add(box);
                        break; // Found a box for this row, move to next row
                    }
                }
            }
            
            return outerBoxes;
        }
        
        private PointWithClass findBottomLeftPoint(List<PointWithClass> points, int minX, int maxY) {
            // Try to find exact match first
            for (PointWithClass pwc : points) {
                if (pwc.point.x == minX && pwc.point.y == maxY) {
                    return pwc;
                }
            }
            
            // If not found, find closest
            double minDist = Double.MAX_VALUE;
            PointWithClass closest = null;
            for (PointWithClass pwc : points) {
                double dist = Math.sqrt(Math.pow(pwc.point.x - minX, 2) + Math.pow(pwc.point.y - maxY, 2));
                if (dist < minDist) {
                    minDist = dist;
                    closest = pwc;
                }
            }
            return closest;
        }
        
        private PointWithClass findTopRightPoint(List<PointWithClass> points, int maxX, int minY) {
            // Try to find exact match first
            for (PointWithClass pwc : points) {
                if (pwc.point.x == maxX && pwc.point.y == minY) {
                    return pwc;
                }
            }
            
            // If not found, find closest
            double minDist = Double.MAX_VALUE;
            PointWithClass closest = null;
            for (PointWithClass pwc : points) {
                double dist = Math.sqrt(Math.pow(pwc.point.x - maxX, 2) + Math.pow(pwc.point.y - minY, 2));
                if (dist < minDist) {
                    minDist = dist;
                    closest = pwc;
                }
            }
            return closest;
        }
        
        // Helper class to represent a bounding box
        private static class BoundingBox {
            int minX, minY, maxX, maxY;
            String class1, class2;
            boolean isArrowStart; // true for arrow starts, false for arrow ends
            Color bottomLeftColor; // Color for bottom and left edges
            Color topRightColor; // Color for top and right edges
            
            BoundingBox(int minX, int minY, int maxX, int maxY, String class1, String class2, boolean isArrowStart, Color bottomLeftColor, Color topRightColor) {
                this.minX = minX;
                this.minY = minY;
                this.maxX = maxX;
                this.maxY = maxY;
                this.class1 = class1;
                this.class2 = class2;
                this.isArrowStart = isArrowStart;
                this.bottomLeftColor = bottomLeftColor;
                this.topRightColor = topRightColor;
            }
            
            // Check if this box is completely contained within another box
            boolean isContainedIn(BoundingBox other) {
                return this.minX >= other.minX && this.maxX <= other.maxX &&
                       this.minY >= other.minY && this.maxY <= other.maxY;
            }
        }
        
        private void drawBoundingBoxes(Graphics2D g2, int padding, int plotSize) {
            // Get all outermost boxes using the shared method
            List<BoundingBox> outerBoxes = getAllOutermostBoxes(padding, plotSize);
            
            // Draw only the outermost boxes with different colored edges
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            for (BoundingBox box : outerBoxes) {
                // Draw bottom edge (left to right) with bottom-left color
                g2.setColor(box.bottomLeftColor);
                g2.drawLine(box.minX, box.maxY, box.maxX, box.maxY);
                
                // Draw left edge (bottom to top) with bottom-left color
                g2.setColor(box.bottomLeftColor);
                g2.drawLine(box.minX, box.maxY, box.minX, box.minY);
                
                // Draw top edge (left to right) with top-right color
                g2.setColor(box.topRightColor);
                g2.drawLine(box.minX, box.minY, box.maxX, box.minY);
                
                // Draw right edge (bottom to top) with top-right color
                g2.setColor(box.topRightColor);
                g2.drawLine(box.maxX, box.maxY, box.maxX, box.minY);
            }
        }
        
        private void drawArrow(Graphics2D g2, int baseX, int baseY, int tipX, int tipY) {
            // Arrow head size - scale with polyline thickness, with minimum size for visibility
            // Use a larger multiplier to ensure arrows are proportional to thicker lines
            double arrowSize = Math.max(6.0, polylineThickness * 5.0);
            
            // Calculate the angle of the line from base to tip
            double dx = tipX - baseX;
            double dy = tipY - baseY;
            double angle = Math.atan2(dy, dx);
            
            // Create the arrow head
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            
            // Arrow tip at the end point
            xPoints[0] = tipX;
            yPoints[0] = tipY;
            
            // First arrow wing
            xPoints[1] = (int) (tipX - arrowSize * Math.cos(angle - Math.PI/6));
            yPoints[1] = (int) (tipY - arrowSize * Math.sin(angle - Math.PI/6));
            
            // Second arrow wing
            xPoints[2] = (int) (tipX - arrowSize * Math.cos(angle + Math.PI/6));
            yPoints[2] = (int) (tipY - arrowSize * Math.sin(angle + Math.PI/6));
            
            // Draw the arrow head
            g2.fillPolygon(xPoints, yPoints, 3);
        }

        private double normalize(double value, int attributeIndex) {
            double min = data.get(attributeIndex).stream().min(Double::compare).orElse(0.0);
            double max = data.get(attributeIndex).stream().max(Double::compare).orElse(1.0);
            return (value - min) / (max - min);
        }
    }
}
