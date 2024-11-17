package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class ParallelCoordinatesPlot extends JFrame {

    private final List<List<Double>> data;
    private final List<String> attributeNames;
    private final Map<String, Color> classColors;
    private final Map<String, Shape> classShapes;
    private final List<String> classLabels;
    private final List<Integer> selectedRows;
    private final Map<String, Boolean> hiddenClasses;
    private final Map<String, Boolean> axisDirections;
    private final Map<String, Point2D.Double> axisPositions;
    private String draggedAxis = null;
    private final double globalMaxValue;
    private final double globalMinValue;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int AXIS_HEIGHT = 400;

    public ParallelCoordinatesPlot(List<List<Double>> data, List<String> attributeNames,
                                   Map<String, Color> classColors, Map<String, Shape> classShapes,
                                   List<String> classLabels, List<Integer> selectedRows, String datasetName) {
        this.data = data;
        this.attributeNames = new ArrayList<>(attributeNames);
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.hiddenClasses = new HashMap<>();
        this.axisDirections = new HashMap<>();
        this.axisPositions = new HashMap<>();

        // Calculate global max and min values
        this.globalMaxValue = data.stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        this.globalMinValue = data.stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        // Initialize axis positions
        int startX = 100;
        int spacing = 150;
        for (int i = 0; i < attributeNames.size(); i++) {
            axisPositions.put(attributeNames.get(i), new Point2D.Double(startX + i * spacing, 100));
        }

        setTitle("Parallel Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        ParallelCoordinatesPanel plotPanel = new ParallelCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(attributeNames.size() * 150, 600)); // Adjust to fit all axes horizontally

        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ParallelCoordinates", datasetName);
            }
        });

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JScrollPane createControlPanel() {
        // Create a panel to hold the buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setPreferredSize(new Dimension(attributeNames.size() * 150, 50));
    
        // Add axis direction toggle buttons for each attribute
        for (String attributeName : attributeNames) {
            JToggleButton axisDirectionToggle = new JToggleButton(attributeName + " Axis Direction");
            axisDirectionToggle.addActionListener(e -> {
                boolean isToggled = axisDirectionToggle.isSelected();
                axisDirections.put(attributeName, isToggled);
                repaint();
            });
            controlPanel.add(axisDirectionToggle);
        }
    
        // Wrap the control panel in a scroll pane for horizontal scrolling
        JScrollPane scrollPane = new JScrollPane(controlPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // No vertical scrolling needed
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Optional: remove borders for a clean look
    
        // Return the scroll pane instead of the plain control panel
        return scrollPane;
    }    

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legendPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.get(className);

            JPanel colorLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorLabelPanel.setBackground(Color.WHITE);
            colorLabelPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel shapeLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(hiddenClasses.getOrDefault(className, false) ? Color.LIGHT_GRAY : color);
                    g2.translate(32, 20);
                    g2.scale(2, 2);
                    g2.fill(shape);
                }
            };
            shapeLabel.setPreferredSize(new Dimension(40, 40));

            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
            label.setForeground(hiddenClasses.getOrDefault(className, false) ? Color.LIGHT_GRAY : Color.BLACK);

            colorLabelPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hiddenClasses.getOrDefault(className, false)) {
                        hiddenClasses.remove(className);
                    } else {
                        hiddenClasses.put(className, true);
                    }
                    repaint();
                }
            });

            colorLabelPanel.add(shapeLabel);
            colorLabelPanel.add(label);
            legendPanel.add(colorLabelPanel);
        }

        return legendPanel;
    }

    private class ParallelCoordinatesPanel extends JPanel {
        private final List<String> visualOrder; // Tracks the visual order of the axes
    
        public ParallelCoordinatesPanel() {
            setBackground(new Color(0xC0C0C0));
    
            // Initialize the visual order of the axes (matches attributeNames initially)
            visualOrder = new ArrayList<>(attributeNames);
    
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (String attributeName : visualOrder) {
                        Point2D.Double pos = axisPositions.get(attributeName);
                        if (Math.abs(e.getX() - pos.x) < 10) {
                            draggedAxis = attributeName;
                            break;
                        }
                    }
                }
    
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedAxis != null) {
                        // Reorder the visual order based on the updated X positions
                        visualOrder.sort(Comparator.comparingDouble(attr -> axisPositions.get(attr).x));
                    }
                    draggedAxis = null;
                    repaint();
                }
            });
    
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedAxis != null) {
                        Point2D.Double pos = axisPositions.get(draggedAxis);
                        pos.x = e.getX();
                        repaint();
                    }
                }
            });
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            // Draw the title on white background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), 50); // Assuming 50 as the height for the title
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth("Parallel Coordinates Plot");
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK); // Set text color to black
            g2.drawString("Parallel Coordinates Plot", (getWidth() - titleWidth) / 2, titleHeight);
            
            // Draw the plot on c0c0c0 background
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, 50, getWidth(), getHeight() - 50); // Adjusted to start from below the title
            drawAxes(g2);
            drawData(g2);
        }
        private void drawAxes(Graphics2D g2) {
            g2.setColor(Color.BLACK);
    
            for (String attributeName : visualOrder) { // Use visualOrder to draw the axes
                Point2D.Double pos = axisPositions.get(attributeName);
                g2.drawLine((int) pos.x, (int) pos.y, (int) pos.x, (int) pos.y + AXIS_HEIGHT);
    
                // Draw attribute label
                g2.setFont(AXIS_LABEL_FONT);
                String label = attributeName;
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, (int) (pos.x - labelWidth / 2), (int) (pos.y + AXIS_HEIGHT + 20));
            }
        }
    
        private void drawData(Graphics2D g2) {
            // Draw non-selected data first
            for (int row = 0; row < data.get(0).size(); row++) {
                if (!selectedRows.contains(row)) {
                    List<Point2D.Double> points = new ArrayList<>();
    
                    for (String attributeName : visualOrder) { // Use visualOrder for drawing polylines
                        int attributeIndex = attributeNames.indexOf(attributeName); // Get the original attribute index
                        double value = data.get(attributeIndex).get(row);
                        double normalizedValue = (value - globalMinValue) / (globalMaxValue - globalMinValue);
    
                        if (axisDirections.getOrDefault(attributeName, false)) {
                            normalizedValue = 1 - normalizedValue;
                        }
    
                        Point2D.Double pos = axisPositions.get(attributeName);
                        double y = pos.y + AXIS_HEIGHT - normalizedValue * AXIS_HEIGHT;
                        points.add(new Point2D.Double(pos.x, y));
                    }
    
                    String classLabel = classLabels.get(row);
                    if (hiddenClasses.containsKey(classLabel)) continue;
    
                    g2.setColor(classColors.getOrDefault(classLabel, Color.BLACK));
                    g2.setStroke(new BasicStroke(1.0f)); // Default line thickness
                    for (int i = 0; i < points.size() - 1; i++) {
                        g2.draw(new Line2D.Double(points.get(i), points.get(i + 1)));
                    }
    
                    // Draw class symbols as vertices
                    for (Point2D.Double point : points) {
                        Shape shape = classShapes.get(classLabel);
                        g2.translate(point.x, point.y);
                        g2.fill(shape);
                        g2.translate(-point.x, -point.y);
                    }
                }
            }
    
            // Draw selected data last to draw on top
            for (int row : selectedRows) {
                List<Point2D.Double> points = new ArrayList<>();
    
                for (String attributeName : visualOrder) { // Use visualOrder for drawing polylines
                    int attributeIndex = attributeNames.indexOf(attributeName); // Get the original attribute index
                    double value = data.get(attributeIndex).get(row);
                    double normalizedValue = (value - globalMinValue) / (globalMaxValue - globalMinValue);
    
                    if (axisDirections.getOrDefault(attributeName, false)) {
                        normalizedValue = 1 - normalizedValue;
                    }
    
                    Point2D.Double pos = axisPositions.get(attributeName);
                    double y = pos.y + AXIS_HEIGHT - normalizedValue * AXIS_HEIGHT;
                    points.add(new Point2D.Double(pos.x, y));
                }
    
                String classLabel = classLabels.get(row);
                if (hiddenClasses.containsKey(classLabel)) continue;
    
                g2.setColor(Color.YELLOW); // Highlight selected cases
                g2.setStroke(new BasicStroke(2.0f)); // Thicker line for selected cases
                for (int i = 0; i < points.size() - 1; i++) {
                    g2.draw(new Line2D.Double(points.get(i), points.get(i + 1)));
                }
    
                // Draw class symbols as vertices
                for (Point2D.Double point : points) {
                    Shape shape = classShapes.get(classLabel);
                    g2.translate(point.x, point.y);
                    g2.fill(shape);
                    g2.translate(-point.x, -point.y);
                }
            }
        }
    }
}
