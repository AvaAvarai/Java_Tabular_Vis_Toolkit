package src;

public class CovariancePair {
    int columnIndex;
    double covariance;

    CovariancePair(int columnIndex, double covariance) {
        this.columnIndex = columnIndex;
        this.covariance = covariance;
    }
}