package src;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class MainMenu extends JFrame {

    private static final Color BACKGROUND_COLOR_TOP = new Color(30, 30, 30);
    private static final Color BACKGROUND_COLOR_BOTTOM = new Color(50, 80, 120);
    private static final Color BUTTON_COLOR = new Color(70, 130, 180);
    private static final Color BUTTON_HOVER_COLOR = new Color(100, 149, 237);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final int CORNER_RADIUS = 15;

    public MainMenu() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 20));

        // Create gradient background panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, BACKGROUND_COLOR_TOP, 
                                                    0, getHeight(), BACKGROUND_COLOR_BOTTOM);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout(0, 20));
        setContentPane(backgroundPanel);

        // Banner
        ImageIcon bannerIcon = null;
        try {
            bannerIcon = new ImageIcon(MainMenu.class.getResource("/graphics/banner.png"));
        } catch (Exception e) {
            System.err.println("Could not load banner image");
        }
        JLabel bannerLabel = new JLabel(bannerIcon);
        bannerLabel.setPreferredSize(new Dimension(getWidth(), 200));
        bannerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        add(bannerLabel, BorderLayout.NORTH);

        // Create modern styled buttons
        JButton startAppButton = createModernButton("Start Application", loadIcon("/graphics/start.png"));
        JButton githubButton = createModernButton("GitHub Repository", loadIcon("/graphics/github.png"));
        JButton aboutButton = createModernButton("About JTabViz", loadIcon("/graphics/about.png"));
        JButton openDatasetsButton = createModernButton("Datasets", loadIcon("/graphics/folder.png"));
        JButton openScreenshotsButton = createModernButton("Screenshots", loadIcon("/graphics/image.png"));
        JButton exitButton = createModernButton("Exit", loadIcon("/graphics/exit.png"));

        // Button panel with modern layout
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 60, 20, 60));

        // Add buttons with spacing
        addButtonWithSpacing(buttonPanel, startAppButton);
        addButtonWithSpacing(buttonPanel, githubButton);
        addButtonWithSpacing(buttonPanel, aboutButton);
        addButtonWithSpacing(buttonPanel, openDatasetsButton);
        addButtonWithSpacing(buttonPanel, openScreenshotsButton);
        addButtonWithSpacing(buttonPanel, exitButton);

        add(buttonPanel, BorderLayout.CENTER);

        // Modern footer
        JLabel footerLabel = new JLabel("© 2024 CWU-VKD-LAB | MIT License", JLabel.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerLabel.setForeground(new Color(200, 200, 200));
        footerLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(footerLabel, BorderLayout.SOUTH);

        // Action listeners
        startAppButton.addActionListener(e -> openCsvViewer());
        githubButton.addActionListener(e -> openGitHub());
        aboutButton.addActionListener(e -> showAboutDialog());
        openDatasetsButton.addActionListener(e -> openFolder("datasets"));
        openScreenshotsButton.addActionListener(e -> openFolder("screenshots"));
        exitButton.addActionListener(e -> System.exit(0));
    }

    private ImageIcon loadIcon(String path) {
        try {
            return new ImageIcon(MainMenu.class.getResource(path));
        } catch (Exception e) {
            System.err.println("Could not load icon: " + path);
            return null;
        }
    }

    private JButton createModernButton(String text, ImageIcon icon) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                super.paintComponent(g2);
                g2.dispose();
            }
        };

        if (icon != null) {
            button.setIcon(icon);
            button.setIconTextGap(10);
        }

        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BUTTON_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(300, 45));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
            }
        });

        return button;
    }

    private void addButtonWithSpacing(JPanel panel, JButton button) {
        panel.add(Box.createVerticalStrut(10));
        panel.add(button);
    }

    private void openCsvViewer() {
        dispose();
        CsvViewer csvViewer = new CsvViewer(this);
        csvViewer.setVisible(true);
    }

    private void openGitHub() {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/CWU-VKD-LAB/jtabviz"));
        } catch (Exception ex) {
            showError("Failed to open GitHub URL.");
        }
    }

    private void showAboutDialog() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/README.md");
            File tempFile = File.createTempFile("README", ".md");
            tempFile.deleteOnExit();

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            Desktop.getDesktop().open(tempFile);

        } catch (Exception ex) {
            showError("Failed to open README.md");
        }
    }

    private void openFolder(String folderName) {
        try {
            Desktop.getDesktop().open(new File(folderName));
        } catch (Exception ex) {
            showError("Failed to open folder: " + folderName);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
