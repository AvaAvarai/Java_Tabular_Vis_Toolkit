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
        sliderPanel.add(new JLabel("Unit Vector Length: "));
        
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

            // Draw non-selected lines first
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel) && !selectedRows.contains(row)) {
                    drawPolyline(g2, row, padding, plotSize, false);
                }
            }

            // Draw selected lines last to highlight them
            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel) && selectedRows.contains(row)) {
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
        
        // Helper class to represent a bounding box
        private static class BoundingBox {
            int minX, minY, maxX, maxY;
            String class1, class2;
            boolean isArrowStart; // true for arrow starts, false for arrow ends
            Color color;
            
            BoundingBox(int minX, int minY, int maxX, int maxY, String class1, String class2, boolean isArrowStart, Color color) {
                this.minX = minX;
                this.minY = minY;
                this.maxX = maxX;
                this.maxY = maxY;
                this.class1 = class1;
                this.class2 = class2;
                this.isArrowStart = isArrowStart;
                this.color = color;
            }
            
            // Check if this box is completely contained within another box
            boolean isContainedIn(BoundingBox other) {
                return this.minX >= other.minX && this.maxX <= other.maxX &&
                       this.minY >= other.minY && this.maxY <= other.maxY;
            }
        }
        
        private void drawBoundingBoxes(Graphics2D g2, int padding, int plotSize) {
            // Map to store arrow start and end points for each class
            Map<String, List<Point>> classArrowStarts = new java.util.HashMap<>();
            Map<String, List<Point>> classArrowEnds = new java.util.HashMap<>();
            
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
                    classArrowStarts.computeIfAbsent(classLabel, k -> new ArrayList<>()).addAll(arrowStarts);
                }
                if (!arrowEnds.isEmpty()) {
                    classArrowEnds.computeIfAbsent(classLabel, k -> new ArrayList<>()).addAll(arrowEnds);
                }
            }
            
            // Get all unique class labels
            Set<String> allClasses = new HashSet<>(classArrowStarts.keySet());
            allClasses.addAll(classArrowEnds.keySet());
            List<String> classList = new ArrayList<>(allClasses);
            java.util.Collections.sort(classList); // Sort for consistent pairing
            
            // Collect all boxes first
            List<BoundingBox> allBoxes = new ArrayList<>();
            
            for (int i = 0; i < classList.size(); i++) {
                for (int j = i + 1; j < classList.size(); j++) {
                    String class1 = classList.get(i);
                    String class2 = classList.get(j);
                    
                    // Combine arrow starts from both classes in this pair
                    List<Point> pairStarts = new ArrayList<>();
                    if (classArrowStarts.containsKey(class1)) {
                        pairStarts.addAll(classArrowStarts.get(class1));
                    }
                    if (classArrowStarts.containsKey(class2)) {
                        pairStarts.addAll(classArrowStarts.get(class2));
                    }
                    
                    // Combine arrow ends from both classes in this pair
                    List<Point> pairEnds = new ArrayList<>();
                    if (classArrowEnds.containsKey(class1)) {
                        pairEnds.addAll(classArrowEnds.get(class1));
                    }
                    if (classArrowEnds.containsKey(class2)) {
                        pairEnds.addAll(classArrowEnds.get(class2));
                    }
                    
                    // Create box for arrow starts of this class pair
                    if (!pairStarts.isEmpty()) {
                        int minXStart = pairStarts.stream().mapToInt(p -> p.x).min().orElse(0);
                        int maxXStart = pairStarts.stream().mapToInt(p -> p.x).max().orElse(0);
                        int minYStart = pairStarts.stream().mapToInt(p -> p.y).min().orElse(0);
                        int maxYStart = pairStarts.stream().mapToInt(p -> p.y).max().orElse(0);
                        
                        // Use a color that represents the pair (blend of both class colors)
                        Color color1 = classColors.getOrDefault(class1, Color.BLACK);
                        Color color2 = classColors.getOrDefault(class2, Color.BLACK);
                        int r = (color1.getRed() + color2.getRed()) / 2;
                        int g = (color1.getGreen() + color2.getGreen()) / 2;
                        int b = (color1.getBlue() + color2.getBlue()) / 2;
                        Color pairColor = new Color(r, g, b, 150);
                        
                        allBoxes.add(new BoundingBox(minXStart, minYStart, maxXStart, maxYStart, class1, class2, true, pairColor));
                    }
                    
                    // Create box for arrow ends of this class pair
                    if (!pairEnds.isEmpty()) {
                        int minXEnd = pairEnds.stream().mapToInt(p -> p.x).min().orElse(0);
                        int maxXEnd = pairEnds.stream().mapToInt(p -> p.x).max().orElse(0);
                        int minYEnd = pairEnds.stream().mapToInt(p -> p.y).min().orElse(0);
                        int maxYEnd = pairEnds.stream().mapToInt(p -> p.y).max().orElse(0);
                        
                        // Use a color that represents the pair (blend of both class colors)
                        Color color1 = classColors.getOrDefault(class1, Color.BLACK);
                        Color color2 = classColors.getOrDefault(class2, Color.BLACK);
                        int r = (color1.getRed() + color2.getRed()) / 2;
                        int g = (color1.getGreen() + color2.getGreen()) / 2;
                        int b = (color1.getBlue() + color2.getBlue()) / 2;
                        Color pairColor = new Color(r, g, b, 150);
                        
                        allBoxes.add(new BoundingBox(minXEnd, minYEnd, maxXEnd, maxYEnd, class1, class2, false, pairColor));
                    }
                }
            }
            
            // Filter out boxes that are contained within other boxes
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
            
            // Draw only the outermost boxes
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            for (BoundingBox box : outerBoxes) {
                g2.setColor(box.color);
                g2.drawRect(box.minX, box.minY, box.maxX - box.minX, box.maxY - box.minY);
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
