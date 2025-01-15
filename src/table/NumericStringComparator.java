package src.table;

import java.util.Comparator;

public class NumericStringComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        if (o1 == null || o2 == null) {
            if (o1 == o2) return 0;
            return o1 == null ? -1 : 1;
        }

        // Clean the strings and handle empty cases
        o1 = o1.trim();
        o2 = o2.trim();
        if (o1.isEmpty() || o2.isEmpty()) {
            if (o1.isEmpty() && o2.isEmpty()) return 0;
            return o1.isEmpty() ? -1 : 1;
        }

        try {
            // Parse as doubles for proper numeric comparison
            double d1 = Double.parseDouble(o1);
            double d2 = Double.parseDouble(o2);
            
            // Handle the case where numbers are very close to zero
            double epsilon = 1e-10;
            if (Math.abs(d1) < epsilon) d1 = 0.0;
            if (Math.abs(d2) < epsilon) d2 = 0.0;
            
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            // If parsing fails, fall back to string comparison
            return o1.compareTo(o2);
        }
    }
}
