package src;

import java.awt.Shape;
import java.awt.geom.Path2D;

public class ShapeUtils {
    public static Shape createStar(int arms, double rOuter, double rInner) {
        double angle = Math.PI / arms;
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 2 * arms; i++) {
            double r = (i & 1) == 0 ? rOuter : rInner;
            double x = Math.cos(i * angle) * r;
            double y = Math.sin(i * angle) * r;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        return path;
    }
}
