package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;

public class UIHelper {

    public static JTextArea createTextArea(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return textArea;
    }

    public static ImageIcon loadIcon(String path, int width, int height) {
        File iconFile = new File(path);
        if (!iconFile.exists()) {
            iconFile = new File("icons/missing.png");
        }

        if (iconFile.exists()) {
            ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
            Image image = icon.getImage();
            Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(newimg);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static JButton createButton(String iconPath, String toolTip, ActionListener actionListener) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(40, 40));
        button.setBackground(new Color(60, 63, 65));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setToolTipText(toolTip);
        button.addActionListener(actionListener);

        if (iconPath != null && !iconPath.isEmpty()) {
            ImageIcon icon = loadIcon(iconPath, 40, 40);
            if (icon != null) {
                button.setIcon(icon);
            }
        }

        return button;
    }
}
