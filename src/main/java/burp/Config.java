package burp;

import java.util.HashSet;
import java.util.Set;

public class Config {
    public static Set reqBodyHashSet = new HashSet<String>();

    public static boolean IS_RUNNING = false;
    public static boolean REQ_UNIQ = false;
    public static boolean REQ_PARAM = false;

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
    public static String DOMAIN_REGX; //从配置文件获取
    public static String SUFFIX_REGX; //从配置文件获取
    public static Integer INTERVAL_TIME; //从配置文件获取


    

}
