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

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        CollocatedPairedCoordinatesPanel plotPanel = new CollocatedPairedCoordinatesPanel();
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private class CollocatedPairedCoordinatesPanel extends JPanel {
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
                
                // Draw the line segment
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                
                // Draw arrow at the end of the line
                drawArrow(g2, p1.x, p1.y, p2.x, p2.y);
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
