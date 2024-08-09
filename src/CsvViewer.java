package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

public class CsvViewer extends JFrame {
    public JTable table;
    public ReorderableTableModel tableModel;
    public CsvDataHandler dataHandler;
    public boolean isNormalized = false;
    public boolean isHeatmapEnabled = false;
    public boolean isClassColorEnabled = false; // Flag for class column coloring
    public Color cellTextColor = Color.BLACK; // Default cell text color
    public JTextArea statsTextArea;
    public JButton toggleButton;
    public JButton toggleStatsButton;
    public JButton toggleStatsOnButton;
    public JButton toggleStatsOffButton;
    public Map<String, Color> classColors = new HashMap<>(); // Store class colors
    public JLabel selectedRowsLabel; // Label to display the number of selected rows
    public JPanel bottomPanel; // Panel for the selected rows label
    public JPanel statsPanel; // Panel for stats visibility toggling
    public JScrollPane statsScrollPane; // Scroll pane for stats text area
    public JScrollPane tableScrollPane; // Scroll pane for the table
    public JSplitPane splitPane; // Split pane for table and stats

    public CsvViewer() {
        setTitle("JTabViz: Java Tabular Visualization Toolkit");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dataHandler = new CsvDataHandler();
        tableModel = new ReorderableTableModel();
        table = TableSetup.createTable(tableModel);
        table.addMouseListener(new TableMouseListener(this));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Add a KeyAdapter to handle Ctrl+C for copying cell content
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    copySelectedCell();
                }
            }
        });

        JPanel buttonPanel = ButtonPanel.createButtonPanel(this);
        add(buttonPanel, BorderLayout.NORTH);

        statsTextArea = UIHelper.createTextArea(3, 0);
        statsScrollPane = new JScrollPane(statsTextArea);

        tableScrollPane = new JScrollPane(table);

        selectedRowsLabel = new JLabel("Selected rows: 0");
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(selectedRowsLabel, BorderLayout.CENTER);

        statsPanel = new JPanel(new BorderLayout());
        statsPanel.add(statsScrollPane, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, statsPanel);
        splitPane.setResizeWeight(0.8); // 80% of space for table, 20% for stats initially
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH); // Always visible at the bottom
    }

    private void copySelectedCell() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row != -1 && col != -1) {
            Object cellValue = table.getValueAt(row, col);
            StringSelection stringSelection = new StringSelection(cellValue != null ? cellValue.toString() : "");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        }
    }

    public void toggleStatsVisibility(boolean hideStats) {
        if (hideStats) {
            // Remove the stats panel
            splitPane.setBottomComponent(null);
            splitPane.setDividerSize(0); // Remove the divider when stats are hidden
            switchToggleStatsButton(toggleStatsOffButton);
        } else {
            // Re-add the stats panel
            splitPane.setBottomComponent(statsPanel);
            splitPane.setDividerSize(10); // Restore the divider size
            splitPane.setDividerLocation(0.8); // Adjust the split pane layout after toggling
            switchToggleStatsButton(toggleStatsOnButton);
        }
    }

    private void switchToggleStatsButton(JButton newButton) {
        // Replace the current toggle stats button with the new one
        JPanel buttonPanel = (JPanel) toggleStatsButton.getParent();
        buttonPanel.remove(toggleStatsButton);
        toggleStatsButton = newButton;
        buttonPanel.add(toggleStatsButton);
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public void noDataLoadedError() {
        JOptionPane.showMessageDialog(this, "No data loaded", "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void loadCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("datasets"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            tableModel.setRowCount(0); // Clear existing table rows
            tableModel.setColumnCount(0); // Clear existing table columns
            classColors.clear(); // Clear existing class colors
            dataHandler.clearData(); // Clear existing data in data handler
            dataHandler.loadCsvData(filePath, tableModel, statsTextArea);
            isNormalized = false;
            isHeatmapEnabled = false; // Reset heatmap state when new CSV is loaded
            isClassColorEnabled = false; // Reset class color state when new CSV is loaded
            updateTableData(dataHandler.getOriginalData());
            generateClassColors(); // Generate class colors based on the loaded data
            updateSelectedRowsLabel(); // Reset the selected rows label

            // Scroll the stats window to the top on initial load
            statsTextArea.setCaretPosition(0);
        }
    }

    public void toggleDataView() {
        // Capture the current caret position
        int currentCaretPosition = statsTextArea.getCaretPosition();

        if (isNormalized) {
            updateTableData(dataHandler.getOriginalData());
            isNormalized = false;
            toggleButton.setIcon(UIHelper.loadIcon("icons/normalize.png", 40, 40));
            toggleButton.setToolTipText("Normalize");
        } else {
            dataHandler.normalizeData();
            updateTableData(dataHandler.getNormalizedData());
            isNormalized = true;
            toggleButton.setIcon(UIHelper.loadIcon("icons/denormalize.png", 40, 40));
            toggleButton.setToolTipText("Default");
        }

        // Restore the caret position to where it was before the data was updated
        statsTextArea.setCaretPosition(currentCaretPosition);
    }

    public void updateTableData(java.util.List<String[]> data) {
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
        updateSelectedRowsLabel(); // Update the selected rows label
    }

    public void highlightBlanks() {
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

    public void toggleHeatmap() {
        // Capture the current caret position
        int currentCaretPosition = statsTextArea.getCaretPosition();

        // Toggle the heatmap state and update the table renderer
        isHeatmapEnabled = !isHeatmapEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);

        // Restore the caret position to where it was before the data was updated
        statsTextArea.setCaretPosition(currentCaretPosition);
    }

    public void generateClassColors() {
        int classColumnIndex = getClassColumnIndex(); // Find the class column index
        if (classColumnIndex == -1) {
            return; // If no class column is found, return early
        }
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

    public int getClassColumnIndex() {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equalsIgnoreCase("class")) {
                return i;
            }
        }
        return -1; // Return -1 if no class column is found
    }

    public void applyCombinedRenderer() {
        int numColumns = tableModel.getColumnCount();
        double[] minValues = new double[numColumns];
        double[] maxValues = new double[numColumns];
        boolean[] isNumerical = new boolean[numColumns];
        int classColumnIndex = getClassColumnIndex(); // Find the class column index

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
                if (isClassColorEnabled && modelColumn == classColumnIndex) {
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

    public void toggleClassColors() {
        isClassColorEnabled = !isClassColorEnabled;
        applyCombinedRenderer();
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    public void applyDefaultRenderer() {
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

    public void showFontSettingsDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        // Font Color Picker
        JLabel colorLabel = new JLabel("Font Color:");
        panel.add(colorLabel);
        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Font Color", cellTextColor);
            if (newColor != null) {
                cellTextColor = newColor;
            }
        });
        panel.add(colorButton);

        // Font Size Selector
        JLabel sizeLabel = new JLabel("Font Size:");
        panel.add(sizeLabel);
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 72, 1)); // Default size: 12
        sizeSpinner.setValue(statsTextArea.getFont().getSize());
        panel.add(sizeSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel, "Font Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            int fontSize = (int) sizeSpinner.getValue();
            Font newFont = statsTextArea.getFont().deriveFont((float) fontSize);
            statsTextArea.setFont(newFont);

            // Apply to other components if needed
            table.setFont(newFont);
            table.getTableHeader().setFont(newFont);

            if (isHeatmapEnabled || isClassColorEnabled) {
                applyCombinedRenderer();
            } else {
                applyDefaultRenderer();
            }

            dataHandler.updateStats(tableModel, statsTextArea);
        }
    }

    public void showColorPickerDialog() {
        int classColumnIndex = getClassColumnIndex(); // Find the class column index
        if (classColumnIndex == -1) {
            return; // If no class column is found, return early
        }

        Set<String> uniqueClassNames = new HashSet<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            uniqueClassNames.add((String) tableModel.getValueAt(i, classColumnIndex));
        }
        String[] classNames = uniqueClassNames.toArray(new String[0]);

        // Create the main panel for the dialog
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create and add the JComboBox for class selection
        JComboBox<String> classComboBox = new JComboBox<>(classNames);

        // Custom renderer to show color swatches with spacing
        classComboBox.setRenderer(new ListCellRenderer<String>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout());
                JLabel label = new JLabel(value);
                JLabel colorSwatch = new JLabel();
                colorSwatch.setOpaque(true);
                colorSwatch.setPreferredSize(new Dimension(20, 20));
                colorSwatch.setBackground(classColors.getOrDefault(value, Color.WHITE));

                panel.add(colorSwatch, BorderLayout.WEST);
                panel.add(Box.createRigidArea(new Dimension(5, 0)), BorderLayout.CENTER); // Add space between swatch and label
                panel.add(label, BorderLayout.EAST);

                if (isSelected) {
                    panel.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    panel.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                return panel;
            }
        });

        mainPanel.add(classComboBox, BorderLayout.NORTH);

        // Create the legend panel
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Current Class Colors"));

        // Add each class color and label to the legend
        for (String className : classNames) {
            JPanel colorLabelPanel = new JPanel();
            colorLabelPanel.setLayout(new BorderLayout());

            // Create a small colored box
            JLabel colorBox = new JLabel();
            colorBox.setOpaque(true);
            colorBox.setBackground(classColors.getOrDefault(className, Color.WHITE));
            colorBox.setPreferredSize(new Dimension(20, 20));

            // Add the class label
            JLabel label = new JLabel(className);
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10)); // Add some space between color box and label

            colorLabelPanel.add(colorBox, BorderLayout.WEST);
            colorLabelPanel.add(label, BorderLayout.CENTER);

            legendPanel.add(colorLabelPanel);
        }

        mainPanel.add(legendPanel, BorderLayout.CENTER);

        // Loop to allow continuous color setting
        while (true) {
            int result = JOptionPane.showConfirmDialog(this, mainPanel, "Select Class", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                break; // Exit the loop if the user cancels
            }

            String selectedClass = (String) classComboBox.getSelectedItem();
            Color color = JColorChooser.showDialog(this, "Choose color for " + selectedClass, classColors.getOrDefault(selectedClass, Color.WHITE));
            if (color != null) {
                classColors.put(selectedClass, color);
                if (isClassColorEnabled) {
                    applyCombinedRenderer();
                }
            }

            // Update the legend with the new color
            legendPanel.removeAll();
            for (String className : classNames) {
                JPanel colorLabelPanel = new JPanel(new BorderLayout());

                JLabel colorBox = new JLabel();
                colorBox.setOpaque(true);
                colorBox.setBackground(classColors.getOrDefault(className, Color.WHITE));
                colorBox.setPreferredSize(new Dimension(20, 20));

                JLabel label = new JLabel(className);
                label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

                colorLabelPanel.add(colorBox, BorderLayout.WEST);
                colorLabelPanel.add(label, BorderLayout.CENTER);

                legendPanel.add(colorLabelPanel);
            }

            // Repaint the legend panel to reflect the new colors
            legendPanel.revalidate();
            legendPanel.repaint();
        }
    }

    public void insertRow() {
        int numColumns = tableModel.getColumnCount();
        String[] emptyRow = new String[numColumns];
        tableModel.addRow(emptyRow);
        dataHandler.updateStats(tableModel, statsTextArea);
    }

    public void deleteRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.removeRow(selectedRow);
            dataHandler.updateStats(tableModel, statsTextArea);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void cloneSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            Object[] rowData = new Object[model.getColumnCount()];
            for (int col = 0; col < model.getColumnCount(); col++) {
                rowData[col] = model.getValueAt(selectedRow, col);
            }
            model.addRow(rowData);
            dataHandler.updateStats(tableModel, statsTextArea);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to clone.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportCsvFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("datasets"));
        fileChooser.setDialogTitle("Save CSV File");
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            dataHandler.saveCsvData(filePath, tableModel);
        }
    }

    public void showParallelCoordinatesPlot() {
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
        java.util.List<String[]> data = dataHandler.isDataEmpty() ? dataHandler.getOriginalData() : (isNormalized ? dataHandler.getNormalizedData() : dataHandler.getOriginalData());
    
        // Create and show the plot
        ParallelCoordinatesPlot plot = new ParallelCoordinatesPlot(data, columnNames, classColors, getClassColumnIndex(), columnOrder);
        plot.setVisible(true);
    }

    public void showShiftedPairedCoordinates() {
        java.util.List<java.util.List<Double>> data = new ArrayList<>();
        java.util.List<String> attributeNames = new ArrayList<>();
        java.util.List<String> classLabels = new ArrayList<>();
    
        // Get the reordered column names
        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = columnModel.getColumnCount();
        int[] columnOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnOrder[i] = table.convertColumnIndexToModel(i);
        }
    
        for (int col = 0; col < columnOrder.length; col++) {
            boolean isNumeric = true;
            java.util.List<Double> columnData = new ArrayList<>();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    columnData.add(Double.parseDouble(tableModel.getValueAt(row, columnOrder[col]).toString()));
                } catch (NumberFormatException e) {
                    isNumeric = false;
                    break;
                }
            }
            if (isNumeric) {
                data.add(columnData);
                attributeNames.add(tableModel.getColumnName(columnOrder[col]));
            }
        }
    
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            classLabels.add((String) tableModel.getValueAt(row, getClassColumnIndex()));
        }
    
        // Calculate the number of plots needed
        int numPlots = (attributeNames.size() + 1) / 2;
    
        ShiftedPairedCoordinates shiftedPairedCoordinates = new ShiftedPairedCoordinates(data, attributeNames, classColors, classLabels, numPlots);
        shiftedPairedCoordinates.setVisible(true);
    }    

    public void showRuleTesterDialog() {
        RuleTesterDialog ruleTesterDialog = new RuleTesterDialog(this, tableModel);
        ruleTesterDialog.setVisible(true);
    }

    public void updateSelectedRowsLabel() {
        int selectedRowCount = table.getSelectedRowCount();
        selectedRowsLabel.setText("Selected rows: " + selectedRowCount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CsvViewer viewer = new CsvViewer();
            viewer.setVisible(true);
        });
    }
}
