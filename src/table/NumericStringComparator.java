package src.table;

import java.util.Comparator;

public class NumericStringComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        // Try to parse both strings as doubles
        Double d1 = tryParseDouble(o1);
        Double d2 = tryParseDouble(o2);

        // If both are numbers, compare them numerically
        if (d1 != null && d2 != null) {
            return Double.compare(d1, d2);
        }

        // If only one is a number, the numeric one should come first
        if (d1 != null) return -1;
        if (d2 != null) return 1;

        // Fall back to lexicographical comparison if neither is a number
        return o1.compareTo(o2);
    }

    // Helper method to try to parse a string as a double
    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null; // Return null if parsing fails
        }
    }
}
