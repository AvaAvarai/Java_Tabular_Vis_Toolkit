package src.plots;

import src.DecisionTree;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class DecisionTreeVisualizationPanel extends JPanel {

    private final DecisionTree.TreeNode root;
    private final List<String> attributeNames;
    private static final int BASE_NODE_HEIGHT = 60;  // Base vertical space between nodes
    private static final int NODE_HORIZONTAL_PADDING = 10;
    private static final int NODE_VERTICAL_PADDING = 5;
    private static final int NODE_SPACING = 20;  // Spacing between nodes

    public DecisionTreeVisualizationPanel(DecisionTree.TreeNode root, List<String> attributeNames) {
        this.root = root;
        this.attributeNames = attributeNames;
        setPreferredSize(new Dimension(800, 600));  // Set a default size
        SwingUtilities.invokeLater(this::adjustPanelSize);  // Adjust size after component is fully initialized
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root != null) {
            drawTree(g, root, getWidth() / 2, 50);
        }
    }

    private void drawTree(Graphics g, DecisionTree.TreeNode node, int x, int y) {
        if (node != null) {
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            String mainText = node.isLeaf ? node.prediction : node.questionText;
            String caseCountText = node.caseCount > 0 ? "(" + node.caseCount + " cases)" : "";

            Rectangle nodeBounds = calculateNodeBounds(g, mainText, caseCountText);
            int nodeWidth = nodeBounds.width;
            int nodeHeight = nodeBounds.height;

            // Draw node background
            g.setColor(Color.WHITE);
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
                int totalWidth = leftWidth + rightWidth + NODE_SPACING;

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
            return bounds.width + NODE_SPACING; // Add spacing between leaves
        }
        int leftWidth = calculateSubtreeWidth(node.left);
        int rightWidth = calculateSubtreeWidth(node.right);
        return leftWidth + rightWidth + NODE_SPACING;
    }

    private int getTreeDepth(DecisionTree.TreeNode node) {
        if (node == null || node.isLeaf) {
            return 0;
        }
        return 1 + Math.max(getTreeDepth(node.left), getTreeDepth(node.right));
    }

    private void adjustPanelSize() {
        int treeDepth = getTreeDepth(root);
        int treeWidth = calculateSubtreeWidth(root);

        int panelWidth = Math.max(800, treeWidth + 100);
        int panelHeight = Math.max(600, (treeDepth + 1) * (BASE_NODE_HEIGHT + 50)) + 100;

        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate();
        repaint();
    }
}
