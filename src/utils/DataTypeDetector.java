package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for detecting and handling different data types in CSV columns.
 * Supports: Numerical (integer/float), Categorical, Nominal, Binary, and Timestamp data.
 */
public class DataTypeDetector {
    
    public enum DataType {
        NUMERICAL,      // Integer or float values
        CATEGORICAL,    // Limited set of string values (mapped to integers)
        NOMINAL,        // String values like IP addresses, IDs, etc.
        BINARY,         // 0/1 or true/false values
        TIMESTAMP,      // Date/time values
        LABEL,          // Class/label column for classification
        UNKNOWN         // Cannot be determined
    }
    
    // Common timestamp patterns
    private static final DateTimeFormatter[] TIMESTAMP_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    };
    
    // IP address pattern
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // Binary value patterns
    private static final Set<String> BINARY_VALUES = Set.of("0", "1", "true", "false", "yes", "no", "y", "n");
    
    /**
     * Detects the data type of a column based on its values.
     * @param values Array of string values from the column
     * @return Detected data type
     */
    public static DataType detectDataType(String[] values) {
        if (values == null || values.length == 0) {
            return DataType.UNKNOWN;
        }
        
        // Count non-empty values
        List<String> nonEmptyValues = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                nonEmptyValues.add(value.trim());
            }
        }
        
        if (nonEmptyValues.isEmpty()) {
            return DataType.UNKNOWN;
        }
        
        // Check for binary data first (most restrictive)
        if (isBinaryData(nonEmptyValues)) {
            return DataType.BINARY;
        }
        
        // Check for numerical data
        if (isNumericalData(nonEmptyValues)) {
            return DataType.NUMERICAL;
        }
        
        // Check for timestamp data
        if (isTimestampData(nonEmptyValues)) {
            return DataType.TIMESTAMP;
        }
        
        // Check for categorical data (limited unique values)
        if (isCategoricalData(nonEmptyValues)) {
            return DataType.CATEGORICAL;
        }
        
        // Everything else is nominal
        return DataType.NOMINAL;
    }
    
    /**
     * Checks if values represent binary data (0/1, true/false, etc.)
     */
    private static boolean isBinaryData(List<String> values) {
        Set<String> uniqueValues = new HashSet<>(values);
        return uniqueValues.size() <= 2 && 
               uniqueValues.stream().allMatch(BINARY_VALUES::contains);
    }
    
    /**
     * Checks if values represent numerical data (integers or floats)
     */
    private static boolean isNumericalData(List<String> values) {
        int numericCount = 0;
        for (String value : values) {
            try {
                Double.parseDouble(value);
                numericCount++;
            } catch (NumberFormatException e) {
                // Not numeric
            }
        }
        // Consider numerical if at least 80% of values are numeric
        return (double) numericCount / values.size() >= 0.8;
    }
    
    /**
     * Checks if values represent timestamp data
     */
    private static boolean isTimestampData(List<String> values) {
        int timestampCount = 0;
        for (String value : values) {
            if (isValidTimestamp(value)) {
                timestampCount++;
            }
        }
        // Consider timestamp if at least 80% of values are valid timestamps
        return (double) timestampCount / values.size() >= 0.8;
    }
    
    /**
     * Checks if a single value is a valid timestamp
     */
    private static boolean isValidTimestamp(String value) {
        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try {
                LocalDateTime.parse(value, formatter);
                return true;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return false;
    }
    
    /**
     * Checks if values represent categorical data (limited unique values)
     */
    private static boolean isCategoricalData(List<String> values) {
        Set<String> uniqueValues = new HashSet<>(values);
        // Consider categorical if there are 2-20 unique values and not too many total values
        return uniqueValues.size() >= 2 && 
               uniqueValues.size() <= 20 && 
               values.size() >= uniqueValues.size() * 2; // At least 2 instances per category
    }
    
    /**
     * Creates a mapping from categorical values to integers
     * @param values Array of categorical values
     * @return Map from original values to integer representations
     */
    public static Map<String, Integer> createCategoricalMapping(String[] values) {
        Set<String> uniqueValues = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                uniqueValues.add(value.trim());
            }
        }
        
        List<String> sortedValues = new ArrayList<>(uniqueValues);
        Collections.sort(sortedValues);
        
        Map<String, Integer> mapping = new HashMap<>();
        for (int i = 0; i < sortedValues.size(); i++) {
            mapping.put(sortedValues.get(i), i);
        }
        
        return mapping;
    }
    
    /**
     * Converts binary values to 0/1 representation
     * @param value Binary value as string
     * @return Integer representation (0 or 1)
     */
    public static int convertBinaryToInt(String value) {
        if (value == null) return 0;
        String trimmed = value.trim().toLowerCase();
        return switch (trimmed) {
            case "1", "true", "yes", "y" -> 1;
            case "0", "false", "no", "n" -> 0;
            default -> 0;
        };
    }
    
    /**
     * Converts timestamp to numerical representation (milliseconds since epoch)
     * @param timestamp Timestamp string
     * @return Milliseconds since epoch, or 0 if parsing fails
     */
    public static long convertTimestampToLong(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return 0;
        }
        
        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
                return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return 0;
    }
    
    /**
     * Checks if a value looks like an IP address
     */
    public static boolean isIPAddress(String value) {
        return value != null && IP_PATTERN.matcher(value.trim()).matches();
    }
    
    /**
     * Gets a human-readable description of the data type
     */
    public static String getDataTypeDescription(DataType type) {
        return switch (type) {
            case NUMERICAL -> "Numerical (Integer/Float)";
            case CATEGORICAL -> "Categorical (Limited Categories)";
            case NOMINAL -> "Nominal (Text/ID)";
            case BINARY -> "Binary (0/1, True/False)";
            case TIMESTAMP -> "Timestamp (Date/Time)";
            case LABEL -> "Label (Class/Target)";
            case UNKNOWN -> "Unknown";
        };
    }
}
