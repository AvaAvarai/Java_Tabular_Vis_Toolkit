package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

public class MainMenu extends JFrame {

    public MainMenu() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit - Main Menu");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Banner graphic with padding
        JLabel bannerLabel = new JLabel(new ImageIcon("resources/graphics/banner.png"));
        bannerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(bannerLabel, BorderLayout.NORTH);

        // Create styled buttons
        JButton startAppButton = createStyledButton("Start Application");
        JButton githubButton = createStyledButton("GitHub");
        JButton aboutButton = createStyledButton("About the Project");
        JButton openDatasetsButton = createStyledButton("Open Datasets Folder");
        JButton openScreenshotsButton = createStyledButton("Open Screenshots Folder");
        JButton exitButton = createStyledButton("Exit");

        // Button panel with background color
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        buttonPanel.setBackground(Color.DARK_GRAY);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding around the buttons
        buttonPanel.add(startAppButton);
        buttonPanel.add(githubButton);
        buttonPanel.add(aboutButton);
        buttonPanel.add(openDatasetsButton);
        buttonPanel.add(openScreenshotsButton);
        buttonPanel.add(exitButton);
        add(buttonPanel, BorderLayout.CENTER);

        // Footer with styled text
        JLabel footerLabel = new JLabel("JTabViz by the CWU-VKD-LAB available for free under the MIT license, 2024.", JLabel.CENTER);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        footerLabel.setForeground(Color.LIGHT_GRAY);
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
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(45, 45, 45));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainMenu mainMenu = new MainMenu();
                mainMenu.setVisible(true);
            }
        });
    }
}
