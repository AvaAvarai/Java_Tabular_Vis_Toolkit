package src;

import javax.swing.*;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import src.managers.ButtonPanelManager;
import src.table.ReorderableTableModel;
import src.table.TableMouseListener;

public class CsvViewerUIHelper {

    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 247); // Light gray background
    private static final Color ACCENT_COLOR = new Color(70, 130, 180);  // Steel blue accent
    private static final Color TEXT_COLOR = new Color(33, 33, 33);  // Dark text
    private static final Color HEADER_COLOR = new Color(70, 130, 180); // Steel blue headers
    private static final Color GRID_COLOR = new Color(230, 230, 230); // Light grid lines
    private static final Color SELECTED_COLOR = new Color(179, 215, 243); // Light blue selection

    // Font settings
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font CELL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font STATS_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    public static JPanel createButtonPanel(CsvViewer viewer) {
        ButtonPanelManager buttonPanelManager = new ButtonPanelManager(viewer);
        return buttonPanelManager.createButtonPanel();
    }

    public static JScrollPane createStatsScrollPane(JTextArea statsTextArea) {
        statsTextArea.setBackground(Color.WHITE);
        statsTextArea.setForeground(TEXT_COLOR);
        statsTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statsTextArea.setFont(STATS_FONT);
        
        JScrollPane scrollPane = new JScrollPane(statsTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 1));
        scrollPane.setBackground(BACKGROUND_COLOR);
        return scrollPane;
    }

    public static JPanel createBottomPanel(JLabel selectedRowsLabel, JSlider thresholdSlider, JLabel thresholdLabel) {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel sliderPanel = new JPanel(new BorderLayout(10, 0));
        sliderPanel.setBackground(BACKGROUND_COLOR);
        
        // Style the labels
        styleLabel(thresholdLabel);
        styleLabel(selectedRowsLabel);
        
        // Style the slider
        thresholdSlider.setBackground(BACKGROUND_COLOR);
        thresholdSlider.setForeground(ACCENT_COLOR);
        thresholdSlider.setUI(new ModernSliderUI(thresholdSlider));
        
        sliderPanel.add(new JLabel("Auto Pure Region Size Threshold:"), BorderLayout.WEST);
        sliderPanel.add(thresholdSlider, BorderLayout.CENTER);
        sliderPanel.add(thresholdLabel, BorderLayout.EAST);
        
        bottomPanel.add(sliderPanel, BorderLayout.EAST);
        bottomPanel.add(selectedRowsLabel, BorderLayout.WEST);
        
        return bottomPanel;
    }

    public static JSplitPane createSplitPane(JScrollPane tableScrollPane, JPanel statsPanel) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_COLOR);
        
        // Style the table scroll pane
        tableScrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 1));
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        
        // Style the stats panel
        statsPanel.setBackground(BACKGROUND_COLOR);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        return splitPane;
    }

    public static void setupTable(JTable table, ReorderableTableModel tableModel, CsvViewer viewer) {
        // Update existing table setup with modern styling
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setGridColor(GRID_COLOR);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT_COLOR);
        table.setFont(CELL_FONT);
        table.setRowHeight(25); // Increase row height for better readability
        
        // Style the header
        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(HEADER_FONT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT_COLOR));
        
        // Selection colors
        table.setSelectionBackground(SELECTED_COLOR);
        table.setSelectionForeground(TEXT_COLOR);

        // Add existing listeners and functionality
        table.getSelectionModel().addListSelectionListener(e -> viewer.updateSelectedRowsLabel());
        table.addMouseListener(new TableMouseListener(viewer));
        table.getTableHeader().addMouseListener(new TableMouseListener(viewer));
        
        // Add existing key listener
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    if (table.getSelectedRows().length == 1)
                        viewer.copySelectedCell();
                    else {
                        StringBuilder sb = new StringBuilder();
                        for (int row : table.getSelectedRows()) {
                            for (int col : table.getSelectedColumns()) {
                                sb.append(table.getValueAt(row, col)).append("\t");
                            }
                            sb.append("\n");
                        }
                        StringSelection stringSelection = new StringSelection(sb.toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                    }
                }
            }
        });
    }

    private static void styleLabel(JLabel label) {
        label.setFont(CELL_FONT);
        label.setForeground(TEXT_COLOR);
    }
}

// Add this custom UI class for the slider
class ModernSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
    private static final Color THUMB_COLOR = new Color(70, 130, 180);
    private static final Color TRACK_COLOR = new Color(200, 200, 200);
    
    public ModernSliderUI(JSlider slider) {
        super(slider);
    }
    
    @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(THUMB_COLOR);
            g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        }
    
    @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(TRACK_COLOR);
            g2d.fillRoundRect(trackRect.x, trackRect.y + (trackRect.height / 2) - 2,
                             trackRect.width, 4, 4, 4);
        }
}
