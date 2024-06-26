package burp;

import plus.Base64;
import plus.UtilsPlus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.Proxy.Type;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import static burp.Utils.*;


//JAVA设置代理的两种方式（HTTP和HTTPS） https://blog.csdn.net/sbc1232123321/article/details/79334130
public class HttpAndHttpsProxy {

    public static Map<String,Object> Proxy(IHttpRequestResponse requestResponse) throws InterruptedException{
        byte[] request = requestResponse.getRequest();
        IHttpService httpService = requestResponse.getHttpService();
        IRequestInfo reqInfo = BurpExtender.helpers.analyzeRequest(httpService,request);

        String reqUrl = reqInfo.getUrl().toString();
        List<String> reqHeaders = reqInfo.getHeaders();
        List<IParameter> reqParams = reqInfo.getParameters();
        byte[] reqBodyBytes = null;
        String reqBodyString = null;
        //HASHMAP 记录当前请求的Key,传递给下一个函数支持错误删除
        HashMap<String, String> ReqKeyHashMap = new HashMap<>();

        //忽略无参数目标
        if(Config.REQ_PARAM){
            //判断是否存在参数
            if(reqParams.size()<=0){
                Utils.showStderrMsg(1, String.format("[-] Ignored By Param Blank: %s", reqUrl));
                return null;
            }
        }

        if(reqInfo.getMethod().equals("POST")){
            int bodyOffset = reqInfo.getBodyOffset();
            //reqBodyString = new String(request, bodyOffset, request.length - bodyOffset, StandardCharsets.UTF_8);
            //reqBodyBytes = reqBodyString.getBytes(StandardCharsets.UTF_8);
            reqBodyBytes = UtilsPlus.getBodyBytes(request, bodyOffset);
            reqBodyString = new String(reqBodyBytes, StandardCharsets.UTF_8);
        }

        //计算认证相关的头信息
        String authParamsJsonStr = "";

        if(Config.REQ_AUTH) {
            //考虑添加auth头相关的信息 //将所有认证头信息组成一个Json字符串追加到URL后面
            HashMap authParamsHashMap = ExtractIParamsAuthParam(reqParams, reqHeaders, true);
            authParamsJsonStr = Utils.paramsHashMapToJsonStr(authParamsHashMap, true);
        }

        //忽略重复参数的请求
        if(Config.REQ_SMART) {
            String reqUrlNoParam = reqUrl.split("\\?",2)[0];
            byte contentType = reqInfo.getContentType();

            //确定HASHMAP请求的key
            String reqUrlKey;
            if(Config.REQ_AUTH){
                //添加auth头相关的信息
                reqUrlKey = String.format("%s[type:%s][auth:%s]", reqUrlNoParam, contentType, authParamsJsonStr);
            }else {
                reqUrlKey = String.format("%s[type:%s]", reqUrlNoParam, contentType);
            }

            //格式化处理每个请求的参数, Burp默认处理的参数对包含Cookie值
            String reqParamsJsonStr;
            //额外处理 多层 Json格式的请求
            if(contentType == IRequestInfo.CONTENT_TYPE_JSON
                    && !Utils.isEmpty(reqBodyString)
                    && Utils.countStr(Utils.decodeUrl(reqBodyString),"{" ,2, true)
                    && Utils.isJson(Utils.decodeUrl(reqBodyString))){
                HashMap reqParamsMap = Utils.JsonParamsToHashMap(reqBodyString, false);
                ParamsHashMapAddIParams(reqParamsMap, reqParams);
                reqParamsJsonStr = Utils.paramsHashMapToJsonStr(reqParamsMap, false);
            }else if(Utils.paramValueHasJson(reqParams)){
                //如果有参数的值有Json格式,需要进一步进行处理
                Utils.showStdoutMsg(2, "[!] Parameter Value Has Json format, Need Depth Processing ...");
                reqParamsJsonStr = Utils.IParametersToJsonStrPlus(reqParams, false);
            }else {
                //通用的参数Json获取方案
                reqParamsJsonStr = Utils.IParametersToJsonStr(reqParams, false);
            }
            Boolean isUniq = Utils.isUniqReqInfo(Config.reqInfoHashMap, reqUrlKey, reqParamsJsonStr, false);
            if(!isUniq){
                Utils.showStderrMsg(1, String.format("[-] Ignored By Param Duplication: %s %s", reqUrlKey, reqParamsJsonStr));
                return null;
            }

            //记录请求的URL 到 reqKeyHASHMAP
            ReqKeyHashMap.put(Config.REQ_SMART_STR, reqUrlKey);

            //内存记录数量超过限制,清空 reqInfoHashMap
            if(Config.HASH_MAP_LIMIT <= Config.reqInfoHashMap.size()){
                Utils.showStdoutMsg(1, String.format("[-] Clear HashMap Content By Exceed Limit %s.", Config.HASH_MAP_LIMIT));
                Config.reqInfoHashMap.clear();
            }
        }


        //忽略完全重复的请求信息
        if(Config.REQ_HASH) {
            String reqUrlKey;
            if(Config.REQ_AUTH){
                //添加auth头相关的信息
                reqUrlKey = String.format("%s[auth:%s]", reqUrl, authParamsJsonStr);
            }else {
                reqUrlKey = String.format("%s", reqUrl);
            }
            //计算请求信息Hash
            String reqInfoHash = Utils.calcReqInfoHash(reqUrlKey, reqBodyBytes);
            //新增 输出url去重处理  记录请求URL和body对应hash
            if (Config.reqInfoHashSet.contains(reqInfoHash)) {
                Utils.showStderrMsg(1, String.format("[-] Ignored By URL&Body(md5): %s", reqInfoHash));
                return null;
            } else {
                Utils.showStdoutMsg(1, String.format("[+] Firstly REQ URL&Body(md5): %s", reqInfoHash));
                Config.reqInfoHashSet.add(reqInfoHash);
            }

            //记录请求的URL 到 reqKeyHASHMAP
            ReqKeyHashMap.put(Config.REQ_HASH_STR, reqInfoHash);

            //内存记录数量超过限制,清空 reqInfoHashSet
            if(Config.HASH_SET_LIMIT <= Config.reqInfoHashSet.size()){
                Utils.showStdoutMsg(1, String.format("[-] Clear HashSet Content By Exceed Limit %s.", Config.HASH_SET_LIMIT));
                Config.reqInfoHashSet.clear();
            }
        }

        //延迟转发
        Thread.sleep(Config.INTERVAL_TIME);

        Map<String, Object> resultMap;
        if(httpService.getProtocol().equals("https")){
            //修改 输出url去重处理
            resultMap = HttpsProxy(ReqKeyHashMap, reqUrl, reqHeaders, reqBodyBytes, Config.PROXY_HOST, Config.PROXY_PORT, Config.PROXY_USERNAME, Config.PROXY_PASSWORD);
        }else {
            //修改 输出url去重处理
            resultMap = HttpProxy(ReqKeyHashMap, reqUrl, reqHeaders, reqBodyBytes, Config.PROXY_HOST, Config.PROXY_PORT,Config.PROXY_USERNAME,Config.PROXY_PASSWORD);
        }

        putResultMapOuter(resultMap, requestResponse);
        return resultMap;
    }

