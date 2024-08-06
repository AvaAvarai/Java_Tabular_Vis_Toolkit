import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TableRowTransferHandler extends TransferHandler {
    private final JTable table;

    public TableRowTransferHandler(JTable table) {
        this.table = table;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        int[] rows = table.getSelectedRows();
        Object[][] data = new Object[rows.length][table.getColumnCount()];

        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                data[i][j] = table.getValueAt(rows[i], j);
            }
        }

        return new TableRowsTransferable(data);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && support.isDataFlavorSupported(TableRowsTransferable.DATA_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
        int row = dropLocation.getRow();
        Transferable transferable = support.getTransferable();

        try {
            Object[][] data = (Object[][]) transferable.getTransferData(TableRowsTransferable.DATA_FLAVOR);
            DefaultTableModel model = (DefaultTableModel) table.getModel();

            // Insert rows at the new location
            for (Object[] rowData : data) {
                model.insertRow(row, rowData);
            }

            // Remove original rows after insertion
            int[] rows = table.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }

            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
