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
    final String status;
    byte[] proxyResponse;
    public String requestTime;
    public String proxyHost;
    public String length;

    LogEntry(int id, IHttpRequestResponsePersisted requestResponse, URL url, String method, Map<String,Object> mapResult) {
        this.id = id;
        this.requestResponse = requestResponse;
        this.url = url;
        this.method = method;
        this.status = (String) mapResult.get("status");
        //this.proxyResponse = (String)mapResult.get("header") + mapResult.get("result");
        byte[] headBytes = ((String) mapResult.get("header")).getBytes();
        byte[] bodyBytes = (byte[]) mapResult.get("result");
        this.proxyResponse = UtilsPlus.concatenateByteArrays(headBytes,bodyBytes);
        this.requestTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
        this.proxyHost = (String) mapResult.get("proxyHost");
        this.length = String.format("%s|%s", headBytes.length, bodyBytes.length);
    }


}
