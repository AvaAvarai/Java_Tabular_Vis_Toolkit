package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import src.utils.ScreenshotUtils;
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
    private boolean normalizeVectors = false;
    private double unitVectorMultiplier = 1.0; // Default multiplier
    private CollocatedPairedCoordinatesPanel plotPanel;
    private JLabel multiplierLabel; // Label to show current multiplier value
    private JPanel sliderPanel; // Store reference to the slider panel

    public CollocatedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, String datasetName, JTable table) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
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
        
        JPanel normalizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add vector normalization toggle
        JCheckBox normalizeVectorsCheckbox = new JCheckBox("Normalize Vectors");
        normalizeVectorsCheckbox.setSelected(normalizeVectors);
        normalizeVectorsCheckbox.addActionListener(e -> {
            normalizeVectors = normalizeVectorsCheckbox.isSelected();
            updateSliderVisibility(normalizeVectors);
            if (plotPanel != null) {
                plotPanel.repaint();
            }
        });
        normalizePanel.add(normalizeVectorsCheckbox);
        controlPanel.add(normalizePanel);
        
        // Add slider panel for unit vector length control
        sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sliderPanel.add(new JLabel("Unit Vector Length: "));
        
        // Create slider for controlling unit vector length in a more focused range (0-0.25)
        JSlider unitVectorSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 25);
        unitVectorSlider.setMajorTickSpacing(25);
        unitVectorSlider.setMinorTickSpacing(5);
        unitVectorSlider.setPaintTicks(true);
        unitVectorSlider.setPaintLabels(true);
        
        // Create custom labels for the slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0.00"));
        labelTable.put(25, new JLabel("0.06"));
        labelTable.put(50, new JLabel("0.12"));
        labelTable.put(75, new JLabel("0.18"));
        labelTable.put(100, new JLabel("0.25"));
        unitVectorSlider.setLabelTable(labelTable);
        
        multiplierLabel = new JLabel(String.format("%.3fx", 0.25 * (unitVectorSlider.getValue() / 100.0)));
        
        unitVectorSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // Map the 0-100 range to 0-0.25 for finer control
                unitVectorMultiplier = 0.25 * (unitVectorSlider.getValue() / 100.0);
                multiplierLabel.setText(String.format("%.3fx", unitVectorMultiplier));
                if (plotPanel != null) {
                    plotPanel.repaint();
                }
            }
        });
        
        sliderPanel.add(unitVectorSlider);
        sliderPanel.add(multiplierLabel);
        controlPanel.add(sliderPanel);
        
        // Initially hide slider if normalization is off
        sliderPanel.setVisible(normalizeVectors);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        plotPanel = new CollocatedPairedCoordinatesPanel();
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }
    
    // Helper method to update slider visibility
    private void updateSliderVisibility(boolean visible) {
        if (sliderPanel != null) {
            sliderPanel.setVisible(visible);
        }
    }

    private class CollocatedPairedCoordinatesPanel extends JPanel {
        // Cached values for vector normalization
        private double minVectorLength = Double.MAX_VALUE;
        private double maxVectorLength = Double.MIN_VALUE;
        
        public CollocatedPairedCoordinatesPanel() {
            setBackground(new Color(0xC0C0C0));
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
        }
        
        private void calculateVectorLengthRange() {
            minVectorLength = Double.MAX_VALUE;
            maxVectorLength = Double.MIN_VALUE;
            
            for (int row = 0; row < data.get(0).size(); row++) {
                List<Point> points = new ArrayList<>();
                int numAttributes = attributeNames.size();

                for (int i = 0; i < numAttributes - 1; i += 2) {
                    double xValue = normalize(data.get(i).get(row), i);
                    double yValue = normalize(data.get(i + 1).get(row), i + 1);
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
            List<Point> points = new ArrayList<>();
            int numAttributes = attributeNames.size();

            for (int i = 0; i < numAttributes - 1; i += 2) {
                double xValue = normalize(data.get(i).get(row), i);
                double yValue = normalize(data.get(i + 1).get(row), i + 1);
                int x = padding + (int) (xValue * plotSize);
                int y = padding + (int) ((1 - yValue) * plotSize);
                points.add(new Point(x, y));
            }

            if (points.size() < 2) return;

            String classLabel = classLabels.get(row);
            Color color = isSelected ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                
                if (normalizeVectors) {
                    // Calculate the vector direction and length
                    double dx = p2.x - p1.x;
                    double dy = p2.y - p1.y;
                    double vectorLength = Math.sqrt(dx * dx + dy * dy);
                    
                    // Skip drawing if vector is too small
                    if (vectorLength < 0.001) continue;
                    
                    // Normalize the vector length to 0-1 range
                    double normalizedLength = (vectorLength - minVectorLength) / (maxVectorLength - minVectorLength);
                    
                    // Apply unit vector multiplier
                    normalizedLength *= unitVectorMultiplier;
                    
                    // Calculate the new endpoint using the normalized length
                    double nx = p1.x + (dx / vectorLength) * normalizedLength * vectorLength;
                    double ny = p1.y + (dy / vectorLength) * normalizedLength * vectorLength;
                    
                    // Draw the normalized line segment
                    g2.drawLine(p1.x, p1.y, (int)nx, (int)ny);
                    
                    // Draw arrow at the end of the normalized line
                    drawArrow(g2, p1.x, p1.y, (int)nx, (int)ny);
                } else {
                    // Draw the regular line segment
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    
                    // Draw arrow at the end of the line
                    drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
        
        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
            // Arrow head size
            int arrowSize = 8;
            
            // Calculate the angle of the line
            double dx = x2 - x1;
            double dy = y2 - y1;
            double angle = Math.atan2(dy, dx);
            
            // Create the arrow head
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            
            // Arrow tip at the end point
            xPoints[0] = x2;
            yPoints[0] = y2;
            
            // First arrow wing
            xPoints[1] = (int) (x2 - arrowSize * Math.cos(angle - Math.PI/6));
            yPoints[1] = (int) (y2 - arrowSize * Math.sin(angle - Math.PI/6));
            
            // Second arrow wing
            xPoints[2] = (int) (x2 - arrowSize * Math.cos(angle + Math.PI/6));
            yPoints[2] = (int) (y2 - arrowSize * Math.sin(angle + Math.PI/6));
            
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
