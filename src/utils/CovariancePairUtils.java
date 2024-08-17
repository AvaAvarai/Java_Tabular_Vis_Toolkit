package src.utils;

public class CovariancePairUtils {
    private int columnIndex;
    private double covariance;

    public CovariancePairUtils(int columnIndex, double covariance) {
        this.columnIndex = columnIndex;
        this.covariance = covariance;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public double getCovariance() {
        return covariance;
    }
}