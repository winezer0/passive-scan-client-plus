package burp;

import javax.swing.*;
import javax.swing.table.TableModel;

public class HttpLogTable extends JTable {
    private HttpLogTableModel httpLogTableModel;

    public HttpLogTableModel getHttpLogTableModel() {
        return httpLogTableModel;
    }


    public HttpLogTable(TableModel tableModel) {
        super(tableModel);
        this.httpLogTableModel = (HttpLogTableModel) tableModel;
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        super.changeSelection(row, col, toggle, extend);
        //show the log entry for the selected row
        LogEntry logEntry = BurpExtender.log.get(row);

        //自动设置请求响应宽度
        GUI.proxyMsgViewerAutoSetSplitCenter();
        GUI.originalMsgViewerAutoSetSplitCenter();

        GUI.proxyRequestViewer.setMessage(logEntry.requestResponse.getRequest(), true);
        GUI.proxyResponseViewer.setMessage(logEntry.proxyResponse, false);
        GUI.originalRequestViewer.setMessage(logEntry.requestResponse.getRequest(), true);
        GUI.originalResponseViewer.setMessage(logEntry.requestResponse.getResponse(), false);
        GUI.currentlyDisplayedItem = logEntry.requestResponse;
    }
}
