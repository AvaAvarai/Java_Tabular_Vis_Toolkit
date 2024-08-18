package src;

import javax.swing.*;
import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;

public class MainMenu extends JFrame {

    public MainMenu() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit - Main Menu");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        ImageIcon bannerIcon = new ImageIcon(getClass().getResource("/graphics/banner.png"));
        JLabel bannerLabel = new JLabel(bannerIcon);
        bannerLabel.setPreferredSize(new Dimension(getWidth(), 150));
        add(bannerLabel, BorderLayout.NORTH);

        // Create styled buttons
        JButton startAppButton = createStyledButton("Start Application");
        JButton githubButton = createStyledButton("Visit Project GitHub");
        JButton aboutButton = createStyledButton("About the Project");
        JButton openDatasetsButton = createStyledButton("Open Datasets Folder");
        JButton openScreenshotsButton = createStyledButton("Open Screenshots Folder");
        JButton exitButton = createStyledButton("Exit");

        // Button panel with darker background color
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        buttonPanel.setBackground(Color.DARK_GRAY); // Darker background color for the button panel
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        buttonPanel.add(startAppButton);
        buttonPanel.add(githubButton);
        buttonPanel.add(aboutButton);
        buttonPanel.add(openDatasetsButton);
        buttonPanel.add(openScreenshotsButton);
        buttonPanel.add(exitButton);
        add(buttonPanel, BorderLayout.CENTER);

        // Footer with darker text color
        JLabel footerLabel = new JLabel("JTabViz is developed by the CWU-VKD-LAB and is available for free under the MIT license as of August 2024.", JLabel.CENTER);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(30, 30, 30));
        footerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(footerLabel, BorderLayout.SOUTH);

        // Action listeners
        startAppButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCsvViewer();
            }
        });

        githubButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openGitHub();
            }
        });

        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });

        openDatasetsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFolder("datasets");
            }
        });

        openScreenshotsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFolder("screenshots");
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                // Custom painting for rounded corners
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // Rounded corners
                super.paintComponent(g);
            }

            @Override
            public void setContentAreaFilled(boolean b) {
                // Do not fill content area for custom painting
            }
        };

        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.BLACK);
        button.setBackground(new Color(200, 200, 200));
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setUI(new MetalButtonUI() {
            protected Color getDisabledTextColor() {
                return Color.BLACK;
            }

            protected void paintButtonPressed(Graphics g, AbstractButton b) {
                g.setColor(b.getBackground().darker());
                g.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 30, 30); // Rounded corners on press
            }
        });

        // Add mouse listener for hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(220, 220, 220)); // Lighter color on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(200, 200, 200)); // Original color when not hovering
            }
        });

        return button;
    }

    private void openCsvViewer() {
        // Close the main menu and open the CSV Viewer
        dispose();
        CsvViewer csvViewer = new CsvViewer(this);
        csvViewer.setVisible(true);
    }

    private void openGitHub() {
        try {
            Desktop.getDesktop().browse(new URI("https://github.com/CWU-VKD-LAB/jtabviz"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to open GitHub URL.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "JTabViz: A Java-based toolkit for visualizing and analyzing tabular machine learning data.", 
            "About the Project", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openFolder(String folderName) {
        try {
            Desktop.getDesktop().open(new File(folderName));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to open folder: " + folderName, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
