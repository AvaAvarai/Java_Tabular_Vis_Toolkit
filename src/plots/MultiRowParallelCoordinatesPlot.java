package src.plots;

import javax.swing.*;

import src.utils.ScreenshotUtils;
import java.awt.*;
import java.util.*;
import java.util.List;
import src.utils.LegendUtils;

public class MultiRowParallelCoordinatesPlot extends JFrame {

    private final List<List<Double>> data;
    private final List<String> attributeNames;
    private final Map<String, Color> classColors;
    private final Map<String, Shape> classShapes;
    private final List<String> classLabels;
    private final List<Integer> selectedRows;
    private Set<String> hiddenClasses;
    private final double globalMaxValue;
    private final double globalMinValue;
    private final int numRows;
    private final int attributesPerRow;
    private Color backgroundColor;
    private float polylineThickness;
    
    // Empty entry tracking
    private final Map<String, Integer> emptyEntryCounts = new HashMap<>();
    private final Map<String, Double> emptyEntryFrequencies = new HashMap<>();
    private final Map<String, Boolean> hasEmptyEntries = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> emptyBlockSizes = new HashMap<>(); // Track block size for each empty entry

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int AXIS_HEIGHT = 250;
    private static final int ROW_SPACING = 50;

    public MultiRowParallelCoordinatesPlot(List<List<Double>> data, List<String> attributeNames,
                                   Map<String, Color> classColors, Map<String, Shape> classShapes,
                                   List<String> classLabels, List<Integer> selectedRows, 
                                   String datasetName, int numRows, Color backgroundColor, float polylineThickness) {
        this.data = data;
        this.attributeNames = new ArrayList<>(attributeNames);
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.polylineThickness = polylineThickness;
        this.hiddenClasses = new HashSet<>();
        this.numRows = numRows;
        
        // Calculate attributes per row (ceiling division to ensure all attributes are included)
        this.attributesPerRow = (int) Math.ceil((double) attributeNames.size() / numRows);

        // Calculate global max and min values (excluding NaN values)
        this.globalMaxValue = data.stream()
                .flatMap(List::stream)
                .filter(d -> !Double.isNaN(d))
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        this.globalMinValue = data.stream()
                .flatMap(List::stream)
                .filter(d -> !Double.isNaN(d))
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        
        // Calculate empty entry frequencies for each attribute
        calculateEmptyEntryFrequencies();

        setTitle("Multi-Row Parallel Coordinates Plot");
        setSize(Math.max(800, attributesPerRow * 150), 200 + numRows * (AXIS_HEIGHT + ROW_SPACING));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        MultiRowParallelCoordinatesPanel plotPanel = new MultiRowParallelCoordinatesPanel();
        int panelWidth = Math.max(800, attributesPerRow * 150);
        int panelHeight = 200 + numRows * (AXIS_HEIGHT + ROW_SPACING);
        plotPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add a button to take a screenshot
        JButton screenshotButton = new JButton("Take Screenshot");
        screenshotButton.addActionListener(e -> {
            ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "MultiRowParallelCoordinates", datasetName);
        });

