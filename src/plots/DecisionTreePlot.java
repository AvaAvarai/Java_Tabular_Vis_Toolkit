package src.plots;

import src.DecisionTree;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import src.utils.ScreenshotUtils;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public class DecisionTreePlot extends JPanel {

    private final DecisionTree.TreeNode root;
    private final List<String> attributeNames;
    private final Map<String, Color> classColors;
    private static final int BASE_NODE_HEIGHT = 60;  // Base vertical space between nodes
    private static final int NODE_HORIZONTAL_PADDING = 10;
    private static final int NODE_VERTICAL_PADDING = 5;
    private double zoomFactor = 0.5;  // Start zoomed out
    private int translateX = 0;
    private int translateY = 0;
    private int treeWidth;
    private int treeHeight;
    private Point lastMousePos;

    public DecisionTreePlot(DecisionTree.TreeNode root, List<String> attributeNames, Map<String, Color> classColors) {
        this.root = root;
        this.attributeNames = attributeNames;
        this.classColors = classColors;
        setPreferredSize(new Dimension(800, 600));  // Set a default size
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMousePos.x;
                int dy = e.getY() - lastMousePos.y;
                translateX += dx;
                translateY += dy;
                lastMousePos = e.getPoint();
                repaint();
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomChange = -0.05 * e.getPreciseWheelRotation();
                zoomFactor = Math.max(0.1, Math.min(2.0, zoomFactor + zoomChange));
                repaint();
            }
        });
        SwingUtilities.invokeLater(this::adjustPanelSize);  // Adjust size after component is fully initialized

        // Add screenshot functionality
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "saveScreenshot");
        getActionMap().put("saveScreenshot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the parent JScrollPane
                Container parent = getParent();
                while (parent != null && !(parent instanceof JScrollPane)) {
                    parent = parent.getParent();
                }
                if (parent instanceof JScrollPane) {
                    ScreenshotUtils.captureAndSaveScreenshot((JScrollPane)parent, "DecisionTree", "DecisionTree");
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Center the tree
        int centerX = getWidth() / 2 - (int)(treeWidth * zoomFactor / 2) + translateX;
        int centerY = getHeight() / 2 - (int)(treeHeight * zoomFactor / 2) + translateY;
        g2d.translate(centerX, centerY);
        
        g2d.scale(zoomFactor, zoomFactor);
        if (root != null) {
            drawTree(g2d, root, treeWidth / 2, 50);
        }
        g2d.dispose();
    }

    private void drawTree(Graphics2D g, DecisionTree.TreeNode node, int x, int y) {
        if (node != null) {
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            String mainText = node.isLeaf ? node.prediction : node.questionText;
            String caseCountText = node.caseCount > 0 ? "(" + node.caseCount + " cases)" : "";

            Rectangle nodeBounds = calculateNodeBounds(g, mainText, caseCountText);
            int nodeWidth = nodeBounds.width;
            int nodeHeight = nodeBounds.height;

            // Draw node background
            if (node.isLeaf && classColors.containsKey(node.prediction)) {
                g.setColor(classColors.get(node.prediction));
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRect(x - nodeWidth / 2, y, nodeWidth, nodeHeight);
            g.setColor(Color.BLACK);
            g.drawRect(x - nodeWidth / 2, y, nodeWidth, nodeHeight);

            // Draw main text
            g.drawString(mainText, x - g.getFontMetrics().stringWidth(mainText) / 2, y + NODE_VERTICAL_PADDING + g.getFontMetrics().getAscent());

            // Draw case count on a new line, if it exists
            if (!caseCountText.isEmpty()) {
                g.drawString(caseCountText, x - g.getFontMetrics().stringWidth(caseCountText) / 2, y + nodeHeight - NODE_VERTICAL_PADDING);
            }

            if (!node.isLeaf) {
                int leftWidth = calculateSubtreeWidth(node.left);
                int rightWidth = calculateSubtreeWidth(node.right);
                int totalWidth = leftWidth + rightWidth;

                int leftX = x - totalWidth / 2 + leftWidth / 2;
                int rightX = x + totalWidth / 2 - rightWidth / 2;
                int childY = y + nodeHeight + BASE_NODE_HEIGHT;

                if (node.left != null) {
                    g.drawLine(x, y + nodeHeight, leftX, childY);
                    drawTree(g, node.left, leftX, childY);
                }
                if (node.right != null) {
                    g.drawLine(x, y + nodeHeight, rightX, childY);
                    drawTree(g, node.right, rightX, childY);
                }
            }
        }
    }

    private Rectangle calculateNodeBounds(Graphics g, String mainText, String caseCountText) {
        if (g == null) {
            // Use a default size if Graphics is not available
            return new Rectangle(100, 50);
        }
        FontMetrics fm = g.getFontMetrics();
        int mainTextWidth = fm.stringWidth(mainText);
        int caseCountWidth = fm.stringWidth(caseCountText);
        int textWidth = Math.max(mainTextWidth, caseCountText.isEmpty() ? 0 : caseCountWidth);
        int width = textWidth + 2 * NODE_HORIZONTAL_PADDING;
        int height = (caseCountText.isEmpty() ? 1 : 2) * fm.getHeight() + 2 * NODE_VERTICAL_PADDING;
        return new Rectangle(width, height);
    }

    private int calculateSubtreeWidth(DecisionTree.TreeNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf) {
            Rectangle bounds = calculateNodeBounds(null, node.prediction, "(" + node.caseCount + " cases)");
            return bounds.width; // Add spacing between leaves
        }
        int leftWidth = calculateSubtreeWidth(node.left);
        int rightWidth = calculateSubtreeWidth(node.right);
        return leftWidth + rightWidth;
    }

    private int getTreeDepth(DecisionTree.TreeNode node) {
        if (node == null || node.isLeaf) {
            return 0;
        }
        return 1 + Math.max(getTreeDepth(node.left), getTreeDepth(node.right));
    }

    private void adjustPanelSize() {
        int treeDepth = getTreeDepth(root);
        treeWidth = calculateSubtreeWidth(root);
        treeHeight = (treeDepth + 1) * (BASE_NODE_HEIGHT + 50);

        int panelWidth = Math.max(800, treeWidth + 100);
        int panelHeight = Math.max(600, treeHeight + 100);

        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate();
        repaint();
    }
}
