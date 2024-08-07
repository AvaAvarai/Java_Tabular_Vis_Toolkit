package src;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CsvViewer extends JFrame {
    private JTable table;
    private ReorderableTableModel tableModel;
    private CsvDataHandler dataHandler;
    private boolean isNormalized = false;
    private boolean isHeatmapEnabled = false;
    private boolean isClassColorEnabled = false; // New flag for class column coloring
    private Color cellTextColor = Color.BLACK; // Default cell text color
    private JTextArea statsTextArea;
    private JButton toggleButton;
    private Map<String, Color> classColors = new HashMap<>(); // Store class colors

    public CsvViewer() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit");
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

        JScrollPane tableScrollPane = new JScrollPane(table);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsScrollPane);
        splitPane.setResizeWeight(0.8); // 80% of space for table, 20% for stats initially
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton loadButton = createButton("icons/load.png", "Load CSV");
        toggleButton = createButton("icons/normalize.png", "Normalize");
        JButton highlightBlanksButton = createButton("icons/highlight.png", "Highlight Blanks");
        JButton heatmapButton = createButton("icons/heatmap.png", "Show Heatmap");
        JButton fontColorButton = createButton("icons/fontcolor.png", "Font Color");
        JButton insertRowButton = createButton("icons/insert.png", "Insert Row");
        JButton deleteRowButton = createButton("icons/delete.png", "Delete Row");
        JButton exportButton = createButton("icons/export.png", "Export CSV");
        JButton parallelPlotButton = createButton("icons/parallel.png", "Parallel Coordinates");
        JButton classColorButton = createButton("icons/classcolor.png", "Toggle Class Colors");
        JButton setClassColorsButton = createButton("icons/setcolor.png", "Set Class Colors"); // New button for setting class colors
        JButton ruleTesterButton = createButton("icons/rule_tester.png", "Rule Tester"); // New button for rule tester

        loadButton.addActionListener(e -> loadCsvFile());
        toggleButton.addActionListener(e -> toggleDataView());
        highlightBlanksButton.addActionListener(e -> highlightBlanks());
        heatmapButton.addActionListener(e -> toggleHeatmap());
        fontColorButton.addActionListener(e -> chooseFontColor());
        insertRowButton.addActionListener(e -> insertRow());
        deleteRowButton.addActionListener(e -> deleteRow());
        exportButton.addActionListener(e -> exportCsvFile());
        parallelPlotButton.addActionListener(e -> showParallelCoordinatesPlot());
        classColorButton.addActionListener(e -> toggleClassColors()); // New action listener for class coloring
        setClassColorsButton.addActionListener(e -> showColorPickerDialog()); // New action listener for setting class colors
        ruleTesterButton.addActionListener(e -> showRuleTesterDialog()); // New action listener for rule tester

        buttonPanel.add(loadButton);
        buttonPanel.add(toggleButton);
        buttonPanel.add(highlightBlanksButton);
        buttonPanel.add(heatmapButton);
        buttonPanel.add(fontColorButton);
        buttonPanel.add(insertRowButton);
        buttonPanel.add(deleteRowButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(parallelPlotButton);
        buttonPanel.add(classColorButton); // Add new button to panel
        buttonPanel.add(setClassColorsButton); // Add set class colors button to panel
        buttonPanel.add(ruleTesterButton); // Add rule tester button to panel

        return buttonPanel;
    }

    private JButton createButton(String iconPath, String toolTip) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(40, 40)); // Set preferred size
        button.setBackground(new Color(60, 63, 65)); // Set background color
        button.setFocusPainted(false); // Remove focus border
        button.setBorderPainted(false); // Remove border
        button.setContentAreaFilled(false); // Remove background
        button.setToolTipText(toolTip); // Set tool tip for hover text

        if (iconPath != null && !iconPath.isEmpty()) {
            ImageIcon icon = loadIcon(iconPath, 40, 40); // Load 40x40 pixel icon to fit button
            if (icon != null) {
                button.setIcon(icon);
            }
        }

        return button;
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        File iconFile = new File(path);
        if (!iconFile.exists()) {
            iconFile = new File("icons/missing.png"); // Load missing icon if file doesn't exist
        }

        if (iconFile.exists()) {
            ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
            Image image = icon.getImage(); // Transform it
            Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // Scale it
            return new ImageIcon(newimg); // Transform it back
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
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
            isClassColorEnabled = false; // Reset class color state when new CSV is loaded
            updateTableData(dataHandler.getOriginalData());
            generateClassColors(); // Generate class colors based on the loaded data
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
            toggleButton.setIcon(loadIcon("icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");
        } else {
            dataHandler.normalizeData();
            updateTableData(dataHandler.getNormalizedData());
            isNormalized = true;
            toggleButton.setIcon(loadIcon("icons/default.png", 40, 40));
            toggleButton.setToolTipText("Default");
        }
    }

    private void updateTableData(List<String[]> data) {
        tableModel.setRowCount(0); // Clear existing data
        for (String[] row : data) {
            tableModel.addRow(row);
        }
        if (isHeatmapEnabled || isClassColorEnabled) {
            applyCombinedRenderer();
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

    private void toggleHeatmap() {
        isHeatmapEnabled = !isHeatmapEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    private void generateClassColors() {
        int classColumnIndex = tableModel.getColumnCount() - 1; // Assuming class column is the last one
        Map<String, Integer> classMap = new HashMap<>();
        int colorIndex = 0;

        // Assign colors to each class
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String className = (String) tableModel.getValueAt(row, classColumnIndex);
            if (!classMap.containsKey(className)) {
                classMap.put(className, colorIndex++);
            }
        }

        // Create distinct colors for each class
        for (Map.Entry<String, Integer> entry : classMap.entrySet()) {
            int value = entry.getValue();
            Color color = new Color(Color.HSBtoRGB(value / (float) classMap.size(), 1.0f, 1.0f));
            classColors.put(entry.getKey(), color);
        }
    }

    private void applyCombinedRenderer() {
        int numColumns = tableModel.getColumnCount();
        double[] minValues = new double[numColumns];
        double[] maxValues = new double[numColumns];
        boolean[] isNumerical = new boolean[numColumns];
        int classColumnIndex = tableModel.getColumnCount() - 1; // Assuming class column is the last one

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

                // Apply class colors
                if (isClassColorEnabled && column == classColumnIndex) {
                    String className = (String) value;
                    if (classColors.containsKey(className)) {
                        c.setBackground(classColors.get(className));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else if (isHeatmapEnabled && value != null && !value.toString().trim().isEmpty() && isNumerical[modelColumn]) {
                    // Apply heatmap colors
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

    private void toggleClassColors() {
        isClassColorEnabled = !isClassColorEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);
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
            if (isHeatmapEnabled || isClassColorEnabled) {
                applyCombinedRenderer();
            } else {
                applyDefaultRenderer();
            }
            dataHandler.updateStats(tableModel, statsTextArea);
        }
    }

    private void showColorPickerDialog() {
        int classColumnIndex = tableModel.getColumnCount() - 1; // Assuming class column is the last one
        Set<String> uniqueClassNames = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            uniqueClassNames.add((String) tableModel.getValueAt(i, classColumnIndex));
        }
        String[] classNames = uniqueClassNames.toArray(new String[0]);
        JComboBox<String> classComboBox = new JComboBox<>(classNames);

        int result = JOptionPane.showConfirmDialog(this, classComboBox, "Select Class", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String selectedClass = (String) classComboBox.getSelectedItem();
            Color color = JColorChooser.showDialog(this, "Choose color for " + selectedClass, classColors.getOrDefault(selectedClass, Color.WHITE));
            if (color != null) {
                classColors.put(selectedClass, color);
                if (isClassColorEnabled) {
                    applyCombinedRenderer();
                }
            }
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
        // Get the reordered column names
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        String[] columnNames = new String[columnCount];
        int[] columnOrder = new int[columnCount]; 
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = columnModel.getColumn(i).getHeaderValue().toString();
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        // Get the data
        List<String[]> data = dataHandler.isDataEmpty() ? dataHandler.getOriginalData() : (isNormalized ? dataHandler.getNormalizedData() : dataHandler.getOriginalData());
    
        // Create and show the plot
        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, classColors, tableModel.getColumnCount() - 1, columnOrder);
        plot.setVisible(true);
    }    

    private void showRuleTesterDialog() {
        RuleTesterDialog ruleTesterDialog = new RuleTesterDialog(this, tableModel);
        ruleTesterDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CsvViewer viewer = new CsvViewer();
            viewer.setVisible(true);
        });
    }
}
