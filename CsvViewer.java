import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class CsvViewer extends JFrame {
    private JTable table;
    private ReorderableTableModel tableModel;
    private CsvDataHandler dataHandler;
    private boolean isNormalized = false;
    private boolean isHeatmapEnabled = false;
    private Color cellTextColor = Color.BLACK; // Default cell text color
    private JTextArea statsTextArea;
    private JButton toggleButton;

    public CsvViewer() {
        setTitle("CSV Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dataHandler = new CsvDataHandler();
        tableModel = new ReorderableTableModel();
        table = new JTable(tableModel);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setFillsViewportHeight(true);
        table.setTransferHandler(new TableRowTransferHandler(table));
        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.NORTH);

        statsTextArea = new JTextArea(3, 0); // Small height
        statsTextArea.setEditable(false);
        statsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statsScrollPane = new JScrollPane(statsTextArea);
        add(statsScrollPane, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton loadButton = createButton("L", "Load CSV");
        toggleButton = createButton("N", "Normalize");
        JButton highlightBlanksButton = createButton("H", "Highlight Blanks");
        JButton heatmapButton = createButton("M", "Show Heatmap");
        JButton fontColorButton = createButton("F", "Font Color");
        JButton insertRowButton = createButton("+", "Insert Row");
        JButton deleteRowButton = createButton("-", "Delete Row");
        JButton exportButton = createButton("E", "Export CSV");
        JButton parallelPlotButton = createButton("P", "Parallel Coordinates");

        loadButton.addActionListener(e -> loadCsvFile());
        toggleButton.addActionListener(e -> toggleDataView());
        highlightBlanksButton.addActionListener(e -> highlightBlanks());
        heatmapButton.addActionListener(e -> toggleHeatmap(heatmapButton));
        fontColorButton.addActionListener(e -> chooseFontColor());
        insertRowButton.addActionListener(e -> insertRow());
        deleteRowButton.addActionListener(e -> deleteRow());
        exportButton.addActionListener(e -> exportCsvFile());
        parallelPlotButton.addActionListener(e -> showParallelCoordinatesPlot());

        buttonPanel.add(loadButton);
        buttonPanel.add(toggleButton);
        buttonPanel.add(highlightBlanksButton);
        buttonPanel.add(heatmapButton);
        buttonPanel.add(fontColorButton);
        buttonPanel.add(insertRowButton);
        buttonPanel.add(deleteRowButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(parallelPlotButton);

        return buttonPanel;
    }

    private JButton createButton(String text, String toolTip) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(50, 50)); // Set preferred size to make the button square
        button.setToolTipText(toolTip); // Set tool tip for hover text
        return button;
    }

    private void loadCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("datasets"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            dataHandler.loadCsvData(filePath, tableModel, statsTextArea);
            isNormalized = false;
            isHeatmapEnabled = false; // Reset heatmap state when new CSV is loaded
            updateTableData(dataHandler.getOriginalData());
        }
    }

    private void toggleDataView() {
        if (dataHandler.isDataEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to toggle", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isNormalized) {
            updateTableData(dataHandler.getOriginalData());
            isNormalized = false;
            toggleButton.setText("N");
            toggleButton.setToolTipText("Normalize");
        } else {
            dataHandler.normalizeData();
            updateTableData(dataHandler.getNormalizedData());
            isNormalized = true;
            toggleButton.setText("D");
            toggleButton.setToolTipText("Default");
        }
    }

    private void updateTableData(List<String[]> data) {
        tableModel.setRowCount(0); // Clear existing data
        for (String[] row : data) {
            tableModel.addRow(row);
        }
        if (isHeatmapEnabled) {
            applyHeatmap();
        } else {
            applyDefaultRenderer();
        }
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    private void highlightBlanks() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value == null || value.toString().trim().isEmpty()) {
                    c.setBackground(Color.YELLOW);
                } else {
                    c.setBackground(Color.WHITE);
                }
                c.setForeground(cellTextColor);
                return c;
            }
        });
        table.repaint();
    }

    private void toggleHeatmap(JButton heatmapButton) {
        isHeatmapEnabled = !isHeatmapEnabled;
        if (isHeatmapEnabled) {
            applyHeatmap();
            heatmapButton.setText("H");
            heatmapButton.setToolTipText("Hide Heatmap");
        } else {
            applyDefaultRenderer();
            heatmapButton.setText("M");
            heatmapButton.setToolTipText("Show Heatmap");
        }
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    private void applyHeatmap() {
        int numColumns = tableModel.getColumnCount();
        double[] minValues = new double[numColumns];
        double[] maxValues = new double[numColumns];
        boolean[] isNumerical = new boolean[numColumns];

        // Initialize min and max values
        for (int i = 0; i < numColumns; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
            isNumerical[i] = true;
        }

        // Calculate min and max for each column
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 0; col < numColumns; col++) {
                try {
                    double value = Double.parseDouble(tableModel.getValueAt(row, col).toString());
                    if (value < minValues[col]) minValues[col] = value;
                    if (value > maxValues[col]) maxValues[col] = value;
                } catch (NumberFormatException e) {
                    isNumerical[col] = false;
                }
            }
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelColumn = table.convertColumnIndexToModel(column); // Get the model index of the column
                if (value != null && !value.toString().trim().isEmpty() && isNumerical[modelColumn]) {
                    try {
                        double val = Double.parseDouble(value.toString());
                        double normalizedValue = (val - minValues[modelColumn]) / (maxValues[modelColumn] - minValues[modelColumn]);
                        Color color = getColorForValue(normalizedValue);
                        c.setBackground(color);
                    } catch (NumberFormatException e) {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
                c.setForeground(cellTextColor);
                return c;
            }

            private Color getColorForValue(double value) {
                // Use a color gradient from blue to red, value is expected to be between 0 and 1
                int red = (int) (255 * value);
                int blue = 255 - red;
                red = Math.max(0, Math.min(255, red));
                blue = Math.max(0, Math.min(255, blue));
                return new Color(red, 0, blue);
            }
        });
        table.repaint();
    }

    private void applyDefaultRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Color.WHITE);
                c.setForeground(cellTextColor);
                return c;
            }
        });
        table.repaint();
    }

    private void chooseFontColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Font Color", cellTextColor);
        if (newColor != null) {
            cellTextColor = newColor;
            if (isHeatmapEnabled) {
                applyHeatmap();
            } else {
                applyDefaultRenderer();
            }
            dataHandler.updateStats(tableModel, statsTextArea);
        }
    }

    private void insertRow() {
        int numColumns = tableModel.getColumnCount();
        String[] emptyRow = new String[numColumns];
        tableModel.addRow(emptyRow);
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    private void deleteRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.removeRow(selectedRow);
            dataHandler.updateStats(tableModel, statsTextArea);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("datasets"));
        fileChooser.setDialogTitle("Save CSV File");
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            dataHandler.saveCsvData(filePath, tableModel);
        }
    }

    private void showParallelCoordinatesPlot() {
        String[] columnNames = new String[tableModel.getColumnCount()];
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            columnNames[i] = tableModel.getColumnName(i);
        }

        List<String[]> data = dataHandler.isDataEmpty() ? dataHandler.getOriginalData() : (isNormalized ? dataHandler.getNormalizedData() : dataHandler.getOriginalData());
        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, tableModel.getColumnCount() - 1);
        plot.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CsvViewer viewer = new CsvViewer();
            viewer.setVisible(true);
        });
    }
}
