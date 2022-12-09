package burp;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.Proxy.Type;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


//https://blog.csdn.net/sbc1232123321/article/details/79334130
public class HttpAndHttpsProxy {

    //修改 输出url去重处理
    public static Map<String,String> Proxy(IHttpRequestResponse requestResponse) throws InterruptedException{
        //public static Map<String,String> Proxy(IHttpRequestResponse requestResponse, Set reqBodyHashSet) throws InterruptedException{
        byte[] request = requestResponse.getRequest();
        String reqUrl = null;
        byte[] reqBody = null;
        String body = null;
        List<String> reqHeaders = null;

        IHttpService httpService = requestResponse.getHttpService();
        IRequestInfo reqInfo = BurpExtender.helpers.analyzeRequest(httpService,request);

        reqUrl = reqInfo.getUrl().toString();
        reqHeaders = reqInfo.getHeaders();
        List<IParameter> reqParams = reqInfo.getParameters();

        //忽略无参数目标
        if(Config.REQ_PARAM){
            //判断是否存在参数
            if(reqParams.size()<=0){
                Utils.showStderrMsgDebug(String.format("[-] Ignored By Param Blank: %s", reqUrl));
                return null;
            }
        }

        if(reqInfo.getMethod().equals("POST")){
            int bodyOffset = reqInfo.getBodyOffset();
            try {
                body = new String(request, bodyOffset, request.length - bodyOffset, "UTF-8");
                reqBody = body.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        //忽略重复参数的请求
        if(Config.REQ_SMART) {
            String reqUrlNoParam = reqUrl.split("\\?",2)[0];
            byte contentType = reqInfo.getContentType();
            String reqUrlWithType = String.format("%s[type:%s]", reqUrlNoParam,contentType);
            Utils.showStdoutMsgDebug(String.format("[*] Current reqUrlWithType : %s", reqUrlWithType));

            //格式化处理每个请求的参数
            String reqParamsJsonStr = "";
            //处理Json格式的请求
            if(contentType == IRequestInfo.CONTENT_TYPE_JSON
                    && !Utils.isEmpty(body)
                    && Utils.countStr(body,"{" ,2)
                    && Utils.isJson(body)){
                HashMap reqParamsMap = Utils.handleJsonParamsStr(body);
                reqParamsJsonStr = Utils.getReqParamsMapJsonStr(reqParamsMap);
            }else {
                //通用的参数Json获取方案
                reqParamsJsonStr = Utils.getReqCommonParamsJsonStr(reqParams);
            }
            Boolean isUniq = Utils.isUniqReqInfo(Config.reqInfoHashMap, reqUrlWithType, reqParamsJsonStr);
            if(!isUniq){
                Utils.showStderrMsgDebug(String.format("[-] Ignored By Param Duplication: %s %s", reqUrlWithType, reqParamsJsonStr));
                return null;
            }

            //内存记录数量超过限制,清空 reqInfoHashMap
            if(Config.HASH_MAP_LIMIT <= Config.reqInfoHashMap.size()){
                Utils.showStdoutMsgInfo(String.format("[-] Clear HashMap Content By Exceed Limit."));
                Config.reqInfoHashMap.clear();
            }
        }


        //输出url去重处理
        if(Config.REQ_UNIQ) {
            String url_body = Utils.getReqInfoHash(reqUrl, reqBody);
            //新增 输出url去重处理  记录请求URL和body对应hash
            if (Config.reqInfoHashSet.contains(url_body)) {
                Utils.showStderrMsgDebug(String.format("[-] Ignored By URL&Body(md5): %s", url_body));
                return null;
            } else {
                Utils.showStdoutMsgDebug(String.format("[+] Firstly REQ URL&Body(md5): %s", url_body));
                Config.reqInfoHashSet.add(url_body);
            }

            //内存记录数量超过限制,清空 reqInfoHashSet
            if(Config.HASH_SET_LIMIT <= Config.reqInfoHashSet.size()){
                Utils.showStdoutMsgInfo(String.format("[-] Clear HashSet Content By Exceed Limit."));
                Config.reqInfoHashSet.clear();
            }
        }

        //延迟转发
        Thread.sleep(Config.INTERVAL_TIME);
        if(httpService.getProtocol().equals("https")){
            //修改 输出url去重处理
            return HttpsProxy(reqUrl, reqHeaders, reqBody, Config.PROXY_HOST, Config.PROXY_PORT,Config.PROXY_USERNAME,Config.PROXY_PASSWORD);
        }else {
            //修改 输出url去重处理
            return HttpProxy(reqUrl, reqHeaders, reqBody, Config.PROXY_HOST, Config.PROXY_PORT,Config.PROXY_USERNAME,Config.PROXY_PASSWORD);
        }
    }

    //感谢chen1sheng的pr，已经修改了我漏修复的https转发bug，并解决了header截断的bug。
    public static Map<String,String> HttpsProxy(String url, List<String> headers,byte[] body, String proxy, int port,String username,String password){
    //public static Map<String,String> HttpsProxy(Set reqBodyHashSet, String url_body, String url, List<String> headers,byte[] body, String proxy, int port,String username,String password){
        Map<String,String> mapResult = new HashMap<String,String>();
        String status = "";
        String rspHeader = "";
        String result = "";

        HttpsURLConnection httpsConn = null;
        PrintWriter out = null;
        BufferedReader in = null;
        BufferedReader reader = null;
        try {
            URL urlClient = new URL(url);
            SSLContext sc = SSLContext.getInstance("SSL");
            // 指定信任https
            sc.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());
            //创建代理虽然是https也是Type.HTTP
            Proxy proxy1=new Proxy(Type.HTTP, new InetSocketAddress(proxy, port));
            //设置代理
            httpsConn = (HttpsURLConnection) urlClient.openConnection(proxy1);

            //设置账号密码
            if(username != null && password != null && username.trim().length() > 0 && password.trim().length() > 0){
                String user_pass = String.format("%s:%s", username, password);
                String headerKey = "Proxy-Authorization";
                String headerValue = "Basic " + Base64.encode(user_pass.getBytes());
                Utils.showStdoutMsgDebug(String.format("[*] Set [%s] Proxy-Authorization Data: [%s]", user_pass, headerValue));
                httpsConn.setRequestProperty(headerKey, headerValue);
            }

            httpsConn.setSSLSocketFactory(sc.getSocketFactory());
            httpsConn.setHostnameVerifier(new TrustAnyHostnameVerifier());

            //设置控制请求方法的Flag
            String methodFlag = "";
            // 设置通用的请求属性
            for(String header:headers){
                if(header.startsWith("GET") || header.startsWith("POST") || header.startsWith("PUT")){
                    if(header.startsWith("GET")){
                        methodFlag = "GET";
                    }
                    else if(header.startsWith("POST")|| header.startsWith("PUT")){
                        methodFlag = "POST";
                    }//在循环中重复设置了methodFlag，代码非常的丑陋冗余，请见谅
                    continue;
                }//判断结束后以键值对的方式获取header
                String[] h = header.split(":",2);
                String header_key = h[0].trim();
                String header_value = h[1].trim();
                httpsConn.setRequestProperty(header_key, header_value);
                //BurpExtender.stdout.println(header_key + ":" + header_value);
            }

            if (methodFlag.equals("GET")){
                // 发送GET请求必须设置如下两行
                httpsConn.setDoOutput(false);
                httpsConn.setDoInput(true);

                // 获取URLConnection对象的连接
                httpsConn.connect();
            }
            else if(methodFlag.equals("POST")){
                // 发送POST请求必须设置如下两行
                httpsConn.setDoOutput(true);
                httpsConn.setDoInput(true);

                // 获取URLConnection对象对应的输出流
                out = new PrintWriter(httpsConn.getOutputStream());
                if(body != null) {
                    // 发送请求参数
                    out.print(new String(body));
                }
                // flush输出流的缓冲
                out.flush();
            }
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(httpsConn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
                result += "\r\n";
            }
            // 断开连接
            httpsConn.disconnect();
            // 获取响应头
            Map<String, List<String>> mapHeaders = httpsConn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : mapHeaders.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                String value = "";
                for(String v:values){
                    value += v;
                }

                if(key == null) {
                    String header_line = String.format("%s\r\n",value);
                    rspHeader += header_line;
                }else{
                    String header_line = String.format("%s: %s\r\n", key, value);
                    rspHeader += header_line;
                }
            }

            //BurpExtender.stdout.println("返回结果https：" + httpsConn.getResponseMessage());
            status = String.valueOf(httpsConn.getResponseCode());
            Utils.updateSuccessCount();
        } catch (Exception e) {
            //e.printStackTrace();
            result = e.getMessage();
            Utils.showStderrMsgDebug("[-] First Times: " + e.getMessage());
            Utils.updateFailCount();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            if (out != null) {
                out.close();
            }
        }

        //再次获取状态码
        try {
            status = String.valueOf(httpsConn.getResponseCode());
        } catch (IOException e) {
            status = e.getMessage();
            Utils.showStderrMsgDebug("[-] Second Times: " + e.getMessage());
            //新增 不记录错误响应的请求
            if(Config.REQ_UNIQ) {
                String url_body = Utils.getReqInfoHash(url, body);
                if(Config.reqInfoHashSet.contains(url_body)){
                    Config.reqInfoHashSet.remove(url_body);//新增
                    Utils.showStderrMsgInfo(String.format("[!] Remove Hashset Record: %s", url_body) );
                }
            }
        }

        mapResult.put("status",status);
        mapResult.put("header",rspHeader);
        mapResult.put("result",result);
        return mapResult;
    }

