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
    private static final Color FRAME_COLOR = new Color(100, 100, 100);
    private static final int CORNER_RADIUS = 15;
    private static final int FRAME_THICKNESS = 4;

    public MainMenu() {
        setTitle("JTabViz Main Menu");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Create gradient background panel with frame
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Draw gradient background
                GradientPaint gp = new GradientPaint(0, 0, BACKGROUND_COLOR_TOP, 
                                                    0, getHeight(), BACKGROUND_COLOR_BOTTOM);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout(0, 0));
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
        bannerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(bannerLabel, BorderLayout.NORTH);

        // Create modern styled buttons with aligned icons
        JButton startAppButton = createModernButton("     Start Application", UIHelper.loadIcon("/icons/start.png", 20, 20));
        JButton githubButton = createModernButton("     GitHub Repository", UIHelper.loadIcon("/icons/github.png", 20, 20));
        JButton aboutButton = createModernButton("     About JTabViz", UIHelper.loadIcon("/icons/about.png", 20, 20));
        JButton openDatasetsButton = createModernButton("     Datasets", UIHelper.loadIcon("/icons/folder.png", 20, 20));
        JButton openScreenshotsButton = createModernButton("     Screenshots", UIHelper.loadIcon("/icons/image.png", 20, 20));
        JButton exitButton = createModernButton("     Exit", UIHelper.loadIcon("/icons/exit.png", 20, 20));

        // Button panel with modern layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setOpaque(false);

        // Subtitle
        JLabel subTitleLabel = new JLabel("Java Tabular Visualization Toolkit", JLabel.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        subTitleLabel.setForeground(new Color(200, 200, 200));
        subTitleLabel.setBorder(BorderFactory.createEmptyBorder(5, 20, 15, 20)); // Reduced top padding

        centerPanel.add(subTitleLabel, BorderLayout.NORTH);

        // Button panel
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

        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

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

        // Draw frame on glass pane
        setGlassPane(new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(FRAME_COLOR);
                g2d.setStroke(new BasicStroke(FRAME_THICKNESS));
                g2d.drawRect(FRAME_THICKNESS/2, FRAME_THICKNESS/2, 
                            getWidth()-FRAME_THICKNESS, getHeight()-FRAME_THICKNESS);
            }
        });
        getGlassPane().setVisible(true);
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
            button.setHorizontalAlignment(SwingConstants.LEFT);
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
