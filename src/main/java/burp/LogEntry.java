package burp;

import plus.UtilsPlus;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class LogEntry {
    final int id;
    final IHttpRequestResponsePersisted requestResponse;
    final URL url;
    final String method;
    final String respStatus;
    byte[] proxyResponse;
    public String requestTime;
    public String proxyHost;
    public String respLength;
    public String equalStatus;
    public String equalLength;

    LogEntry(int id, IHttpRequestResponsePersisted requestResponse, URL url, String method, Map<String,Object> mapResult) {
        this.id = id;
        this.requestResponse = requestResponse;
        this.url = url;
        this.method = method;
        this.respStatus = (String) mapResult.get("respStatus");
        byte[] headBytes = ((String) mapResult.get("header")).getBytes();
        byte[] bodyBytes = (byte[]) mapResult.get("respBody");
        this.proxyResponse = UtilsPlus.concatenateByteArrays(headBytes,bodyBytes);
        this.requestTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
        this.proxyHost = (String) mapResult.get("proxyHost");
        this.respLength = String.format("%s|%s", headBytes.length, bodyBytes.length);
        this.equalStatus = String.format("%s", mapResult.get("equalStatus"));
        this.equalLength = String.format("%s", mapResult.get("equalLength"));
    }
}