    public static Map<String,String> HttpProxy(String url,List<String> headers,byte[] body, String proxy, int port,String username,String password) {
        //public static Map<String,String> HttpProxy(Set reqBodyHashSet, String url_body,String url,List<String> headers,byte[] body, String proxy, int port,String username,String password) {
        Map<String,String> mapResult = new HashMap<String,String>();
        String status = "";
        String rspHeader = "";
        String result = "";

        HttpURLConnection httpConn = null;
        PrintWriter out = null;
        BufferedReader in = null;
        BufferedReader reader = null;
        try {
            URL urlClient = new URL(url);
            SSLContext sc = SSLContext.getInstance("SSL");
            // 指定信任https
            sc.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());
            //创建代理
            Proxy proxy1=new Proxy(Type.HTTP, new InetSocketAddress(proxy, port));
            //设置代理
            httpConn = (HttpURLConnection) urlClient.openConnection(proxy1);

            //设置账号密码
            if(username != null && password != null && username.trim().length() > 0 && password.trim().length() > 0){
                String user_pass = String.format("%s:%s", username, password);
                String headerKey = "Proxy-Authorization";
                String headerValue = "Basic " + Base64.encode(user_pass.getBytes());
                Utils.showStdoutMsgDebug(String.format("[*] Set [%s] Proxy-Authorization Data: [%s]", user_pass, headerValue));
                httpConn.setRequestProperty(headerKey, headerValue);
            }

            //设置控制请求方法的Flag
            String methodFlag = "";
            // 设置通用的请求属性
            for(String header:headers){
                if(header.startsWith("GET") || header.startsWith("POST") || header.startsWith("PUT")){
                    if(header.startsWith("GET")){
                        methodFlag = "GET";
                    }
                    else if(header.startsWith("POST")|| header.startsWith("PUT")){
                        methodFlag = "POST";
                    }//在循环中重复设置了methodFlag，代码非常的丑陋冗余，请见谅
                    continue;
                }//判断结束后以键值对的方式获取header
                String[] h = header.split(":",2);
                String header_key = h[0].trim();
                String header_value = h[1].trim();
                httpConn.setRequestProperty(header_key, header_value);
                //BurpExtender.stdout.println(header_key + ":" + header_value);
            }

            if (methodFlag.equals("GET")){
                // 发送GET请求必须设置如下两行
                httpConn.setDoOutput(false);
                httpConn.setDoInput(true);

                // 获取URLConnection对象的连接
                httpConn.connect();
            }
            else if(methodFlag.equals("POST")){
                // 发送POST请求必须设置如下两行
                httpConn.setDoOutput(true);
                httpConn.setDoInput(true);

                // 获取URLConnection对象对应的输出流
                out = new PrintWriter(httpConn.getOutputStream());
                if(body != null) {
                    // 发送请求参数
                    out.print(new String(body));
                }
                // flush输出流的缓冲
                out.flush();
            }
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
                result += "\r\n";
            }
            // 断开连接
            httpConn.disconnect();
            // 获取响应头
            Map<String, List<String>> mapHeaders = httpConn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : mapHeaders.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                String value = "";
                for(String v:values){
                    value += v;
                }

