package src;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class ReorderableTableModel extends DefaultTableModel {
    public ReorderableTableModel() {
        super();
    }

    public ReorderableTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    public void moveRow(int start, int end, int to) {
        if (start == end) {
            return;
        }

        Vector<Object> rows = new Vector<>();
        for (int i = start; i <= end; i++) {
            rows.add(getDataVector().remove(start));
        }

        for (Object row : rows) {
            getDataVector().add(to++, (Vector<?>) row);
        }

        fireTableRowsDeleted(start, end);
        fireTableRowsInserted(to - rows.size(), to - 1);
    }
}
