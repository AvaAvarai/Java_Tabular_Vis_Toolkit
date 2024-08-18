package src;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Map;

public class RendererManager {

    private final CsvViewer csvViewer;
    private double[] minValues;
    private double[] maxValues;
    private boolean[] isNumerical;

    public RendererManager(CsvViewer csvViewer) {
        this.csvViewer = csvViewer;
    }

    public void applyDefaultRenderer() {
        csvViewer.table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Color.decode("#C0C0C0"));
    
                if (hasFocus) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder());
                    }
                }
                
                c.setForeground(csvViewer.getCellTextColor());
                return c;
            }
        });
        csvViewer.table.repaint();
    }

    public void applyCombinedRenderer() {
        int numColumns = csvViewer.tableModel.getColumnCount();
        
        minValues = new double[numColumns];
        maxValues = new double[numColumns];
        isNumerical = new boolean[numColumns];
        int classColumnIndex = csvViewer.getClassColumnIndex();
    
        for (int i = 0; i < numColumns; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
            isNumerical[i] = true;
        }
    
        for (int row = 0; row < csvViewer.tableModel.getRowCount(); row++) {
            for (int col = 0; col < numColumns; col++) {
                try {
                    double value = Double.parseDouble(csvViewer.tableModel.getValueAt(row, col).toString());
                    if (value < minValues[col]) minValues[col] = value;
                    if (value > maxValues[col]) maxValues[col] = value;
                } catch (NumberFormatException e) {
                    isNumerical[col] = false;
                }
            }
        }
    
        csvViewer.table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelColumn = table.convertColumnIndexToModel(column);
    
                if (modelColumn >= 0 && modelColumn < numColumns) {
                    if (csvViewer.isClassColorEnabled() && modelColumn == classColumnIndex) {
                        String className = (String) value;
                        if (csvViewer.getClassColors().containsKey(className)) {
                            c.setBackground(csvViewer.getClassColors().get(className));
                        } else {
                            c.setBackground(Color.decode("#C0C0C0"));
                        }
                    } else if (csvViewer.isHeatmapEnabled() && value != null && !value.toString().trim().isEmpty() && isNumerical[modelColumn]) {
                        try {
                            double val = Double.parseDouble(value.toString());
                            double normalizedValue = (val - minValues[modelColumn]) / (maxValues[modelColumn] - minValues[modelColumn]);
                            Color color = getColorForValue(normalizedValue);
                            c.setBackground(color);
                        } catch (NumberFormatException e) {
                            c.setBackground(Color.decode("#C0C0C0"));
                        }
                    } else {
                        c.setBackground(Color.decode("#C0C0C0"));
                    }
                } else {
                    c.setBackground(Color.decode("#C0C0C0"));
                }
    
                if (hasFocus) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    }
                } else {
                    if (c instanceof JComponent) {
                        ((JComponent) c).setBorder(BorderFactory.createEmptyBorder());
                    }
                }
    
                c.setForeground(csvViewer.getCellTextColor());
                return c;
            }
    
            private Color getColorForValue(double value) {
                int red = (int) (255 * value);
                int blue = 255 - red;
                red = Math.max(0, Math.min(255, red));
                blue = Math.max(0, Math.min(255, blue));
                return new Color(red, 0, blue);
            }
        });
        csvViewer.table.repaint();
    }
}
