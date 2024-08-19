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

        // Increase resolution by scaling
        int scaleFactor = 2; // Adjust this for higher resolution screenshots
        BufferedImage image = new BufferedImage(fullSize.width * scaleFactor, fullSize.height * scaleFactor, BufferedImage.TYPE_INT_ARGB);

        // Create a Graphics2D object with enhanced rendering hints
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Scale the graphics object to render at a higher resolution
        g2d.scale(scaleFactor, scaleFactor);
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
