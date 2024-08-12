package src;

public class PureRegion {
    String attributeName;
    double start;
    double end;
    String currentClass;
    int regionCount;
    double percentageOfClass;
    double percentageOfDataset;

    public PureRegion(String attributeName, double start, double end, String currentClass,
                      int regionCount, double percentageOfClass, double percentageOfDataset) {
        this.attributeName = attributeName;
        this.start = start;
        this.end = end;
        this.currentClass = currentClass;
        this.regionCount = regionCount;
        this.percentageOfClass = percentageOfClass;
        this.percentageOfDataset = percentageOfDataset;
    }
}
