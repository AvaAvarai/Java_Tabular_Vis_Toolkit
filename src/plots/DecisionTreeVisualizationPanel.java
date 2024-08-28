package src.plots;

import src.DecisionTree;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class DecisionTreeVisualizationPanel extends JPanel {

    private final DecisionTree.TreeNode root;
    private final List<String> attributeNames;

    public DecisionTreeVisualizationPanel(DecisionTree.TreeNode root, List<String> attributeNames) {
        this.root = root;
        this.attributeNames = attributeNames;
        setPreferredSize(new Dimension(800, 600)); // Set initial size, can be adjusted dynamically
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root != null) {
            drawTree(g, root, getWidth() / 2, 50, getWidth() / 4, 0);
        }
    }

    private void drawTree(Graphics g, DecisionTree.TreeNode node, int x, int y, int horizontalOffset, int level) {
        if (node != null) {
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            String text = node.isLeaf ? "Predict: " + node.prediction + " (" + node.caseCount + " cases)" 
                                      : node.questionText;
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, x - textWidth / 2, y);

            if (!node.isLeaf) {
                int childY = y + 50;
                if (node.left != null) {
                    g.drawLine(x, y, x - horizontalOffset, childY);
                    drawTree(g, node.left, x - horizontalOffset, childY, horizontalOffset / 2, level + 1);
                }
                if (node.right != null) {
                    g.drawLine(x, y, x + horizontalOffset, childY);
                    drawTree(g, node.right, x + horizontalOffset, childY, horizontalOffset / 2, level + 1);
                }
            }
        }
    }
}
