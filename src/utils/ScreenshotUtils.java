package src.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    public static void captureAndSaveScreenshot(JScrollPane scrollPane, String plotName, String datasetName) {
        // Get the view component from the JScrollPane
        Component view = scrollPane.getViewport().getView();
        
        // Save the original size
        Dimension originalSize = view.getSize();
        
        // Resize the component to the full size
        Dimension fullSize = view.getPreferredSize();
        view.setSize(fullSize);
        
        // Create a BufferedImage that fits the entire content
        BufferedImage image = new BufferedImage(fullSize.width, fullSize.height, BufferedImage.TYPE_INT_ARGB);
        
        // Create a Graphics2D object to render the entire content
        Graphics2D g2d = image.createGraphics();
        view.paint(g2d);  // Manually paint the entire view component to the image
        g2d.dispose();
        
        // Reset the view component to its original size
        view.setSize(originalSize);
        
        // Generate a timestamp
        String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());

        // Ensure the screenshots/export directory exists
        File exportDir = new File("screenshots/export");
        if (!exportDir.exists()) {
            exportDir.mkdirs(); // Create directories if they do not exist
        }

        // Create the filename
        String filename = String.format("screenshots/export/%s_%s_%s.png", datasetName, plotName, timestamp);

        // Save the image to a file
        try {
            ImageIO.write(image, "png", new File(filename));
            JOptionPane.showMessageDialog(scrollPane, "Screenshot saved: " + filename);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(scrollPane, "Failed to save screenshot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
