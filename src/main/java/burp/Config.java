package burp;

import java.util.HashMap;
import java.util.HashSet;

public class Config {
    public static HashMap<String, String> reqInfoHashMap = new HashMap();
    public static HashSet<String> reqInfoHashSet = new HashSet<>();
    public static Integer HASH_MAP_LIMIT; //限制reqInfoHashMap中最大记录的请求数量,
    public static Integer HASH_SET_LIMIT; //限制reqInfoHashSet中最大记录的请求数量

    public static boolean IS_RUNNING = false;
    public static boolean REQ_UNIQ = false;
    public static boolean REQ_PARAM = false;
    public static boolean REQ_SMART = false;

    public static Integer REQUEST_TOTAL = 0;
    public static Integer SUCCESS_TOTAL = 0;
    public static Integer FAIL_TOTAL = 0;

    public static String EXTENSION_NAME; //从配置文件获取
    public static String VERSION; //从配置文件获取
    public static String PROXY_HOST; //从配置文件获取
    public static Integer PROXY_PORT; //从配置文件获取
    public static String PROXY_USERNAME; //从配置文件获取
    public static String PROXY_PASSWORD; //从配置文件获取
    public static Integer PROXY_TIMEOUT; //从配置文件获取
    public static String TARGET_HOST_REGX; //从配置文件获取
    public static String BLACK_SUFFIX_REGX; //从配置文件获取
    public static String BLACK_HOST_REGX; //从配置文件获取
    public static Integer INTERVAL_TIME; //从配置文件获取
    public static Boolean SELECTED_UNIQ; //从配置文件获取
    public static Boolean SELECTED_PARAM; //从配置文件获取
    public static Boolean SELECTED_SMART; //从配置文件获取
    public static Boolean SHOW_DEBUG_MSG; //从配置文件获取
}
