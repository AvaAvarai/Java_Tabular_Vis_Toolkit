package src.utils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Map;
import java.util.Set;

public class LegendUtils {
    private static final Font LEGEND_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final int SWATCH_SIZE = 20;
    private static final int PADDING = 5;
    private static final int SHAPE_SCALE = 3; // Scale factor for the shapes

    public static JComponent createLegendPanel(Map<String, Color> classColors, Map<String, Shape> classShapes, Set<String> hiddenClasses) {
        // Create the inner panel that will hold the legend items
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        legendPanel.setBackground(Color.WHITE);

        for (Map.Entry<String, Color> entry : classColors.entrySet()) {
            String className = entry.getKey();
            Color color = entry.getValue();
            Shape shape = classShapes.getOrDefault(className, new Rectangle(-4, -4, 8, 8));

            JToggleButton legendItem = createLegendItem(className, color, shape);
            legendItem.setSelected(!hiddenClasses.contains(className));
            legendItem.addActionListener(e -> {
                if (legendItem.isSelected()) {
                    hiddenClasses.remove(className);
                } else {
                    hiddenClasses.add(className);
                }
                // Find the parent plot and repaint it
                Component parent = legendPanel.getParent();
                while (parent != null && !(parent instanceof JFrame)) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    parent.repaint();
                }
            });

            legendPanel.add(legendItem);
        }

        // Create a scroll pane for the legend
        JScrollPane legendScrollPane = new JScrollPane(legendPanel);
        legendScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        legendScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        legendScrollPane.setBorder(BorderFactory.createTitledBorder("Legend"));
        legendScrollPane.setPreferredSize(new Dimension(800, 90));
        legendScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); // Limit height
        
        // Customize the scrollbar appearance
        legendScrollPane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(100, 100, 100);
                this.trackColor = new Color(200, 200, 200);
            }
        });

        return legendScrollPane;
    }

    private static JToggleButton createLegendItem(String className, Color color, Shape shape) {
        JToggleButton button = new JToggleButton(className) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw scaled shape with color
                g2.setColor(isSelected() ? color : Color.LIGHT_GRAY);
                AffineTransform transform = new AffineTransform();
                transform.translate(PADDING + SWATCH_SIZE/2, getHeight()/2);
                transform.scale(SHAPE_SCALE, SHAPE_SCALE);
                Shape scaledShape = transform.createTransformedShape(shape);
                g2.fill(scaledShape);
            }
        };

        button.setFont(LEGEND_FONT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        Dimension size = new Dimension(150, 30);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setFocusPainted(false);
        
        return button;
    }
} 