package src;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;

public class UIHelper {

    public static JTextArea createTextArea(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return textArea;
    }

    public static ImageIcon loadIcon(String path, int width, int height) {
        try (InputStream stream = UIHelper.class.getResourceAsStream(path)) {
            if (stream != null) {
                Image image = ImageIO.read(stream).getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(image);
            } else {
                System.err.println("Couldn't find file: " + path);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    
        ImageIcon icon = null;
        if (iconPath != null && !iconPath.isEmpty()) {
            icon = loadIcon(iconPath, 40, 40);
        }
    
        if (icon == null) {
            // Fallback to "missing.png" if the desired icon is not found
            icon = loadIcon("/icons/missing.png", 40, 40);
        }
    
        if (icon != null) {
            button.setIcon(icon);
        }
    
        return button;
    }    
}
