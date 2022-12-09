package burp;

import java.util.HashMap;
import java.util.HashSet;

public class Config {
    public static HashMap<String, String> reqInfoHashMap = new HashMap();
    public static HashSet<String> reqInfoHashSet = new HashSet<>();
    public static Integer HASH_MAP_LIMIT; //限制reqInfoHashMap中最大记录的请求数量,
    public static Integer HASH_SET_LIMIT; //限制reqInfoHashSet中最大记录的请求数量

    public static boolean IS_RUNNING = false;
    public static boolean REQ_UNIQ = false;  //HASH去重模式
    public static boolean REQ_PARAM = false; //无参过滤模式
    public static boolean REQ_SMART = false; //参数去重模式
    public static boolean REQ_AUTH = false; //关注认证信息

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
    public static Integer INTERVAL_TIME; //从配置文件获取

    public static String TARGET_HOST_REGX; //从配置文件获取
    public static String BLACK_SUFFIX_REGX; //从配置文件获取
    public static String BLACK_HOST_REGX; //从配置文件获取
    public static String AUTH_INFO_STR; //从配置文件获取,去重时应该关注认证头信息字符串

    public static Boolean SELECTED_UNIQ; //从配置文件获取,HASH去重模式 按钮的默认设置 注：按钮变量可合并到参数
    public static Boolean SELECTED_PARAM; //从配置文件获取,过滤无参数 按钮的默认设置 注：按钮变量可合并到参数
    public static Boolean SELECTED_SMART; //从配置文件获取,参数去重模式 按钮的默认设置 注：按钮变量可合并到参数
    public static Boolean SELECTED_AUTH; //从配置文件获取,去重是否关注认证头信息 按钮的默认设置 注：按钮变量可合并到参数

    public static Boolean SHOW_DEBUG_MSG; //从配置文件获取,显示调试信息 //考虑将其增加按钮,但是很有可能发生异步输出错误.
    public static Boolean DEL_ERROR_KEY; //从配置文件获取,请求失败是否删除记录
}
