package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import src.utils.LegendUtils;

public class LineCoordinatesPlot extends JFrame {
    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private String datasetName;
    private Map<String, Double> axisScales;
    private Map<String, Boolean> axisDirections;
    private Set<String> hiddenClasses;
    private boolean showConnections = true;
    private Map<String, Point> axisPositions;
    private Map<String, Integer> curveHeights; // Map to store individual curve heights for each attribute
    private String draggedAxis = null;
    private boolean showLabels = true;
    private Color backgroundColor;
    private float polylineThickness;

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int TITLE_PADDING = 20;
    private static final int AXIS_LENGTH = 400;

    public LineCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, 
            Map<String, Color> classColors, Map<String, Shape> classShapes, 
            List<String> classLabels, List<Integer> selectedRows, String datasetName, Color backgroundColor, float polylineThickness) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.backgroundColor = backgroundColor;
        this.datasetName = datasetName;
        this.axisScales = new HashMap<>();
        this.axisDirections = new HashMap<>();
        this.hiddenClasses = new HashSet<>();
        this.axisPositions = new HashMap<>();
        this.curveHeights = new HashMap<>(); // Initialize curveHeights map

        int startY = 200;
        int totalWidth = 0;
        for (int i = 0; i < attributeNames.size(); i++) {
            String attr = attributeNames.get(i);
            axisScales.put(attr, 1.0);  // Default scale
            axisDirections.put(attr, true);  // Default direction
            int x = i * AXIS_LENGTH + 50; // Ensure axes are placed sequentially with proper spacing
            int y = startY; // Dynamically calculate Y position as half the panel's height
            axisPositions.put(attr, new Point(x, y));
            curveHeights.put(attr, 50);  // Default curve height
            totalWidth = x + AXIS_LENGTH + 50; // Update total width needed
        }        

        setTitle("Line Coordinates Plot");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the main panel with vertical layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Create control panel at the top
        JPanel controlPanel = createControlPanel();
        JScrollPane controlScrollPane = new JScrollPane(controlPanel);
        controlScrollPane.setPreferredSize(new Dimension(getWidth(), 150));
        controlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(controlScrollPane, BorderLayout.NORTH);

        // Create main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        // Add the plot panel with scrolling
        LineCoordinatesPanel plotPanel = new LineCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(totalWidth, 600)); // Set preferred size based on total width needed
        
        JScrollPane plotScrollPane = new JScrollPane(plotPanel);
        plotScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        plotScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        plotScrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                 .put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        plotScrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(plotScrollPane, "LineCoordinates", datasetName);
            }
        });

        contentPanel.add(plotScrollPane, BorderLayout.CENTER);
        contentPanel.add(LegendUtils.createLegendPanel(classColors, classShapes, hiddenClasses), BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add label toggle button at the start
        JToggleButton labelToggle = new JToggleButton("Show Labels", true);
        labelToggle.addActionListener(e -> {
            showLabels = labelToggle.isSelected();
            repaint();
        });
        controlPanel.add(labelToggle);
        
        // Add controls for each attribute
        for (String attr : attributeNames) {
            JPanel attrPanel = new JPanel();
            attrPanel.setBorder(BorderFactory.createTitledBorder(attr));
            
            // Scale slider
            JPanel scalePanel = new JPanel(new BorderLayout());
            scalePanel.add(new JLabel("Scale:"), BorderLayout.WEST);
            JSlider scaleSlider = new JSlider(0, 200, 100);
            scaleSlider.addChangeListener(e -> {
                axisScales.put(attr, scaleSlider.getValue() / 100.0);
                repaint();
            });
            scalePanel.add(scaleSlider);
            
            // Direction toggle
            JToggleButton directionToggle = new JToggleButton("\u2B05");
            directionToggle.addActionListener(e -> {
                axisDirections.put(attr, !directionToggle.isSelected());
                directionToggle.setText(directionToggle.isSelected() ? "\u27A1" : "\u2B05");
                repaint();
            });
            // Curve height slider
            JPanel curveHeightPanel = new JPanel(new BorderLayout());
            curveHeightPanel.add(new JLabel("Curve Height:"), BorderLayout.WEST);
            JSlider curveHeightSlider = new JSlider(-100, 100, curveHeights.get(attr));
            curveHeightSlider.addChangeListener(e -> {
                curveHeights.put(attr, curveHeightSlider.getValue());
                repaint();
            });
            curveHeightPanel.add(curveHeightSlider);
            
            attrPanel.add(scalePanel);
            attrPanel.add(directionToggle);
            attrPanel.add(curveHeightPanel);
            controlPanel.add(attrPanel);
        }
        
        return controlPanel;
    }

    private class LineCoordinatesPanel extends JPanel {
        public LineCoordinatesPanel() {
            setBackground(backgroundColor);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (String attr : attributeNames) {
                        Point pos = axisPositions.get(attr);
                        if (Math.abs(e.getY() - pos.y) < 10 && 
                            e.getX() >= pos.x && e.getX() <= pos.x + AXIS_LENGTH) {
                            draggedAxis = attr;
                            break;
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggedAxis = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedAxis != null) {
                        Point pos = axisPositions.get(draggedAxis);
                        pos.x = e.getX();
                        pos.y = e.getY();
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

            // Draw title
            g2.setFont(TITLE_FONT);
            String title = "Line Coordinates Plot";
            FontMetrics fm = g2.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            g2.drawString(title, (getWidth() - titleWidth) / 2, fm.getHeight());

            // Draw horizontal axes
            g2.setFont(AXIS_LABEL_FONT);
            for (String attr : attributeNames) {
                Point pos = axisPositions.get(attr);
                g2.setColor(Color.BLACK);
                int scaledAxisLength = (int) (AXIS_LENGTH * axisScales.get(attr));
                g2.drawLine(pos.x, pos.y, pos.x + scaledAxisLength, pos.y);
                
                // Only draw labels if showLabels is true
                if (showLabels) {
                    g2.drawString(attr, pos.x + scaledAxisLength / 2, pos.y + 20);
                }
            }
            
            // Draw data points and connections
            for (int row = 0; row < data.get(0).size(); row++) {
                if (hiddenClasses.contains(classLabels.get(row))) {
                    continue;
                }

                Color classColor = classColors.get(classLabels.get(row));
                g2.setColor(classColor);

                List<Point2D.Double> points = new ArrayList<>();
                for (int i = 0; i < attributeNames.size(); i++) {
                    String attr = attributeNames.get(i);
                    double value = data.get(i).get(row);
                    double minValue = Collections.min(data.get(i));
                    double maxValue = Collections.max(data.get(i));
                    
                    double normalizedValue = (value - minValue) / (maxValue - minValue);
                    if (!axisDirections.get(attr)) {
                        normalizedValue = 1 - normalizedValue;
                    }
                    
                    double scale = axisScales.get(attr);
                    Point pos = axisPositions.get(attr);
                    double x = pos.x + normalizedValue * scale * AXIS_LENGTH;
                    points.add(new Point2D.Double(x, pos.y));
                }

                // Draw connections between points using Bezier curves
                if (showConnections) {
                    g2.setStroke(new BasicStroke(Math.max(1.0f, polylineThickness))); // Set stroke before drawing curves
                    for (int i = 0; i < points.size() - 1; i++) {
                        Point2D.Double p1 = points.get(i);
                        Point2D.Double p2 = points.get(i + 1);
                        
                        // Control points for Bezier curve directly above axes
                        Point2D.Double ctrl1 = new Point2D.Double(
                            p1.x,
                            p1.y - curveHeights.get(attributeNames.get(i))
                        );
                        Point2D.Double ctrl2 = new Point2D.Double(
                            p2.x,
                            p2.y - curveHeights.get(attributeNames.get(i + 1))
                        );
                        
                        CubicCurve2D curve = new CubicCurve2D.Double(
                            p1.x, p1.y, ctrl1.x, ctrl1.y,
                            ctrl2.x, ctrl2.y, p2.x, p2.y
                        );
                        g2.draw(curve);
                    }
                }
                
                // Draw points with scaling
                Shape shape = classShapes.get(classLabels.get(row));
                for (Point2D.Double point : points) {
                    AffineTransform transform = new AffineTransform();
                    transform.translate(point.x, point.y);
                    String attr = attributeNames.get(points.indexOf(point));
                    double scale = axisScales.get(attr);
                    transform.scale(scale, scale);
                    Shape scaledShape = transform.createTransformedShape(shape);
                    g2.fill(scaledShape);
                }
            }

            // Highlight selected cases in yellow thicker drawn last above everything
            for (int row : selectedRows) {
                if (hiddenClasses.contains(classLabels.get(row))) {
                    continue;
                }

                Color classColor = Color.YELLOW; // Highlight selected cases in yellow
                g2.setColor(classColor);
                List<Point2D.Double> points = new ArrayList<>();
                for (int i = 0; i < attributeNames.size(); i++) {
                    String attr = attributeNames.get(i);
                    double value = data.get(i).get(row);
                    double minValue = Collections.min(data.get(i));
                    double maxValue = Collections.max(data.get(i));
                    
                    double normalizedValue = (value - minValue) / (maxValue - minValue);
                    if (!axisDirections.get(attr)) {
                        normalizedValue = 1 - normalizedValue;
                    }
                    
                    double scale = axisScales.get(attr);
                    Point pos = axisPositions.get(attr);
                    double x = pos.x + normalizedValue * scale * AXIS_LENGTH;
                    points.add(new Point2D.Double(x, pos.y));
                }

                // Draw connections between points using Bezier curves
                if (showConnections) {
                    g2.setStroke(new BasicStroke(polylineThickness)); // Set stroke for selected curves
                    for (int i = 0; i < points.size() - 1; i++) {
                        Point2D.Double p1 = points.get(i);
                        Point2D.Double p2 = points.get(i + 1);
                        
                        // Control points for Bezier curve directly above axes
                        Point2D.Double ctrl1 = new Point2D.Double(
                            p1.x,
                            p1.y - curveHeights.get(attributeNames.get(i))
                        );
                        Point2D.Double ctrl2 = new Point2D.Double(
                            p2.x,
                            p2.y - curveHeights.get(attributeNames.get(i + 1))
                        );
                        
                        CubicCurve2D curve = new CubicCurve2D.Double(
                            p1.x, p1.y, ctrl1.x, ctrl1.y,
                            ctrl2.x, ctrl2.y, p2.x, p2.y
                        );
                        g2.setStroke(new BasicStroke(polylineThickness));
                        g2.draw(curve);
                    }
                }
                
                // Draw points with scaling
                Shape shape = classShapes.get(classLabels.get(row));
                for (Point2D.Double point : points) {
                    AffineTransform transform = new AffineTransform();
                    transform.translate(point.x, point.y);
                    String attr = attributeNames.get(points.indexOf(point));
                    double scale = axisScales.get(attr);
                    transform.scale(scale, scale);
                    Shape scaledShape = transform.createTransformedShape(shape);
                    g2.fill(scaledShape);
                }
            }
        }
    }
}