package src;

import src.utils.PureRegionUtils;
import src.table.NumericStringComparator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class PureRegionManager {
    private final CsvViewer csvViewer;
    private final DefaultTableModel tableModel;
    private final JTextArea statsTextArea;
    private final JSlider thresholdSlider;
    private Set<Integer> hiddenRows;

    public PureRegionManager(CsvViewer csvViewer, DefaultTableModel tableModel, JTextArea statsTextArea, JSlider thresholdSlider) {
        this.csvViewer = csvViewer;
        this.tableModel = tableModel;
        this.statsTextArea = statsTextArea;
        this.thresholdSlider = thresholdSlider;
        this.hiddenRows = new HashSet<>();
    }

    public double calculateAndDisplayPureRegions(int thresholdPercentage) {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            csvViewer.noDataLoadedError();
            return 0;
        }

        List<PureRegionUtils> pureRegions = PureRegionUtils.calculatePureRegions(tableModel, thresholdPercentage, classColumnIndex);

        int totalRows = tableModel.getRowCount();
        hiddenRows = new HashSet<>();
        for (PureRegionUtils region : pureRegions) {
            for (int row = 0; row < totalRows; row++) {
                String attributeName = region.getAttributeName();
                int attributeColumnIndex = tableModel.findColumn(attributeName);

                if (attributeColumnIndex != -1) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(row, classColumnIndex).toString();

                        if (value >= region.getStart() && value < region.getEnd() && className.equals(region.getCurrentClass())) {
                            hiddenRows.add(row);
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numerical values
                    }
                }
            }
        }

        double remainingCoverage = ((totalRows - hiddenRows.size()) / (double) totalRows) * 100.0;

        displayPureRegions(pureRegions);

        return remainingCoverage;
    }

    public void toggleEasyCases() {
        if (hiddenRows.isEmpty()) {
            hideEasyCases();
            csvViewer.updateToggleEasyCasesButton(false);
        } else {
            showEasyCases();
            csvViewer.updateToggleEasyCasesButton(true);
        }
    }

    public void hideEasyCases() {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            csvViewer.noDataLoadedError();
            return;
        }

        int currentThreshold = thresholdSlider.getValue();
        List<PureRegionUtils> pureRegions = PureRegionUtils.calculatePureRegions(tableModel, currentThreshold, classColumnIndex);
        Set<Integer> rowsToHide = new HashSet<>();

        for (PureRegionUtils region : pureRegions) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String attributeName = region.getAttributeName();
                int attributeColumnIndex = tableModel.findColumn(attributeName);

                if (attributeColumnIndex != -1) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(row, classColumnIndex).toString();

                        if (value >= region.getStart() && value < region.getEnd() && className.equals(region.getCurrentClass())) {
                            rowsToHide.add(row);
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numerical values
                    }
                }
            }
        }

        hiddenRows.clear();
        hiddenRows.addAll(rowsToHide);
        applyRowFilter();
        csvViewer.updateSelectedRowsLabel();
    }

    public void showEasyCases() {
        hiddenRows.clear();
        applyRowFilter();
        csvViewer.updateSelectedRowsLabel();
    }

    void applyRowFilter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return !hiddenRows.contains(entry.getIdentifier());
            }
        });
        // Apply the custom comparator for each column
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            sorter.setComparator(i, new NumericStringComparator());
        }
        csvViewer.getTable().setRowSorter(sorter);
        csvViewer.updateSelectedRowsLabel();
    }

    public int calculateRemainingCases(int threshold) {
        int classColumnIndex = csvViewer.getClassColumnIndex();
        if (classColumnIndex == -1) {
            csvViewer.noDataLoadedError();
            return 0;
        }

        List<PureRegionUtils> pureRegions = PureRegionUtils.calculatePureRegions(tableModel, threshold, classColumnIndex);
        Set<Integer> hiddenRows = new HashSet<>();

        for (PureRegionUtils region : pureRegions) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                String attributeName = region.getAttributeName();
                int attributeColumnIndex = tableModel.findColumn(attributeName);

                if (attributeColumnIndex != -1) {
                    try {
                        double value = Double.parseDouble(tableModel.getValueAt(row, attributeColumnIndex).toString());
                        String className = tableModel.getValueAt(row, classColumnIndex).toString();

                        if (value >= region.getStart() && value < region.getEnd() && className.equals(region.getCurrentClass())) {
                            hiddenRows.add(row);
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numerical values
                    }
                }
            }
        }

        return tableModel.getRowCount() - hiddenRows.size();
    }

    private void displayPureRegions(List<PureRegionUtils> pureRegions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Single-Attribute Pure Regions:\n");
        for (int i = pureRegions.size() - 1; i >= 0; i--) {
            PureRegionUtils region = pureRegions.get(i);
            sb.append(String.format("Attribute: %s, Pure Region: %.2f <= %s < %.2f, Class: %s, Count: %d (%.2f%% of class, %.2f%% of dataset)\n",
                    region.getAttributeName(), region.getStart(), region.getAttributeName(), region.getEnd(),
                    region.getCurrentClass(), region.getRegionCount(), region.getPercentageOfClass(), region.getPercentageOfDataset()));
        }
        csvViewer.getDataHandler().updateStats(tableModel, statsTextArea);
        statsTextArea.append(sb.toString());
    }
}
