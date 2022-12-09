package burp;

import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class Utils {

    private static final Object FLAG_EXIST = "1";

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

    public static void showStdoutMsgDebug(String msg){
        if(Config.SHOW_DEBUG_MSG) {
            BurpExtender.stdout.println(msg);
        }
    }

    public static void showStdoutMsgInfo(String msg){
        BurpExtender.stdout.println(msg);
    }

    public static void showStderrMsgDebug(String msg){
        if(Config.SHOW_DEBUG_MSG) {
            BurpExtender.stderr.println(msg);
        }
    }

    public static void showStderrMsgInfo(String msg){
        BurpExtender.stderr.println(msg);
    }

    //域名匹配
    public static boolean isMatchTargetHost(String regx, String str, Boolean NoRegxValue){
        //如果没有正在表达式,的情况下返回指定值 NoRegxValue
        if (regx.trim().length() == 0){
            return NoRegxValue;
        }

        Pattern pat = Pattern.compile("^.*("+regx+").*$",Pattern.CASE_INSENSITIVE);//正则判断
        Matcher mc= pat.matcher(str);//条件匹配
        if(mc.find()){
            return true;
        }else{
            return false;
        }
    }

    //域名匹配
    public static boolean isMatchBlackHost(String regx, String str, Boolean NoRegxValue){
        //如果没有正在表达式,的情况下返回指定值 NoRegxValue
        if (regx.trim().length() == 0){
            return NoRegxValue;
        }

        Pattern pat = Pattern.compile("^.*("+regx+")$",Pattern.CASE_INSENSITIVE);//正则判断
        Matcher mc= pat.matcher(str);//条件匹配
        if(mc.find()){
            return true;
        }else{
            return false;
        }
    }

    //获取请求路径的扩展名
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
                }
            }
        }catch (Exception exception){
            Utils.showStderrMsgInfo(String.format("[*] GetPathExtension [%s] Occur Error [%s]", path, exception.getMessage()));
        }
        //BurpExtender.out.println("Extension: " + extension);
        return extension;
    }

    //后缀匹配
    public static boolean isMatchBlackSuffix(String regx, String path, Boolean NoRegxValue){
        //如果没有正在表达式,的情况下返回指定值 NoRegxValue
        if (regx.trim().length() == 0){
            return NoRegxValue;
        }

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

    //判断字符串是否为空
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
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
            Utils.showStderrMsgInfo(String.format("[*] getReqInfoHash [%s] Occur Error [%s]", req_url, exception.getMessage()));
        }
        return url_body;
    }

    //获取所有请求信息的URL和参数JSON
    public static String getReqCommonParamsJsonStr(List<IParameter> reqParams) {
        // 后续需要考虑无参数情况
        // 组合成参数json
        JSONObject reqParamsJson = new JSONObject();
        for (IParameter param:reqParams) {
            //param.getValue(); 当前根据是否存在值，后续可考虑改成参数是否存在列表[false,true] 参数类型列表[None,int,string,bytes等]
            reqParamsJson.put(param.getName(), true);
        }

        //排序输出URL JSON
        String reqParamsJsonStr = JSON.toJSONString(reqParamsJson, SerializerFeature.MapSortField);
        //BurpExtender.stdout.println("reqParamsJson：" + reqParamsJsonStr );
        return reqParamsJsonStr;
    }

    //判断Json是否是不重复的(不存在与HashMap中),独特返回true,重复返回false
    public static Boolean isUniqReqInfo(HashMap<String,String> reqInfoHashMap, String reqUrl, String newReqParamsJsonStr) {
        // 判断全局HashMap里面是否已经存在URL对应的参数字符串，有就合并新旧参数字符串，没有就直接存入
        String oldReqParamsJsonStr = reqInfoHashMap.get(reqUrl);
        showStderrMsgDebug("[*] Old ReqParamsJsonStr:" + oldReqParamsJsonStr);
        showStderrMsgDebug("[*] New ReqParamsJsonStr:" + newReqParamsJsonStr);

        // 不存在历史参数列表，直接存入,返回false
        if(Utils.isEmpty(oldReqParamsJsonStr)){
            reqInfoHashMap.put(reqUrl, newReqParamsJsonStr);
            Utils.showStdoutMsgDebug("[+] reqInfoHashMap Add By None:" + reqInfoHashMap.get(reqUrl));
            return true;
        }

        // 如果新旧的参数JsonStr相同,直接忽略,返回false
        if(newReqParamsJsonStr.equals(oldReqParamsJsonStr)){
            return false;
        }

        // 如果对应请求目标已经存在参数Json,且不完全相同,需要解开参数JsonStr进行对比
        JSONObject oldReqParamsJsonObj = JSONObject.parseObject(oldReqParamsJsonStr);

        // 如果旧的JsonStr内没有数据,就直接存入新的参数JsonStr
        if(oldReqParamsJsonObj == null){
            reqInfoHashMap.put(reqUrl, newReqParamsJsonStr);
            Utils.showStdoutMsgDebug("[+] reqInfoHashMap Add By Null:" + reqInfoHashMap.get(reqUrl));
            return true;
        }

        // hasNewParam 记录是否存在新的参数
        Boolean hasNewParam = false;
        // 如果旧的json内有 旧的参数， {"aaa":"1", "bbb","2"}
        Map<String, String> newReqParamsJsonMap = JSONObject.parseObject(newReqParamsJsonStr, Map.class);
        for(Map.Entry<String, String> newReqParamEntry : newReqParamsJsonMap.entrySet()){
            // 旧的参数包含新的参数key，则跳过
            if(oldReqParamsJsonObj.containsKey(newReqParamEntry.getKey())){
                continue;
            }
            // 往旧的Json参数对象 存入 新的参数
            oldReqParamsJsonObj.put(newReqParamEntry.getKey(), true);
            hasNewParam = true;
        }

        //如果有新参数加入就重新整理HashMap
        if(hasNewParam){
            // 放入集合
            reqInfoHashMap.put(reqUrl, oldReqParamsJsonObj.toJSONString());
            Utils.showStdoutMsgDebug("[+] reqInfoHashMap Add By New:" + reqInfoHashMap.get(reqUrl));
            return true;
        }else {
            return false;
        }
    }

    //根据请求参数:值键值对,获取参数对应的参数Json
    public static String getReqParamsMapJsonStr(HashMap<String,String> reqParams) {
        // 组合成参数json
        JSONObject reqParamsJson = new JSONObject();

        for(String key : reqParams.keySet()) {
            //BurpExtender.stdout.println(String.format("key:%s,vaule:%s", key, reqParams.get(key)));
            reqParamsJson.put(key, reqParams.get(key));
        }

        //排序输出URL JSON
        String reqParamsJsonStr = JSON.toJSONString(reqParamsJson, SerializerFeature.MapSortField);
        //BurpExtender.stdout.println("reqParamsJson：" + reqParamsJsonStr );
        return reqParamsJsonStr;
    }

    //判断字符串是否是Json格式
    public static boolean isJson(Object obj) {
        try{
            String str = obj.toString().trim();
            if (str.charAt(0) == '{' && str.charAt(str.length() - 1) == '}') {
                JSONObject.parseObject(str);
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    //处理JSon格式的参数字符串
    public static HashMap handleJsonParamsStr(String ParamsStr) {
        HashMap paramHashMap = new HashMap<>();
        Map<String, String> map = JSONObject.parseObject(ParamsStr, Map.class);
        for(Map.Entry<String, String> obj : map.entrySet()){
            //BurpExtender.stdout.println(String.format("handleJsonParamsStr: %s %s", obj.getKey(), obj.getValue()));
            String tempKey = String.valueOf(obj.getKey());
            String tempValue = String.valueOf(obj.getValue());
            if(isJson(tempValue)){
                //BurpExtender.stdout.println(String.format("参数值 %s 是Json格式的,需要进一步处理", tempValue));
                HashMap subHashMap = handleSubJsonParamsStr(tempKey,tempValue);
                paramHashMap.putAll(subHashMap);
            }else {
                paramHashMap.put(tempKey, FLAG_EXIST);
            }
        }
        return paramHashMap;
    }

    //递归处理Json内的子Json
    public static HashMap handleSubJsonParamsStr(String prefix, String subParamsStr){
        HashMap subParamHashMap = new HashMap();
        Map<String, String> subMap = JSONObject.parseObject(subParamsStr, Map.class);
        for(Map.Entry<String, String> subObj : subMap.entrySet()) {
            //BurpExtender.stdout.println(String.format("SubHandleJsonParamsStr:%s %s %s", prefix, subObj.getKey(), subObj.getValue()));
            String tempKey = String.format("%s.%s", prefix, subObj.getKey());
            String tempValue = String.valueOf(subObj.getValue());
            if(isJson(tempValue)){
                HashMap subSubJsonParamsStr = handleSubJsonParamsStr(tempKey, String.valueOf(subObj.getValue()));
                subParamHashMap.putAll(subSubJsonParamsStr);
            }
            else {
                //subParamHashMap.put(tempKey, tempValue);
                subParamHashMap.put(tempKey, FLAG_EXIST);
            }
        }
        return subParamHashMap;
    }

    //计算字符串内是否至少包含limit个指定字符 //只需要处理多层级别的Json就可以了,单层的用内置方法即可
    public static boolean countStr(String longStr, String mixStr, int limit) {
        int count = 0;
        int index = 0;
        while((index = longStr.indexOf(mixStr,index))!= -1){
            index = index + mixStr.length();
            count++;
            if(count >= limit){
                return true;
            }
        }
        return false;
    }



}
