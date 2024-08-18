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

        // TODO: Banner graphic
        //JLabel bannerLabel = new JLabel(new ImageIcon("path/to/banner/image.png"));
        //add(bannerLabel, BorderLayout.NORTH);

        // Create buttons
        JButton startAppButton = new JButton("Start Application");
        JButton githubButton = new JButton("GitHub");
        JButton aboutButton = new JButton("About the Project");
        JButton openDatasetsButton = new JButton("Open Datasets Folder");
        JButton openScreenshotsButton = new JButton("Open Screenshots Folder");

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        buttonPanel.add(startAppButton);
        buttonPanel.add(githubButton);
        buttonPanel.add(aboutButton);
        buttonPanel.add(openDatasetsButton);
        buttonPanel.add(openScreenshotsButton);
        add(buttonPanel, BorderLayout.CENTER);

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
    }

    private void openCsvViewer() {
        // Close the main menu and open the CSV Viewer
        dispose();
        CsvViewer csvViewer = new CsvViewer();
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
