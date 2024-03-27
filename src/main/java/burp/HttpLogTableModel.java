package burp;

import javax.swing.table.AbstractTableModel;

public class HttpLogTableModel extends AbstractTableModel {
    public int getRowCount() {
        return BurpExtender.log.size();
    }

    public int getColumnCount() {
        return 8;
    }

    @Override
    public String getColumnName(int columnIndex) {

        switch (columnIndex)
        {
            case 0:
                return "#";
            case 1:
                return "ProxyHost";
            case 2:
                return "Method";
            case 3:
                return "URL";
            case 4:
                return "RespStatus";
            case 5:
                return "RespLength";
            case 6:
                return "StatusEqual";
            case 7:
                return "Time";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return String.class;
    }


    public Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry logEntry = BurpExtender.log.get(rowIndex);

        switch (columnIndex) {
            case 0:
                return logEntry.id;
            case 1:
                return logEntry.proxyHost;
            case 2:
                return logEntry.method;
            case 3:
                return logEntry.url.toString();
            case 4:
                return logEntry.respStatus;
            case 5:
                return logEntry.respLength;
            case 6:
                return logEntry.statusEqual;
            case 7:
                return logEntry.requestTime;
            default:
                return "";
        }
    }
}
