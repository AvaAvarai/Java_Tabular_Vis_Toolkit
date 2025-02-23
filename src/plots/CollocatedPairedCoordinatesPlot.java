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
    private double zoomLevel = 1.0;
    private Set<String> hiddenClasses;
    private boolean showAttributeLabels = true;

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

        // Zoom slider
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.addChangeListener(e -> {
            zoomLevel = zoomSlider.getValue() / 100.0;
            repaint();
        });
        controlPanel.add(new JLabel("Zoom: "));
        controlPanel.add(zoomSlider);

        // Attribute labels toggle
        JToggleButton attributeLabelsToggle = new JToggleButton("Show Labels");
        attributeLabelsToggle.addActionListener(e -> {
            showAttributeLabels = attributeLabelsToggle.isSelected();
            repaint();
        });
        controlPanel.add(attributeLabelsToggle);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        CollocatedPairedCoordinatesPanel plotPanel = new CollocatedPairedCoordinatesPanel();
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private class CollocatedPairedCoordinatesPanel extends JPanel {
        public CollocatedPairedCoordinatesPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || attributeNames == null || data.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.scale(zoomLevel, zoomLevel);

            int width = getWidth();
            int height = getHeight();
            int padding = 50;
            int plotSize = Math.min(width, height) - 2 * padding;
            int centerX = width / 2;
            int centerY = height / 2;

            g2.setColor(Color.BLACK);
            g2.drawRect(padding, padding, plotSize, plotSize);

            for (int i = 0; i < table.getRowCount(); i++) {
                int row = table.convertRowIndexToModel(i);
                String classLabel = classLabels.get(row);
                if (!hiddenClasses.contains(classLabel)) {
                    drawPolyline(g2, row, padding, plotSize);
                }
            }
        }

        private void drawPolyline(Graphics2D g2, int row, int padding, int plotSize) {
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
            Color color = selectedRows.contains(row) ? Color.YELLOW : classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            
            for (int i = 0; i < points.size() - 1; i++) {
                g2.drawLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
            }
            
            // Draw scatter points
            Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
            for (Point p : points) {
                g2.translate(p.x, p.y);
                g2.fill(shape);
                g2.translate(-p.x, -p.y);
            }
        }

        private double normalize(double value, int attributeIndex) {
            double min = data.get(attributeIndex).stream().min(Double::compare).orElse(0.0);
            double max = data.get(attributeIndex).stream().max(Double::compare).orElse(1.0);
            return (value - min) / (max - min);
        }
    }
}
