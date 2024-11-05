package src.plots;

import javax.swing.*;
import src.utils.ScreenshotUtils;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Arrays;

public class ConcentricCoordinatesPlot extends JFrame {

    private List<List<Double>> data;
    private List<String> attributeNames;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private List<String> classLabels;
    private List<Integer> selectedRows;
    private List<Integer> hiddenRows;
    private double globalMaxValue;
    private ConcentricCoordinatesPanel plotPanel;
    private double piAdjustment = 0.05;
    private boolean showLabels = true;
    private boolean closeLoop = true;
    private Map<String, Double> attributeRotations = new HashMap<>();

    // Font settings
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
    private static final Font AXIS_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final int TITLE_PADDING = 20;

    public ConcentricCoordinatesPlot(List<List<Double>> data, List<String> attributeNames, Map<String, Color> classColors, Map<String, Shape> classShapes, List<String> classLabels, List<Integer> selectedRows, List<Integer> hiddenRows, String datasetName) {
        this.data = data;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        this.classShapes = classShapes;
        this.classLabels = classLabels;
        this.selectedRows = selectedRows;
        this.hiddenRows = hiddenRows;

        // Initialize rotation values for each attribute
        for (String attribute : attributeNames) {
            attributeRotations.put(attribute, 0.0);
        }

        // Calculate the global maximum value across all attributes
        this.globalMaxValue = data.stream()
            .flatMap(List::stream)
            .max(Double::compare)
            .orElse(1.0);

        setTitle("Concentric Coordinates Plot");
        setSize(800, 800); // Increased height to accommodate sliders
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set up the main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Add the plot panel
        plotPanel = new ConcentricCoordinatesPanel();
        plotPanel.setPreferredSize(new Dimension(800, 600));

        // Add the plot panel to a scroll pane
        JScrollPane scrollPane = new JScrollPane(plotPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add a key listener for the space bar to save a screenshot
        scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        scrollPane.getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScreenshotUtils.captureAndSaveScreenshot(scrollPane, "ConcentricCoordinates", datasetName);
            }
        });

        // Create control panel for sliders and toggles
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(Color.WHITE);
        
        // Add PI adjustment slider
        JPanel globalControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        globalControlPanel.setBackground(Color.WHITE);
        
        JLabel sliderLabel = new JLabel("PI Adjustment: ");
        JSlider piSlider = new JSlider(JSlider.HORIZONTAL, 0, 360*2, 5);
        piSlider.setMajorTickSpacing(360);
        piSlider.setMinorTickSpacing(0);
        piSlider.setPaintTicks(true);
        piSlider.setPaintLabels(true);
        // use an array of labels to make sure the labels are aligned properly
        String[] labels = {"0", "2π", "4π"};
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(360, new JLabel("2π"));
        labelTable.put(720, new JLabel("4π"));
        piSlider.setLabelTable(labelTable);
        piSlider.addChangeListener(e -> {
            piAdjustment = piSlider.getValue() / 100.0;
            plotPanel.repaint();
        });

        // Add label toggle button
        JToggleButton labelToggle = new JToggleButton("Show Labels", true);
        labelToggle.addActionListener(e -> {
            showLabels = labelToggle.isSelected();
            plotPanel.repaint();
        });

        // Add loop toggle button
        JToggleButton loopToggle = new JToggleButton("Close Loop", true);
        loopToggle.addActionListener(e -> {
            closeLoop = loopToggle.isSelected();
            plotPanel.repaint();
        });

        globalControlPanel.add(sliderLabel);
        globalControlPanel.add(piSlider);
        globalControlPanel.add(labelToggle);
        globalControlPanel.add(loopToggle);
        
        controlPanel.add(globalControlPanel);

        // Add individual attribute rotation sliders
        JPanel attributeSlidersPanel = new JPanel();
        attributeSlidersPanel.setLayout(new BoxLayout(attributeSlidersPanel, BoxLayout.Y_AXIS));
        attributeSlidersPanel.setBackground(Color.WHITE);
        attributeSlidersPanel.setBorder(BorderFactory.createTitledBorder("Attribute Rotations"));

        for (String attribute : attributeNames) {
            JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            sliderPanel.setBackground(Color.WHITE);
            JLabel label = new JLabel(attribute + ": ");
            JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 360, 0);
            slider.setPreferredSize(new Dimension(200, 40));
            slider.addChangeListener(e -> {
                attributeRotations.put(attribute, slider.getValue() * Math.PI / 180);
                plotPanel.repaint();
            });
            sliderPanel.add(label);
            sliderPanel.add(slider);
            attributeSlidersPanel.add(sliderPanel);
        }

        JScrollPane sliderScrollPane = new JScrollPane(attributeSlidersPanel);
        sliderScrollPane.setPreferredSize(new Dimension(300, 150));
        controlPanel.add(sliderScrollPane);

        // Ensure the JFrame is focusable to capture key events
        setFocusable(true);
        requestFocusInWindow();

        // Add components to main panel
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(createLegendPanel(), BorderLayout.SOUTH);
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        setContentPane(mainPanel);
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

    private class ConcentricCoordinatesPanel extends JPanel {
        public ConcentricCoordinatesPanel() {
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

            // Set the background color for the entire panel to white
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw the title
            String title = "Concentric Coordinates Plot";
            g2.setFont(TITLE_FONT);
            FontMetrics fm = g2.getFontMetrics(TITLE_FONT);
            int titleWidth = fm.stringWidth(title);
            int titleHeight = fm.getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString(title, (getWidth() - titleWidth) / 2, titleHeight);

            // Fill the plot area with light grey background
            g2.setColor(new Color(0xC0C0C0));
            g2.fillRect(0, titleHeight + TITLE_PADDING, getWidth(), getHeight() - titleHeight - TITLE_PADDING);

            // Calculate center and radius for the plot
            int centerX = getWidth() / 2;
            int centerY = (getHeight() - titleHeight - TITLE_PADDING) / 2 + titleHeight + TITLE_PADDING;
            int maxRadius = Math.min(centerX, (getHeight() - titleHeight - TITLE_PADDING) / 2) - 50;

            // Draw concentric axes
            drawConcentricAxes(g2, centerX, centerY, maxRadius);

            // Draw the concentric coordinates for each data point
            for (int row = 0; row < data.get(0).size(); row++) {
                drawConcentricCoordinates(g2, row, centerX, centerY, maxRadius);
            }

            // Draw attribute labels if enabled
            if (showLabels) {
                drawAttributeLabels(g2, centerX, centerY, maxRadius);
            }

            // Draw highlights for selected rows
            drawHighlights(g2, centerX, centerY, maxRadius);
        }

        private void drawConcentricAxes(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setColor(Color.BLACK);
            int numAttributes = attributeNames.size();
            for (int i = 1; i <= numAttributes; i++) {
                int currentRadius = i * (maxRadius / numAttributes);
                g2.draw(new Ellipse2D.Double(centerX - currentRadius, centerY - currentRadius, 2 * currentRadius, 2 * currentRadius));
            }
        }

        private void drawConcentricCoordinates(Graphics2D g2, int row, int centerX, int centerY, int maxRadius) {
            int numAttributes = attributeNames.size();
            Point2D.Double[] points = new Point2D.Double[numAttributes];

            for (int i = 0; i < numAttributes; i++) {
                double value = data.get(i).get(row);
                double normalizedValue = value / globalMaxValue;

                // Apply both global and individual attribute rotations
                double attributeRotation = attributeRotations.get(attributeNames.get(i));
                double angle = -(Math.PI - piAdjustment) / 2 + normalizedValue * 2 * (Math.PI - piAdjustment) + attributeRotation;
                
                int currentRadius = (i + 1) * (maxRadius / numAttributes);
                double x = centerX + currentRadius * Math.cos(angle);
                double y = centerY + currentRadius * Math.sin(angle);

                points[i] = new Point2D.Double(x, y);
            }

            String classLabel = classLabels.get(row);
            Color color = classColors.getOrDefault(classLabel, Color.BLACK);
            g2.setColor(color);

            // Draw lines connecting the points across the concentric circles
            g2.setStroke(new BasicStroke(1.0f));
            for (int i = 0; i < numAttributes - 1; i++) {
                g2.draw(new Line2D.Double(points[i], points[i + 1]));
            }
            
            if (closeLoop) {
                g2.draw(new Line2D.Double(points[numAttributes - 1], points[0]));
            }

            // Draw the shapes at the points
            for (int i = 0; i < numAttributes; i++) {
                g2.translate(points[i].x, points[i].y);
                Shape shape = classShapes.getOrDefault(classLabel, new Ellipse2D.Double(-3, -3, 6, 6));
                g2.fill(shape);
                g2.translate(-points[i].x, -points[i].y);
            }
        }

        private void drawHighlights(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2.0f));

            for (int row : selectedRows) {
                int numAttributes = attributeNames.size();
                Point2D.Double[] points = new Point2D.Double[numAttributes];

                for (int i = 0; i < numAttributes; i++) {
                    double value = data.get(i).get(row);
                    double normalizedValue = value / globalMaxValue;

                    double attributeRotation = attributeRotations.get(attributeNames.get(i));
                    double angle = -(Math.PI - piAdjustment) / 2 + normalizedValue * 2 * (Math.PI - piAdjustment) + attributeRotation;
                    
                    int currentRadius = (i + 1) * (maxRadius / numAttributes);
                    double x = centerX + currentRadius * Math.cos(angle);
                    double y = centerY + currentRadius * Math.sin(angle);
                    
                    points[i] = new Point2D.Double(x, y);
                }

                for (int i = 0; i < numAttributes - 1; i++) {
                    g2.draw(new Line2D.Double(points[i], points[i + 1]));
                }
                
                if (closeLoop) {
                    g2.draw(new Line2D.Double(points[numAttributes - 1], points[0]));
                }

                for (int i = 0; i < numAttributes; i++) {
                    g2.translate(points[i].x, points[i].y);
                    Shape shape = classShapes.getOrDefault(classLabels.get(row), new Ellipse2D.Double(-4.5, -4.5, 9, 9));
                    g2.fill(shape);
                    g2.translate(-points[i].x, -points[i].y);
                }
            }
        }

        private void drawAttributeLabels(Graphics2D g2, int centerX, int centerY, int maxRadius) {
            g2.setFont(AXIS_LABEL_FONT);
            g2.setColor(Color.BLACK);

            int numAttributes = attributeNames.size();
            for (int i = 0; i < numAttributes; i++) {
                int currentRadius = (i + 1) * (maxRadius / numAttributes);
                
                // Apply rotation to label position
                double attributeRotation = attributeRotations.get(attributeNames.get(i));
                double labelAngle = -(Math.PI / 2) + attributeRotation; // Start from top (12 o'clock)
                
                int x = centerX + (int)((currentRadius + 20) * Math.cos(labelAngle));
                int y = centerY + (int)((currentRadius + 20) * Math.sin(labelAngle));

                g2.drawString(attributeNames.get(i), x, y);
            }
        }
    }
}