    //感谢chen1sheng的pr，已经修改了我漏修复的https转发bug，并解决了header截断的bug。
    public static Map<String,Object> HttpsProxy(HashMap<String,String> ReqKeyHashMap, String url, List<String> headers,byte[] body, String proxy, int port,String username,String password){
    //public static Map<String,String> HttpsProxy(Set reqBodyHashSet, String url_body, String url, List<String> headers,byte[] body, String proxy, int port,String username,String password){
        Map<String,Object> mapResult = new HashMap<>();
        String status;
        String rspHeader = null;
        byte[] respBody = new byte[0];

        HttpsURLConnection urlConnection = null;
        PrintWriter out = null;
        BufferedReader reader = null;
        try {
            URL urlClient = new URL(url);
            SSLContext sslContext = SSLContext.getInstance("SSL");
            //指定信任https
            sslContext.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());
            //创建代理虽然是https也是Type.HTTP
            Proxy proxy1=new Proxy(Type.HTTP, new InetSocketAddress(proxy, port));
            //设置代理
            urlConnection = (HttpsURLConnection) urlClient.openConnection(proxy1);

            //设置账号密码
            SetProxyAuth(username, password, urlConnection);

            urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            urlConnection.setHostnameVerifier(new TrustAnyHostnameVerifier());

            //设置控制请求方法的Flag
            String methodFlag = "";
            //设置通用的请求属性
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
                urlConnection.setRequestProperty(header_key, header_value);
                //BurpExtender.stdout.println(header_key + ":" + header_value);
            }

            if (methodFlag.equals("GET")){
                //发送GET请求必须设置如下两行
                urlConnection.setDoOutput(false);
                urlConnection.setDoInput(true);

                //获取URLConnection对象的连接
                urlConnection.connect();
            }
            else if(methodFlag.equals("POST")){
                //发送POST请求必须设置如下两行
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);

