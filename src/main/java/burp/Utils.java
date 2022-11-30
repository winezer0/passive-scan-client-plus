package burp;

import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class Utils {

    public static String MD5(String key) {
        //import java.security.MessageDigest;
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        try {
            byte[] btInput = key.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getBanner(){
        String bannerInfo =
                "[+] " + BurpExtender.extensionName + " is loaded\n"
                        + "[+] #####################################\n"
                        + "[+]    " + BurpExtender.extensionName + " v" + BurpExtender.version +"\n"
                        + "[+]    anthor: c0ny1\n"
                        + "[+]    github: http://github.com/c0ny1/passive-scan-client\n"
                        + "[+]    update: https://github.com/winezer0/passive-scan-client-plus\n"
                        + "[+] ####################################";
        return bannerInfo;
    }

    public static boolean isMatchDomain(String regx, String str){
        Pattern pat = Pattern.compile("([\\w]+[\\.]|)("+regx+")",Pattern.CASE_INSENSITIVE);//正则判断
        Matcher mc= pat.matcher(str);//条件匹配
        if(mc.find()){
            return true;
        }else{
            return false;
        }
    }

    //获取请求信息的HASH
    public static String getReqInfoHash(String req_url, byte[] req_body ) {
        String url_body = "";
        try {
            if(req_body == null){
                url_body = req_url;
            }else {
                url_body = req_url + "&" + Utils.MD5(Arrays.toString(req_body));
            }
        }catch (Exception exception){
            BurpExtender.stderr.println(String.format("[*] getReqInfoHash [%s] Occur Error [%s]", req_url, exception.getMessage()));
        }
        return url_body;
    }

    //获取请求信息的URL和参数JSON
    public static String getReqParamsJsonStr(List<IParameter> reqParams) {
        // 后续需要考虑无参数情况
        // 组合成参数json
        JSONObject reqParamsJson = new JSONObject();
        for (IParameter param:reqParams) {
            System.out.println(param.toString());
            //param.getValue(); 当前根据是否存在值，后续可考虑改成参数是否存在列表[false,true] 参数类型列表[None,int,string,bytes等]
            reqParamsJson.put(param.getName(), true);
        }

        //排序输出URL JSON
        String reqParamsJsonStr = JSON.toJSONString(reqParamsJson, SerializerFeature.MapSortField);
        //BurpExtender.stdout.println("reqParamsJson：" + reqParamsJsonStr );
        return reqParamsJsonStr;
    }


    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    //判断Json是否已经在HashMap中,是返回false，不是返回true
    public static Boolean isUniqReqInfo(HashMap<String,String> reqInfoHashMap, String reqUrl, String newReqParamsJsonStr) {
        // 判断全局HashMap里面有没有，有就重新放入
        //考虑不使用 StrUtil
        String oldReqParamsJsonStr = reqInfoHashMap.get(reqUrl);
        //BurpExtender.stdout.println("oldReqParamsJsonStr:" + oldReqParamsJsonStr);
        //BurpExtender.stdout.println("newReqParamsJsonStr:" + newReqParamsJsonStr);

        if(Utils.isEmpty(oldReqParamsJsonStr)){
            reqInfoHashMap.put(reqUrl, newReqParamsJsonStr);
            BurpExtender.stdout.println("[+] reqInfoHashMap Add By None:" + reqInfoHashMap.get(reqUrl));
            return true;
        }

        //如果新旧的JsonStr相同,直接忽略
        if(newReqParamsJsonStr.equals(oldReqParamsJsonStr)){
            return false;
        }

        //如果已经存在参数Json,需要解开Json进行对比
        JSONObject oldReqParamsJson = JSONObject.parseObject(oldReqParamsJsonStr);
        //如果旧的json内没有数据
        if(oldReqParamsJson == null){
            reqInfoHashMap.put(reqUrl, newReqParamsJsonStr);
            BurpExtender.stdout.println("[+] reqInfoHashMap Add By Null:" + reqInfoHashMap.get(reqUrl));
            return true;
        }

        // 是否存在新的参数
        Boolean hasNewParam = false;
        // 如果旧的json内有 旧的参数， {"aaa":"1", "bbb","2"}
        Map<String, String> map = JSONObject.parseObject(newReqParamsJsonStr, Map.class);
        for(Map.Entry<String, String> obj : map.entrySet()){
            // 旧的参数包含新的参数key，则跳过
            boolean containsKey = oldReqParamsJson.containsKey(obj.getKey());
            if(containsKey){
                continue;
            }
            // 否则放入json 放入新的参数
            hasNewParam = true;
            //oldReqParamsJson.put(obj.getKey(), obj.getValue());
            oldReqParamsJson.put(obj.getKey(), true);
        }

        //如果有新参数就重新整理HashMap
        if(hasNewParam){
            // 放入集合
            reqInfoHashMap.put(reqUrl, oldReqParamsJson.toJSONString());
            BurpExtender.stdout.println("[+] reqInfoHashMap Add By New:" + reqInfoHashMap.get(reqUrl));
            return true;
        }else {
            //BurpExtender.stdout.println("No New Param:" + reqInfoHashMap.get(reqUrl));
            return false;
        }
    }


    public static String getPathExtension(String path) {
        String extension="";

        if("/".equals(path)||"".equals(path)){
            return extension;
        }

        try {
            String[] pathContents = path.split("[\\\\/]");
            if(pathContents != null){
                int pathContentsLength = pathContents.length;
                String lastPart = pathContents[pathContentsLength-1];
                String[] lastPartContents = lastPart.split("\\.");
                if(lastPartContents != null && lastPartContents.length > 1){
                    int lastPartContentLength = lastPartContents.length;
                    //extension
                    extension = lastPartContents[lastPartContentLength -1];

                    /*
                    //name
                    String name = "";
                    for (int i = 0; i < lastPartContentLength; i++) {
                        // System.out.println("Last Part " + i + ": "+ lastPartContents[i]);
                        if(i < (lastPartContents.length -1)){
                            name += lastPartContents[i] ;
                            if(i < (lastPartContentLength -2)){
                                name += ".";
                            }
                        }
                    }
                    String filename = name + "." + extension;
                    System.out.println("Name: " + name);
                    System.out.println("Filename: " + filename); */
                }
            }
        }catch (Exception exception){
            BurpExtender.stderr.println(String.format("[*] GetPathExtension [%s] Occur Error [%s]", path, exception.getMessage()));
        }
        //System.out.println("Extension: " + extension);
        return extension;
    }

    public static boolean isMatchExtension(String regx, String path){
        String ext = getPathExtension(path);
        //无后缀情况全部放行
        if("".equalsIgnoreCase(ext)){
            return false;
        }else {
            //Pattern pat = Pattern.compile("([\\w]+[\\.]|)("+regx+")",Pattern.CASE_INSENSITIVE);//正则判断
            Pattern pat = Pattern.compile("^("+regx+")$",Pattern.CASE_INSENSITIVE);//正则判断
            Matcher mc= pat.matcher(ext);//条件匹配
            if(mc.find()){
                return true;
            }else{
                return false;
            }
        }
    }

    public static void updateSuccessCount(){
        synchronized(Config.FAIL_TOTAL){
            Config.REQUEST_TOTAL++;
            Config.SUCCESS_TOTAL++;
            GUI.lbRequestCount.setText(String.valueOf(Config.REQUEST_TOTAL));
            GUI.lbSuccessCount.setText(String.valueOf(Config.SUCCESS_TOTAL));
        }
    }

    public static void updateFailCount(){
        synchronized(Config.SUCCESS_TOTAL){
            Config.REQUEST_TOTAL++;
            Config.FAIL_TOTAL++;
            GUI.lbRequestCount.setText(String.valueOf(Config.REQUEST_TOTAL));
            GUI.lbFailCount.setText(String.valueOf(Config.FAIL_TOTAL));
        }
    }


}
