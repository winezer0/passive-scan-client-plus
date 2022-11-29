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
    public static Map<String,String> Proxy(IHttpRequestResponse requestResponse, Set reqBodyHashSet) throws InterruptedException{

        byte[] req = requestResponse.getRequest();
        String url = null;
        byte[] reqbody = null;
        List<String> headers = null;
        String body_hash = ""; //新增 输出url去重处理
        String url_body = ""; //新增 输出url去重处理

        IHttpService httpService = requestResponse.getHttpService();
        IRequestInfo reqInfo = BurpExtender.helpers.analyzeRequest(httpService,req);

        url = reqInfo.getUrl().toString();
        headers = reqInfo.getHeaders();

        //忽略无参数目标
        if(Config.REQ_PARAM){
            //判断是否存在参数
            List<IParameter> Parameters = reqInfo.getParameters();
            if(Parameters.size()<=0){
                BurpExtender.stdout.println(String.format("[-] REQ Param None Ignored: %s", url));
                return null;
            }
        }

        if(reqInfo.getMethod().equals("POST")){
            int bodyOffset = reqInfo.getBodyOffset();
            String body = null;
            try {
                body = new String(req, bodyOffset, req.length - bodyOffset, "UTF-8");
                reqbody = body.getBytes("UTF-8");

                if(Config.REQ_UNIQ){ body_hash = Utils.MD5(body);  }//新增 输出url去重处理

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if(Config.REQ_UNIQ) {
            //新增 输出url去重处理  记录请求URL和body对应hash
            url_body = url + "&" + body_hash;
            if (reqBodyHashSet.contains(url_body)) {
                BurpExtender.stdout.println(String.format("[-] REQ URL&Body(md5) Ignored: %s", url_body));
                return null;
            } else {
                BurpExtender.stdout.println(String.format("[+] REQ URL&Body(md5) Firstly: %s", url_body));
                reqBodyHashSet.add(url_body);
            }
        }

        Thread.sleep(Config.INTERVAL_TIME);
        if(httpService.getProtocol().equals("https")){
            //修改 输出url去重处理
            return HttpsProxy(reqBodyHashSet, url_body, url, headers, reqbody, Config.PROXY_HOST, Config.PROXY_PORT,Config.PROXY_USERNAME,Config.PROXY_PASSWORD);
        }else {
            //修改 输出url去重处理
            return HttpProxy(reqBodyHashSet, url_body, url, headers, reqbody, Config.PROXY_HOST, Config.PROXY_PORT,Config.PROXY_USERNAME,Config.PROXY_PASSWORD);
        }
    }

    //感谢chen1sheng的pr，已经修改了我漏修复的https转发bug，并解决了header截断的bug。
    public static Map<String,String> HttpsProxy(Set reqBodyHashSet, String url_body, String url, List<String> headers,byte[] body, String proxy, int port,String username,String password){
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
                BurpExtender.stdout.println(String.format("[*] Set [%s] Proxy-Authorization Data: [%s]", user_pass, headerValue));
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
            BurpExtender.stderr.println("[*] First Times: " + e.getMessage());
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
            BurpExtender.stderr.println("[*] Second Times: " + e.getMessage());
            //新增 不记录错误响应的请求
            if(Config.REQ_UNIQ) {
                if(reqBodyHashSet.contains(url_body)){
                    reqBodyHashSet.remove(url_body);//新增
                    BurpExtender.stderr.println(String.format("[!] Remove Conn: %s", url_body) );
                }
            }
        }

        mapResult.put("status",status);
        mapResult.put("header",rspHeader);
        mapResult.put("result",result);
        return mapResult;
    }

    public static Map<String,String> HttpProxy(Set reqBodyHashSet, String url_body,String url,List<String> headers,byte[] body, String proxy, int port,String username,String password) {
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
                BurpExtender.stdout.println(String.format("[*] Set [%s] Proxy-Authorization Data: [%s]", user_pass, headerValue));
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
            BurpExtender.stderr.println("[*] First Times: " + e.getMessage());
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
            BurpExtender.stderr.println("[*] Second Times: " + e.getMessage());
            //新增 不记录错误响应的请求
            if(Config.REQ_UNIQ) {
                if(reqBodyHashSet.contains(url_body)){
                    reqBodyHashSet.remove(url_body);//新增
                    BurpExtender.stderr.println(String.format("[!] Remove Conn: %s", url_body) );
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