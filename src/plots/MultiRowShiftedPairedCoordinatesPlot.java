package src.plots;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import src.utils.ScreenshotUtils;
import src.utils.LegendUtils;

public class MultiRowShiftedPairedCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private JTable table;
    private Set<String> hiddenClasses;
    private int numRows;
    private Color backgroundColor;
    private int pairsPerRow;

    // Font and layout settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font ROW_LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final int PLOT_SIZE = 150;
    private static final int ROW_SPACING = 50;
    private static final int PLOT_SPACING = 30;
    private static final int ROW_PADDING = 80;

    public MultiRowShiftedPairedCoordinatesPlot(List<List<Double>> data, List<String> attributeNames,
                                        Map<String, Color> classColors, Map<String, Shape> classShapes,
                                        List<String> classLabels, List<Integer> selectedRows, 
                                        String datasetName, JTable table, int numRows, Color backgroundColor) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.table = table;
        this.hiddenClasses = new HashSet<>();
        this.numRows = numRows;
        
        // Calculate how many attribute pairs per row (ceiling division to ensure all attributes are included)
        int totalPairs = attributeNames.size() / 2 + (attributeNames.size() % 2 == 0 ? 0 : 1);
        this.pairsPerRow = (int) Math.ceil((double) totalPairs / numRows);

        setTitle("Multi-Row Shifted Paired Coordinates Plot");
        setSize(Math.max(800, pairsPerRow * (PLOT_SIZE + PLOT_SPACING) + 100), 
                200 + numRows * (PLOT_SIZE + ROW_SPACING + ROW_PADDING));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        MultiRowShiftedPairedCoordinatesPanel plotPanel = new MultiRowShiftedPairedCoordinatesPanel();
        int panelWidth = Math.max(800, pairsPerRow * (PLOT_SIZE + PLOT_SPACING) + 100);
        int panelHeight = 200 + numRows * (PLOT_SIZE + ROW_SPACING + ROW_PADDING);
        plotPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add a button to take a screenshot
        JButton screenshotButton = new JButton("Take Screenshot");
        screenshotButton.addActionListener(e -> {
            ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "MultiRowShiftedPairedCoordinates", datasetName);
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(screenshotButton);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private class MultiRowShiftedPairedCoordinatesPanel extends JPanel {
        
        public MultiRowShiftedPairedCoordinatesPanel() {
            setBackground(backgroundColor);
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            // Draw the title on white background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), 50);
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth("Multi-Row Shifted Paired Coordinates Plot");
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString("Multi-Row Shifted Paired Coordinates Plot", (getWidth() - titleWidth) / 2, titleHeight);
            
            // Draw each row of shifted paired coordinates
            for (int row = 0; row < numRows; row++) {
                drawRowPlots(g2, row);
                drawRowConnections(g2, row);
            }
        }
        
        private void drawRowPlots(Graphics2D g2, int rowIndex) {
            int startY = 100 + rowIndex * (PLOT_SIZE + ROW_SPACING + ROW_PADDING);
            int startX = 50;
            
            // Draw row label
            g2.setFont(ROW_LABEL_FONT);
            g2.setColor(Color.BLACK);
            g2.drawString("Row " + (rowIndex + 1), 10, startY - 20);
            
            // Calculate the range of attribute pairs for this row
            int startPairIndex = rowIndex * pairsPerRow;
            int endPairIndex = Math.min(startPairIndex + pairsPerRow, attributeNames.size() / 2 + (attributeNames.size() % 2 == 0 ? 0 : 1));
            
            // Draw each attribute pair plot for this row
            for (int pairIdx = startPairIndex; pairIdx < endPairIndex; pairIdx++) {
                int plotX = startX + (pairIdx - startPairIndex) * (PLOT_SIZE + PLOT_SPACING);
                int plotY = startY;
                
                // Calculate the attribute indices for this pair
                int attr1Idx = pairIdx * 2;
                int attr2Idx = attr1Idx + 1;
                
                // Handle case when we have odd number of attributes
                if (attr2Idx >= attributeNames.size()) {
                    attr2Idx = attr1Idx;
                }
                
                // Draw plot frame
                g2.setColor(Color.BLACK);
                g2.drawRect(plotX, plotY, PLOT_SIZE, PLOT_SIZE);
                
                // Draw attribute labels
                g2.setFont(AXIS_LABEL_FONT);
                String attr1Name = attributeNames.get(attr1Idx);
                String attr2Name = attributeNames.get(attr2Idx);
                
                // X-axis label (bottom)
                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(attr1Name);
                g2.drawString(attr1Name, plotX + (PLOT_SIZE - labelWidth) / 2, plotY + PLOT_SIZE + 20);
                
                // Y-axis label (left side, rotated)
                g2.rotate(-Math.PI / 2, plotX - 10, plotY + PLOT_SIZE / 2);
                g2.drawString(attr2Name, plotX - 10 - fm.stringWidth(attr2Name) / 2, plotY + PLOT_SIZE / 2 + fm.getAscent() / 2);
                g2.rotate(Math.PI / 2, plotX - 10, plotY + PLOT_SIZE / 2);
                
                // Draw data points for this plot
                drawDataPoints(g2, attr1Idx, attr2Idx, plotX, plotY);
            }
        }
        
        private void drawDataPoints(Graphics2D g2, int attr1Idx, int attr2Idx, int plotX, int plotY) {
            // First draw non-selected points
            for (int row = 0; row < data.get(0).size(); row++) {
                if (selectedRows.contains(row)) continue;
                
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;
                
                // Get normalized coordinates
                double x = normalize(data.get(attr1Idx).get(row), attr1Idx);
                double y = normalize(data.get(attr2Idx).get(row), attr2Idx);
                
                // Map to screen coordinates
                int screenX = plotX + (int)(x * PLOT_SIZE);
                int screenY = plotY + PLOT_SIZE - (int)(y * PLOT_SIZE);
                
                // Draw the point with class color and shape
                Color color = classColors.getOrDefault(classLabel, Color.BLACK);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                
                g2.setColor(color);
                g2.translate(screenX, screenY);
                g2.fill(shape);
                g2.translate(-screenX, -screenY);
            }
            
            // Then draw selected points on top
            for (int row : selectedRows) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;
                
                // Get normalized coordinates
                double x = normalize(data.get(attr1Idx).get(row), attr1Idx);
                double y = normalize(data.get(attr2Idx).get(row), attr2Idx);
                
                // Map to screen coordinates
                int screenX = plotX + (int)(x * PLOT_SIZE);
                int screenY = plotY + PLOT_SIZE - (int)(y * PLOT_SIZE);
                
                // Draw the point with highlight color
                g2.setColor(Color.YELLOW);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                
                g2.translate(screenX, screenY);
                g2.fill(shape);
                g2.translate(-screenX, -screenY);
            }
        }
        
        private void drawRowConnections(Graphics2D g2, int rowIndex) {
            int startY = 100 + rowIndex * (PLOT_SIZE + ROW_SPACING + ROW_PADDING);
            int startX = 50;
            
            // Calculate the range of attribute pairs for this row
            int startPairIndex = rowIndex * pairsPerRow;
            int endPairIndex = Math.min(startPairIndex + pairsPerRow, attributeNames.size() / 2 + (attributeNames.size() % 2 == 0 ? 0 : 1));
            
            // Skip if there's only one plot or no plots
            if (endPairIndex - startPairIndex <= 1) return;
            
            // Draw connections between plots for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;
                
                // Set color based on selection
                if (selectedRows.contains(row)) {
                    g2.setColor(Color.YELLOW);
                    g2.setStroke(new BasicStroke(2.0f));
                } else {
                    g2.setColor(classColors.getOrDefault(classLabel, Color.BLACK));
                    g2.setStroke(new BasicStroke(1.0f));
                }
                
                // Connect points across plots in this row
                for (int pairIdx = startPairIndex; pairIdx < endPairIndex - 1; pairIdx++) {
                    int plotX1 = startX + (pairIdx - startPairIndex) * (PLOT_SIZE + PLOT_SPACING);
                    int plotX2 = startX + (pairIdx + 1 - startPairIndex) * (PLOT_SIZE + PLOT_SPACING);
                    
                    // Calculate attribute indices
                    int attr1Idx = pairIdx * 2;
                    int attr2Idx = attr1Idx + 1;
                    int nextAttr1Idx = (pairIdx + 1) * 2;
                    int nextAttr2Idx = nextAttr1Idx + 1;
                    
                    // Handle odd number of attributes
                    if (attr2Idx >= attributeNames.size()) attr2Idx = attr1Idx;
                    if (nextAttr2Idx >= attributeNames.size()) nextAttr2Idx = nextAttr1Idx;
                    
                    // Calculate coordinates
                    double x1 = normalize(data.get(attr1Idx).get(row), attr1Idx);
                    double y1 = normalize(data.get(attr2Idx).get(row), attr2Idx);
                    double x2 = normalize(data.get(nextAttr1Idx).get(row), nextAttr1Idx);
                    double y2 = normalize(data.get(nextAttr2Idx).get(row), nextAttr2Idx);
                    
                    // Map to screen coordinates
                    int screenX1 = plotX1 + (int)(x1 * PLOT_SIZE);
                    int screenY1 = startY + PLOT_SIZE - (int)(y1 * PLOT_SIZE);
                    int screenX2 = plotX2 + (int)(x2 * PLOT_SIZE);
                    int screenY2 = startY + PLOT_SIZE - (int)(y2 * PLOT_SIZE);
                    
                    // Draw connection line
                    g2.drawLine(screenX1, screenY1, screenX2, screenY2);
                }
            }
            
            // Reset stroke
            g2.setStroke(new BasicStroke(1.0f));
        }
        
        private double normalize(double value, int attributeIndex) {
            double min = data.get(attributeIndex).stream().min(Double::compare).orElse(0.0);
            double max = data.get(attributeIndex).stream().max(Double::compare).orElse(1.0);
            return (value - min) / (max - min);
        }
    }
} 