package src;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateManager {
    private String datasetName;
    private List<String> originalColumnNames;
    private boolean normalized;
    private boolean heatmapEnabled;
    private boolean classColorEnabled;
    private List<Integer> hiddenRows;
    private Map<String, Color> classColors;
    private Map<String, Shape> classShapes;
    private Color cellTextColor;
    private boolean differenceColumnsVisible;

    public StateManager() {
        this.datasetName = "";
        this.originalColumnNames = new ArrayList<>();
        this.normalized = false;
        this.heatmapEnabled = false;
        this.classColorEnabled = false;
        this.hiddenRows = new ArrayList<>();
        this.classColors = new HashMap<>();
        this.classShapes = new HashMap<>();
        this.cellTextColor = Color.BLACK;
        this.differenceColumnsVisible = false;
    }

    public void clearState() {
        originalColumnNames.clear();
        normalized = false;
        heatmapEnabled = false;
        classColorEnabled = false;
        hiddenRows.clear();
        classColors.clear();
        classShapes.clear();
        cellTextColor = Color.BLACK;
        differenceColumnsVisible = false;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public List<String> getOriginalColumnNames() {
        return originalColumnNames;
    }

    public void setOriginalColumnNames(List<String> originalColumnNames) {
        this.originalColumnNames = originalColumnNames;
    }

    public boolean isNormalized() {
        return normalized;
    }

    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    public boolean isHeatmapEnabled() {
        return heatmapEnabled;
    }

    public void setHeatmapEnabled(boolean heatmapEnabled) {
        this.heatmapEnabled = heatmapEnabled;
    }

    public boolean isClassColorEnabled() {
        return classColorEnabled;
    }

    public void setClassColorEnabled(boolean classColorEnabled) {
        this.classColorEnabled = classColorEnabled;
    }

    public List<Integer> getHiddenRows() {
        return hiddenRows;
    }

    public void setHiddenRows(List<Integer> hiddenRows) {
        this.hiddenRows = hiddenRows;
    }

    public Map<String, Color> getClassColors() {
        return classColors;
    }

    public Map<String, Shape> getClassShapes() {
        return classShapes;
    }

    public Color getCellTextColor() {
        return cellTextColor;
    }

    public void setCellTextColor(Color cellTextColor) {
        this.cellTextColor = cellTextColor;
    }

    public boolean areDifferenceColumnsVisible() {
        return differenceColumnsVisible;
    }

    public void setDifferenceColumnsVisible(boolean differenceColumnsVisible) {
        this.differenceColumnsVisible = differenceColumnsVisible;
    }
}
