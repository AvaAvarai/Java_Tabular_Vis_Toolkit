package src;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import src.managers.ButtonPanelManager;
import src.table.ReorderableTableModel;
import src.table.TableMouseListener;

public class CsvViewerUIHelper {

    private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
    private static final Color ACCENT_COLOR = new Color(70, 130, 180);
    private static final Color TEXT_COLOR = new Color(51, 51, 51);

    public static JPanel createButtonPanel(CsvViewer viewer) {
        ButtonPanelManager buttonPanelManager = new ButtonPanelManager(viewer);
        return buttonPanelManager.createButtonPanel();
    }

    public static JScrollPane createStatsScrollPane(JTextArea statsTextArea) {
        statsTextArea.setBackground(Color.WHITE);
        statsTextArea.setForeground(TEXT_COLOR);
        statsTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statsTextArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(statsTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        scrollPane.setBackground(BACKGROUND_COLOR);
        return scrollPane;
    }

    public static JPanel createBottomPanel(JLabel selectedRowsLabel, JSlider thresholdSlider, JLabel thresholdLabel) {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel sliderPanel = new JPanel(new BorderLayout(10, 0));
        sliderPanel.setBackground(BACKGROUND_COLOR);
        
        // Style the labels
        JLabel thresholdTitle = new JLabel("Auto Pure Region Size Threshold:");
        styleLabel(thresholdTitle);
        styleLabel(thresholdLabel);
        styleLabel(selectedRowsLabel);
        
        // Style the slider
        thresholdSlider.setBackground(BACKGROUND_COLOR);
        thresholdSlider.setForeground(ACCENT_COLOR);
        thresholdSlider.setUI(new ModernSliderUI(thresholdSlider));
        
        sliderPanel.add(thresholdTitle, BorderLayout.WEST);
        sliderPanel.add(thresholdSlider, BorderLayout.CENTER);
        sliderPanel.add(thresholdLabel, BorderLayout.EAST);
        
        bottomPanel.add(sliderPanel, BorderLayout.EAST);
        bottomPanel.add(selectedRowsLabel, BorderLayout.CENTER);
        
        return bottomPanel;
    }

    public static JSplitPane createSplitPane(JScrollPane tableScrollPane, JPanel statsPanel) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8);
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_COLOR);
        
        // Style the table scroll pane
        tableScrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        
        // Style the stats panel
        statsPanel.setBackground(BACKGROUND_COLOR);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        return splitPane;
    }

    public static void setupTable(JTable table, ReorderableTableModel tableModel, CsvViewer viewer) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT_COLOR);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        // Style the header
        table.getTableHeader().setBackground(ACCENT_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        
        viewer.applyDefaultRenderer();
        
        // Add existing listeners
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
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
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
