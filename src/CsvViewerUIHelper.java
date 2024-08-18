package src;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import src.table.ReorderableTableModel;
import src.table.TableMouseListener;

public class CsvViewerUIHelper {

    public static JPanel createButtonPanel(CsvViewer viewer) {
        return ButtonPanel.createButtonPanel(viewer);
    }

    public static JScrollPane createStatsScrollPane(JTextArea statsTextArea) {
        return new JScrollPane(statsTextArea);
    }

    public static JPanel createBottomPanel(JLabel selectedRowsLabel, JSlider thresholdSlider, JLabel thresholdLabel) {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(thresholdSlider, BorderLayout.CENTER);
        sliderPanel.add(thresholdLabel, BorderLayout.EAST);
        bottomPanel.add(sliderPanel, BorderLayout.EAST);
        bottomPanel.add(selectedRowsLabel, BorderLayout.CENTER);
        return bottomPanel;
    }

    public static JSplitPane createSplitPane(JScrollPane tableScrollPane, JPanel statsPanel) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8);
        return splitPane;
    }

    public static void setupTable(JTable table, ReorderableTableModel tableModel, CsvViewer viewer) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        viewer.applyDefaultRenderer();

        table.getSelectionModel().addListSelectionListener(e -> viewer.updateSelectedRowsLabel());

        table.addMouseListener(new TableMouseListener(viewer));
        table.getTableHeader().addMouseListener(new TableMouseListener(viewer));

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
}