                if(key == null) {
                    String header_line = String.format("%s\r\n",value);
                    rspHeader += header_line;
                }else{
                    String header_line = String.format("%s: %s\r\n", key, value);
                    rspHeader += header_line;
                }
            }

            //BurpExtender.stdout.println("返回结果http：" + httpConn.getResponseMessage());
            status = String.valueOf(httpConn.getResponseCode());
            Utils.updateSuccessCount();
        } catch (Exception e) {
            //e.printStackTrace();
            result = e.getMessage();
            Utils.showStderrMsgDebug("[-] First Times: " + e.getMessage());
            Utils.updateFailCount();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            if (out != null) {
                out.close();
            }
        }

        //再次获取状态码
        try {
            status = String.valueOf(httpConn.getResponseCode());
        } catch (IOException e) {
            status = e.getMessage();
            Utils.showStderrMsgDebug("[-] Second Times: " + e.getMessage());
            //新增 不记录错误响应的请求
            if(Config.REQ_UNIQ) {
                String url_body = Utils.getReqInfoHash(url, body);
                if(Config.reqInfoHashSet.contains(url_body)){
                    Config.reqInfoHashSet.remove(url_body);//新增
                    Utils.showStderrMsgInfo(String.format("[!] Remove Hashset Record: %s", url_body) );
                }
            }
        }

        mapResult.put("status",status);
        mapResult.put("header",rspHeader);
        mapResult.put("result",result);
        return mapResult;
    }

    private static class TrustAnyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}