        JPanel controlPanel = new JPanel();
        controlPanel.add(screenshotButton);
        
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }
    
    /**
     * Calculate empty entry frequencies for each attribute
     */
    private void calculateEmptyEntryFrequencies() {
        int totalRows = data.get(0).size();
        
        for (int i = 0; i < attributeNames.size(); i++) {
            String attributeName = attributeNames.get(i);
            int emptyCount = 0;
            Map<Integer, Integer> blockSizes = new HashMap<>();
            
            // Detect consecutive empty entry blocks
            int currentBlockStart = -1;
            int currentBlockSize = 0;
            
            for (int row = 0; row < totalRows; row++) {
                double value = data.get(i).get(row);
                if (Double.isNaN(value)) {
                    emptyCount++;
                    if (currentBlockStart == -1) {
                        // Start of a new block
                        currentBlockStart = row;
                        currentBlockSize = 1;
                    } else {
                        // Continue current block
                        currentBlockSize++;
                    }
                } else {
                    // End of current block (if any)
                    if (currentBlockStart != -1) {
                        // Record block size for all rows in this block
                        for (int blockRow = currentBlockStart; blockRow < currentBlockStart + currentBlockSize; blockRow++) {
                            blockSizes.put(blockRow, currentBlockSize);
                        }
                        currentBlockStart = -1;
                        currentBlockSize = 0;
                    }
                }
            }
            
            // Handle block that extends to the end
            if (currentBlockStart != -1) {
                for (int blockRow = currentBlockStart; blockRow < currentBlockStart + currentBlockSize; blockRow++) {
                    blockSizes.put(blockRow, currentBlockSize);
                }
            }
            
            emptyEntryCounts.put(attributeName, emptyCount);
            emptyBlockSizes.put(attributeName, blockSizes);
            double frequency = (double) emptyCount / totalRows;
            emptyEntryFrequencies.put(attributeName, frequency);
            hasEmptyEntries.put(attributeName, emptyCount > 0);
        }
    }
    
    /**
     * Draw empty entry block on the axis
     */
    private void drawEmptyEntryBlock(Graphics2D g2, String attributeName, int xPos, int startY, int axisHeight) {
        double frequency = emptyEntryFrequencies.getOrDefault(attributeName, 0.0);
        int emptyCount = emptyEntryCounts.getOrDefault(attributeName, 0);
        
        // Calculate block size proportional to frequency
        int blockHeight = (int) (frequency * axisHeight);
        if (blockHeight < 5) blockHeight = 5; // Minimum visible size
        
        // Position the block at the bottom of the axis
        int blockY = startY + axisHeight - blockHeight;
        int blockX = xPos - 10; // Offset from axis line
        int blockWidth = 20;
        
        // Draw the empty entry block
        g2.setColor(new Color(200, 200, 200, 150)); // Light gray with transparency
        g2.fillRect(blockX, blockY, blockWidth, blockHeight);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(blockX, blockY, blockWidth, blockHeight);
        
        // Draw frequency text
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String frequencyText = String.format("%.1f%%", frequency * 100);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(frequencyText);
        int textX = blockX + (blockWidth - textWidth) / 2;
        int textY = blockY + blockHeight / 2 + fm.getHeight() / 4;
        g2.drawString(frequencyText, textX, textY);
        
        // Draw count text below
        String countText = "(" + emptyCount + ")";
        int countTextWidth = fm.stringWidth(countText);
        int countTextX = blockX + (blockWidth - countTextWidth) / 2;
        int countTextY = textY + fm.getHeight();
        g2.drawString(countText, countTextX, countTextY);
    }

    private class MultiRowParallelCoordinatesPanel extends JPanel {
        
        public MultiRowParallelCoordinatesPanel() {
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
            int titleWidth = fm.stringWidth("Multi-Row Parallel Coordinates Plot");
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString("Multi-Row Parallel Coordinates Plot", (getWidth() - titleWidth) / 2, titleHeight);
            
            // Draw the plot on the selected background color
            g2.setColor(backgroundColor);
            g2.fillRect(0, 50, getWidth(), getHeight() - 50);
            
            // Draw each row of parallel coordinates
            for (int row = 0; row < numRows; row++) {
                drawRowAxes(g2, row);
                drawRowData(g2, row);
            }
        }
        
        private void drawRowAxes(Graphics2D g2, int rowIndex) {
            g2.setColor(Color.BLACK);
            
            int startY = 100 + rowIndex * (AXIS_HEIGHT + ROW_SPACING);
            int startX = 50;
            int spacing = 150; // Space between axes
            
            // Calculate the range of attributes for this row
            int startAttrIndex = rowIndex * attributesPerRow;
            int endAttrIndex = Math.min(startAttrIndex + attributesPerRow, attributeNames.size());
            
            // Draw row label
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Row " + (rowIndex + 1), 10, startY - 20);
            
            // Draw each axis for this row
            for (int i = startAttrIndex; i < endAttrIndex; i++) {
                int xPos = startX + (i - startAttrIndex) * spacing;
                
                // Draw axis
                g2.drawLine(xPos, startY, xPos, startY + AXIS_HEIGHT);
                
                // Draw empty entry block if this attribute has empty entries
                String attributeName = attributeNames.get(i);
                if (hasEmptyEntries.getOrDefault(attributeName, false)) {
                    drawEmptyEntryBlock(g2, attributeName, xPos, startY, AXIS_HEIGHT);
                }
                
                // Draw attribute label
                g2.setFont(AXIS_LABEL_FONT);
                String label = attributeNames.get(i);
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, xPos - labelWidth / 2, startY + AXIS_HEIGHT + 20);
                
                // Draw min/max values
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString(String.format("%.2f", globalMaxValue), xPos + 5, startY + 10);
                g2.drawString(String.format("%.2f", globalMinValue), xPos + 5, startY + AXIS_HEIGHT - 5);
            }
        }
        
        private void drawRowData(Graphics2D g2, int rowIndex) {
            // Calculate the range of attributes for this row
            int startAttrIndex = rowIndex * attributesPerRow;
            int endAttrIndex = Math.min(startAttrIndex + attributesPerRow, attributeNames.size());
            
            if (startAttrIndex >= attributeNames.size()) return; // No attributes for this row
            
            int startY = 100 + rowIndex * (AXIS_HEIGHT + ROW_SPACING);
            int startX = 50;
            int spacing = 150;
            
            // First draw non-selected lines
            for (int row = 0; row < data.get(0).size(); row++) {
                if (selectedRows.contains(row)) continue; // Skip selected rows for now
                
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;
                
                Color color = classColors.getOrDefault(classLabel, Color.BLACK);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(polylineThickness));
                
                List<Point> points = new ArrayList<>();
                
                // Calculate points for each attribute in this row
                for (int i = startAttrIndex; i < endAttrIndex; i++) {
                    double value = data.get(i).get(row);
                    String attributeName = attributeNames.get(i);
                    
                    int xPos = startX + (i - startAttrIndex) * spacing;
                    int yPos;
                    
                    // Check if this is an empty entry (NaN only, not 0.0)
                    if (Double.isNaN(value)) {
                        // For empty entries, position them within the empty entry block at the bottom
                        double frequency = emptyEntryFrequencies.getOrDefault(attributeName, 0.0);
                        int blockHeight = (int) (frequency * AXIS_HEIGHT);
                        if (blockHeight < 5) blockHeight = 5;
                        
                        // Get the size of the consecutive empty block this entry belongs to
                        Map<Integer, Integer> blockSizes = emptyBlockSizes.getOrDefault(attributeName, new HashMap<>());
                        int consecutiveBlockSize = blockSizes.getOrDefault(row, 1);
                        
                        // Find position within the consecutive block
                        int positionInConsecutiveBlock = 0;
                        for (int checkRow = 0; checkRow < row; checkRow++) {
                            if (Double.isNaN(data.get(i).get(checkRow))) {
                                positionInConsecutiveBlock++;
                            } else {
                                positionInConsecutiveBlock = 0; // Reset if we hit a non-empty entry
                            }
                        }
                        
                        // Normalize position within the consecutive block
                        double normalizedPosition = (double) positionInConsecutiveBlock / Math.max(1, consecutiveBlockSize - 1);
                        yPos = startY + AXIS_HEIGHT - blockHeight + (int)(normalizedPosition * blockHeight);
                    } else {
                        // Normal value processing - adjust for empty block space and gap
                        double frequency = emptyEntryFrequencies.getOrDefault(attributeName, 0.0);
                        int blockHeight = (int) (frequency * AXIS_HEIGHT);
                        if (blockHeight < 5) blockHeight = 5;
                        
                        // Gap size is the size of one point (assuming point size of 8 pixels)
                        int gapSize = 8;
                        
                        // Available height for numerical data (excluding empty block and gap)
                        int availableHeight = AXIS_HEIGHT - blockHeight - gapSize;
                        
                        double normalizedValue = (value - globalMinValue) / (globalMaxValue - globalMinValue);
                        yPos = startY + availableHeight - (int)(normalizedValue * availableHeight);
                    }
                    
                    points.add(new Point(xPos, yPos));
                }
                
                // Draw polyline
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
            
            // Now draw selected lines on top with yellow color
            for (int row : selectedRows) {
                String classLabel = classLabels.get(row);
                if (hiddenClasses.contains(classLabel)) continue;
                
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(polylineThickness * 2.0f));
                
                List<Point> points = new ArrayList<>();
                
                // Calculate points for each attribute in this row
                for (int i = startAttrIndex; i < endAttrIndex; i++) {
                    double value = data.get(i).get(row);
                    String attributeName = attributeNames.get(i);
                    
                    int xPos = startX + (i - startAttrIndex) * spacing;
                    int yPos;
                    
                    // Check if this is an empty entry (NaN only, not 0.0)
                    if (Double.isNaN(value)) {
                        // For empty entries, position them within the empty entry block at the bottom
                        double frequency = emptyEntryFrequencies.getOrDefault(attributeName, 0.0);
                        int blockHeight = (int) (frequency * AXIS_HEIGHT);
                        if (blockHeight < 5) blockHeight = 5;
                        
                        // Get the size of the consecutive empty block this entry belongs to
                        Map<Integer, Integer> blockSizes = emptyBlockSizes.getOrDefault(attributeName, new HashMap<>());
                        int consecutiveBlockSize = blockSizes.getOrDefault(row, 1);
                        
                        // Find position within the consecutive block
                        int positionInConsecutiveBlock = 0;
                        for (int checkRow = 0; checkRow < row; checkRow++) {
                            if (Double.isNaN(data.get(i).get(checkRow))) {
                                positionInConsecutiveBlock++;
                            } else {
                                positionInConsecutiveBlock = 0; // Reset if we hit a non-empty entry
                            }
                        }
                        
                        // Normalize position within the consecutive block
                        double normalizedPosition = (double) positionInConsecutiveBlock / Math.max(1, consecutiveBlockSize - 1);
                        yPos = startY + AXIS_HEIGHT - blockHeight + (int)(normalizedPosition * blockHeight);
                    } else {
                        // Normal value processing - adjust for empty block space and gap
                        double frequency = emptyEntryFrequencies.getOrDefault(attributeName, 0.0);
                        int blockHeight = (int) (frequency * AXIS_HEIGHT);
                        if (blockHeight < 5) blockHeight = 5;
                        
                        // Gap size is the size of one point (assuming point size of 8 pixels)
                        int gapSize = 8;
                        
                        // Available height for numerical data (excluding empty block and gap)
                        int availableHeight = AXIS_HEIGHT - blockHeight - gapSize;
                        
                        double normalizedValue = (value - globalMinValue) / (globalMaxValue - globalMinValue);
                        yPos = startY + availableHeight - (int)(normalizedValue * availableHeight);
                    }
                    
                    points.add(new Point(xPos, yPos));
                }
                
                // Draw polyline
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
} 