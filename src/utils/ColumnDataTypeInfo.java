package utils;

import java.util.Map;

/**
 * Stores information about the data type and metadata for a column.
 */
public class ColumnDataTypeInfo {
    private final DataTypeDetector.DataType dataType;
    private final Map<String, Integer> categoricalMapping;
    private final String originalColumnName;
    private final boolean isTransformed;
    
    public ColumnDataTypeInfo(DataTypeDetector.DataType dataType, String originalColumnName) {
        this.dataType = dataType;
        this.originalColumnName = originalColumnName;
        this.categoricalMapping = null;
        this.isTransformed = false;
    }
    
    public ColumnDataTypeInfo(DataTypeDetector.DataType dataType, String originalColumnName, 
                             Map<String, Integer> categoricalMapping) {
        this.dataType = dataType;
        this.originalColumnName = originalColumnName;
        this.categoricalMapping = categoricalMapping;
        this.isTransformed = true;
    }
    
    public DataTypeDetector.DataType getDataType() {
        return dataType;
    }
    
    public Map<String, Integer> getCategoricalMapping() {
        return categoricalMapping;
    }
    
    public String getOriginalColumnName() {
        return originalColumnName;
    }
    
    public boolean isTransformed() {
        return isTransformed;
    }
    
    /**
     * Converts a value to its numerical representation for visualization
     * @param value Original value as string
     * @return Numerical representation
     */
    public double convertToNumerical(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        
        return switch (dataType) {
            case NUMERICAL -> {
                try {
                    yield Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            case BINARY -> DataTypeDetector.convertBinaryToInt(value);
            case CATEGORICAL -> {
                if (categoricalMapping != null && categoricalMapping.containsKey(value.trim())) {
                    yield categoricalMapping.get(value.trim());
                }
                yield 0.0;
            }
            case TIMESTAMP -> DataTypeDetector.convertTimestampToLong(value) / 1000.0; // Convert to seconds
            case NOMINAL -> {
                // For nominal data, use hash code as numerical representation
                yield Math.abs(value.hashCode()) % 1000; // Keep values in reasonable range
            }
            case LABEL -> {
                // For label data, use hash code as numerical representation
                yield Math.abs(value.hashCode()) % 1000; // Keep values in reasonable range
            }
            case UNKNOWN -> 0.0;
        };
    }
    
    /**
     * Gets the original value from a numerical representation (for categorical data)
     * @param numericalValue Numerical representation
     * @return Original string value, or null if not found
     */
    public String getOriginalValue(double numericalValue) {
        if (dataType == DataTypeDetector.DataType.CATEGORICAL && categoricalMapping != null) {
            int intValue = (int) Math.round(numericalValue);
            for (Map.Entry<String, Integer> entry : categoricalMapping.entrySet()) {
                if (entry.getValue() == intValue) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    /**
     * Gets a display name for the column that includes data type information
     */
    public String getDisplayName() {
        String typeInfo = switch (dataType) {
            case NUMERICAL -> "[NUM]";
            case CATEGORICAL -> "[CAT]";
            case NOMINAL -> "[NOM]";
            case BINARY -> "[BIN]";
            case TIMESTAMP -> "[TIME]";
            case LABEL -> "[LABEL]";
            case UNKNOWN -> "[?]";
        };
        return originalColumnName + " " + typeInfo;
    }
}