                //获取URLConnection对象对应的输出流
                out = new PrintWriter(urlConnection.getOutputStream());
                if(body != null) {
                    //发送请求参数
                    out.print(new String(body));
                }
                //flush输出流的缓冲
                out.flush();
            }

            //忽略响应存储
            if(!Config.IGNORE_RESP){
                //读取URL的响应
                respBody = UtilsPlus.readBytesFromStream((urlConnection.getInputStream()));

                // 检查是否有Content-Encoding头，并且值为gzip
                String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                    respBody = UtilsPlus.gzipDecompress(respBody);
                }
            }

            //断开连接
            urlConnection.disconnect();

            //获取响应头
            rspHeader = UtilsPlus.getHeaderByHeaderFields(urlConnection.getHeaderFields());

            //BurpExtender.stdout.println("返回结果https：" + httpsConn.getResponseMessage());
            status = String.valueOf(urlConnection.getResponseCode());
            GUI.updateSuccessCount();
        } catch (Exception e) {
            //e.printStackTrace();
            respBody = e.getMessage().getBytes(StandardCharsets.UTF_8);
            Utils.showStderrMsg(1, "[!] First Times: " + e.getMessage());
            GUI.updateFailCount();

            //不记录错误响应的请求
            String cause = "Response Error";
            DeleteErrorKey(ReqKeyHashMap, cause);
        } finally {
            ErrorHandle(out, null, reader);
        }

        //再次获取状态码 // 影响服务器状态码的获取
        try {
            status = String.valueOf(urlConnection.getResponseCode());
        } catch (IOException e) {
            status = e.getMessage();
            Utils.showStderrMsg(1, "[!] Second Times: " + e.getMessage());
        }

        //修复rspHeader为空导致的空行
        if("".equals(rspHeader.trim())){
            rspHeader = "Failed to obtain the response header";
        }

        //不记录指定响应状态码的请求
        if(Utils.isEqualKeywords(Config.DEL_STATUS_REGX,status,false)){
            String cause = String.format("Status %s In %s", status, Config.DEL_STATUS_REGX);
            DeleteErrorKey(ReqKeyHashMap, cause);
        }

        //保存结果
        putResultMapInner(mapResult, status, rspHeader, respBody, proxy, port);
        return mapResult;
    }


    public static Map<String,Object> HttpProxy(HashMap<String,String> ReqKeyHashMap, String url,List<String> headers,byte[] body, String proxy, int port,String username,String password) {
        //public static Map<String,String> HttpProxy(Set reqBodyHashSet, String url_body,String url,List<String> headers,byte[] body, String proxy, int port,String username,String password) {
        Map<String,Object> mapResult = new HashMap<>();
        String status;
        String rspHeader = "";
        byte[] respBody = new byte[0];

        HttpURLConnection urlConnection = null;
        PrintWriter out = null;
        BufferedReader reader = null;
        try {
            URL urlClient = new URL(url);
            SSLContext sc = SSLContext.getInstance("SSL");
            //指定信任https
            sc.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());
            //创建代理
            Proxy proxy1=new Proxy(Type.HTTP, new InetSocketAddress(proxy, port));
            //设置代理
            urlConnection = (HttpURLConnection) urlClient.openConnection(proxy1);

            //设置账号密码
            SetProxyAuth(username, password, urlConnection);

            //设置控制请求方法的Flag
            String methodFlag = "";
            //设置通用的请求属性
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
                urlConnection.setRequestProperty(header_key, header_value);
                //BurpExtender.stdout.println(header_key + ":" + header_value);
            }

            if (methodFlag.equals("GET")){
                //发送GET请求必须设置如下两行
                urlConnection.setDoOutput(false);
                urlConnection.setDoInput(true);

                //获取URLConnection对象的连接
                urlConnection.connect();
            } else if(methodFlag.equals("POST")){
                //发送POST请求必须设置如下两行
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);

                //获取URLConnection对象对应的输出流
                out = new PrintWriter(urlConnection.getOutputStream());
                if(body != null) {
                    //发送请求参数
                    out.print(new String(body));
                }
                //flush输出流的缓冲
                out.flush();
            }

            //忽略响应存储
            if(!Config.IGNORE_RESP){
                //读取URL的响应
                respBody = UtilsPlus.readBytesFromStream((urlConnection.getInputStream()));

                // 检查是否有Content-Encoding头，并且值为gzip
                String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                    respBody = UtilsPlus.gzipDecompress(respBody);
                }
            }

            //断开连接
            urlConnection.disconnect();

            //获取响应头
            rspHeader = UtilsPlus.getHeaderByHeaderFields(urlConnection.getHeaderFields());

            //BurpExtender.stdout.println("返回结果http：" + httpConn.getResponseMessage());
            status = String.valueOf(urlConnection.getResponseCode());
            GUI.updateSuccessCount();
        } catch (Exception e) {
            //e.printStackTrace();
            respBody = e.getMessage().getBytes(StandardCharsets.UTF_8);
            Utils.showStderrMsg(1, "[!] First Times: " + e.getMessage());
            GUI.updateFailCount();

            //不记录错误响应的请求
            String cause = "Response Error";
            DeleteErrorKey(ReqKeyHashMap, cause);
        } finally {
            ErrorHandle(out, null, reader);
        }

        //再次获取状态码 // 影响服务器状态码的获取
        try {
            status = String.valueOf(urlConnection.getResponseCode());
        } catch (IOException e) {
            status = e.getMessage();
            Utils.showStderrMsg(1, "[!] Second Times: " + e.getMessage());
        }

        //修复rspHeader为空导致的空行
        if("".equals(rspHeader.trim())){
            rspHeader = "Failed to obtain the response header";
        }

        //不记录指定响应状态码的请求
        if(Utils.isEqualKeywords(Config.DEL_STATUS_REGX,status,false)){
            String cause = String.format("Status %s In %s", status, Config.DEL_STATUS_REGX);
            DeleteErrorKey(ReqKeyHashMap, cause);
        }

        //保存结果
        putResultMapInner(mapResult, status, rspHeader, respBody, proxy, port);
        return mapResult;
    }


    public static void SetProxyAuth(String username, String password, HttpURLConnection httpConn) {
        if(username != null && password != null && username.trim().length() > 0 && password.trim().length() > 0){
            String user_pass = String.format("%s:%s", username, password);
            String headerKey = "Proxy-Authorization";
            String headerValue = "Basic " + Base64.encode(user_pass.getBytes());
            Utils.showStdoutMsg(1, String.format("[*] Set [%s] Proxy-Authorization Data: [%s]", user_pass, headerValue));
            httpConn.setRequestProperty(headerKey, headerValue);
        }
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

    //报错数据处理
    public static void ErrorHandle(PrintWriter out, BufferedReader in, BufferedReader reader) {
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

    //不记录错误响应的请求
    private static void DeleteErrorKey(HashMap<String,String> reqKeyHashMap, String cause){
        if(Config.DEL_ERROR_KEY){
            if(Config.REQ_HASH){
                String reqKey = reqKeyHashMap.get(Config.REQ_HASH_STR);
                if(Config.reqInfoHashSet.contains(reqKey)){
                    Config.reqInfoHashSet.remove(reqKey);
                    Utils.showStderrMsg(1, String.format("[-] Cause [%s] So Remove Hashset Record: %s", cause, reqKey) );
                }
            }
            if(Config.REQ_SMART){
                String reqKey = reqKeyHashMap.get(Config.REQ_SMART_STR);
                if(Config.reqInfoHashMap.containsKey(reqKey)){
                    Config.reqInfoHashMap.remove(reqKey);
                    Utils.showStderrMsg(1, String.format("[-] Cause [%s] So Remove Hashmap Record: %s", cause, reqKey) );
                }
            }
        }
    }

    private static void putResultMapInner(Map<String, Object> mapResult, String status, String rspHeader, byte[] respBody, String proxy, int port) {
        mapResult.put("respStatus", status);
        mapResult.put("header", rspHeader);
        mapResult.put("respBody", respBody);
        mapResult.put("proxyHost", String.format("%s:%s", proxy, port));
    }

    private static void putResultMapOuter(Map<String, Object> mapResult, IHttpRequestResponse rawRequestResponse) {
        //添加对比结果
        byte[] rawResponse = rawRequestResponse.getResponse();
        boolean equalStatus = false;  //代理响应状态码是否和原始响应状态码相同
        boolean equalLength = false;  //代理响应长度是否和原始响应长度相同
        if (rawResponse.length > 0) {
            IResponseInfo rawRespInfo = BurpExtender.helpers.analyzeResponse(rawResponse);
            //获取原始响应状态码
            String respStatus = String.valueOf(rawRespInfo.getStatusCode());
            equalStatus = mapResult.get("respStatus").equals(respStatus);
            //获取原始响应体长度
            int bodyOffset = rawRespInfo.getBodyOffset();
            int respLength = UtilsPlus.getBodyBytes(rawResponse, bodyOffset).length;
            equalLength = ((byte[])mapResult.get("respBody")).length == respLength;
        }
        mapResult.put("equalStatus", equalStatus);
        mapResult.put("equalLength", equalLength);
    }
}