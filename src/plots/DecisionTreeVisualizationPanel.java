package src.plots;

import src.DecisionTree;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class DecisionTreeVisualizationPanel extends JPanel {

    private final DecisionTree.TreeNode root;
    private final List<String> attributeNames;
    private static final int BASE_NODE_HEIGHT = 60;  // Base vertical space between nodes

    public DecisionTreeVisualizationPanel(DecisionTree.TreeNode root, List<String> attributeNames) {
        this.root = root;
        this.attributeNames = attributeNames;
        adjustPanelSize(root);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root != null) {
            int treeDepth = getTreeDepth(root);
            drawTree(g, root, getWidth() / 2, 50, BASE_NODE_HEIGHT, 0);
        }
    }

    private void drawTree(Graphics g, DecisionTree.TreeNode node, int x, int y, int verticalSpacing, int level) {
        if (node != null) {
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            String mainText = node.isLeaf ? node.prediction : node.questionText;
            String caseCountText = node.caseCount > 0 ? "(" + node.caseCount + " cases)" : "";
            int mainTextWidth = g.getFontMetrics().stringWidth(mainText);
            int caseCountWidth = g.getFontMetrics().stringWidth(caseCountText);

            // Draw main text
            g.drawString(mainText, x - mainTextWidth / 2, y);

            // Draw case count on a new line, if it exists
            if (!caseCountText.isEmpty()) {
                g.drawString(caseCountText, x - caseCountWidth / 2, y + 15);
            }

            if (!node.isLeaf) {
                int childY = y + verticalSpacing + level * 10;  // Adjust vertical spacing based on the level
                int nextHorizontalOffset = calculateHorizontalSpacing(level + 1);

                if (node.left != null) {
                    g.drawLine(x, y + 15, x - nextHorizontalOffset, childY);
                    drawTree(g, node.left, x - nextHorizontalOffset, childY, verticalSpacing, level + 1);
                }
                if (node.right != null) {
                    g.drawLine(x, y + 15, x + nextHorizontalOffset, childY);
                    drawTree(g, node.right, x + nextHorizontalOffset, childY, verticalSpacing, level + 1);
                }
            }
        }
    }

    private int calculateHorizontalSpacing(int level) {
        return (int) (300 / Math.pow(2, level));
    }

    private int getTreeDepth(DecisionTree.TreeNode node) {
        if (node == null || node.isLeaf) {
            return 0;
        }
        return 1 + Math.max(getTreeDepth(node.left), getTreeDepth(node.right));
    }

    private int getMaxWidth(DecisionTree.TreeNode node, int level) {
        if (node == null || node.isLeaf) {
            return calculateHorizontalSpacing(level);
        }
        int leftWidth = getMaxWidth(node.left, level + 1);
        int rightWidth = getMaxWidth(node.right, level + 1);
        return leftWidth + rightWidth + calculateHorizontalSpacing(level);
    }

    private void adjustPanelSize(DecisionTree.TreeNode root) {
        int treeDepth = getTreeDepth(root);
        int maxTreeWidth = getMaxWidth(root, 0);

        int panelWidth = Math.max(800, maxTreeWidth);
        int panelHeight = Math.max(600, (treeDepth + 1) * (BASE_NODE_HEIGHT + 10)) + 100;

        setPreferredSize(new Dimension(panelWidth, panelHeight));
        revalidate();
        repaint();
    }
}
