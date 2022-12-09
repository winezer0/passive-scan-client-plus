package burp;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


public class BurpExtender implements IBurpExtender,ITab,IProxyListener, IContextMenuFactory {
    public static IBurpExtenderCallbacks callbacks;
    public static IExtensionHelpers helpers;
    public static PrintWriter stdout;
    public static PrintWriter stderr;
    public static GUI gui;
    public static final List<LogEntry> log = new ArrayList<LogEntry>();
    public static BurpExtender burpExtender;
    private ExecutorService executorService;

    public static String extensionName;
    public static String version;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.burpExtender = this;
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.stdout = new PrintWriter(callbacks.getStdout(),true);
        this.stderr = new PrintWriter(callbacks.getStderr(),true);
        callbacks.registerContextMenuFactory(this);//必须注册右键菜单Factory

        //读取配置文件
        Config.EXTENSION_NAME = YamlReader.getInstance(callbacks).getString("DEFAULT_EXTENSION_NAME");
        Config.VERSION = YamlReader.getInstance(callbacks).getString("DEFAULT_VERSION");
        Config.PROXY_HOST = YamlReader.getInstance(callbacks).getString("DEFAULT_PROXY_HOST");
        Config.PROXY_PORT = YamlReader.getInstance(callbacks).getInteger("DEFAULT_PROXY_PORT");
        Config.PROXY_USERNAME = YamlReader.getInstance(callbacks).getString("DEFAULT_PROXY_USERNAME");
        Config.PROXY_PASSWORD = YamlReader.getInstance(callbacks).getString("DEFAULT_PROXY_PASSWORD");
        Config.PROXY_TIMEOUT = YamlReader.getInstance(callbacks).getInteger("DEFAULT_PROXY_TIMEOUT");

        Config.TARGET_HOST_REGX = YamlReader.getInstance(callbacks).getString("DEFAULT_TARGET_HOST_REGX");
        Config.BLACK_HOST_REGX = YamlReader.getInstance(callbacks).getString("DEFAULT_BLACK_HOST_REGX");
        Config.BLACK_SUFFIX_REGX = YamlReader.getInstance(callbacks).getString("DEFAULT_BLACK_SUFFIX_REGX");

        Config.INTERVAL_TIME = YamlReader.getInstance(callbacks).getInteger("DEFAULT_INTERVAL_TIME");
        Config.SELECTED_UNIQ = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_UNIQ");
        Config.SELECTED_PARAM = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_PARAM");
        Config.SELECTED_SMART = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_SMART");

        Config.HASH_MAP_LIMIT = YamlReader.getInstance(callbacks).getInteger("DEFAULT_HASH_MAP_LIMIT");
        Config.HASH_SET_LIMIT = YamlReader.getInstance(callbacks).getInteger("DEFAULT_HASH_SET_LIMIT");

        Config.SHOW_DEBUG_MSG = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SHOW_DEBUG_MSG");
        Config.DEL_ERROR_KEY = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_DEL_ERROR_KEY");

        Config.SELECTED_AUTH = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_AUTH");
        Config.AUTH_INFO_STR = YamlReader.getInstance(callbacks).getString("DEFAULT_AUTH_INFO_STR");

        this.version = Config.VERSION;
        this.extensionName= Config.EXTENSION_NAME;
        callbacks.setExtensionName(this.extensionName + " " + this.version);
        BurpExtender.this.gui = new GUI();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
                BurpExtender.this.callbacks.registerProxyListener(BurpExtender.this);
                Utils.showStdoutMsgInfo(Utils.getBanner());
                Utils.showStdoutMsgInfo(String.format("[*] INIT EXTENSION_NAME: %s", Config.EXTENSION_NAME));
                Utils.showStdoutMsgInfo(String.format("[*] INIT VERSION: %s", Config.VERSION));
                Utils.showStdoutMsgInfo(String.format("[*] INIT PROXY_HOST: %s", Config.PROXY_HOST));
                Utils.showStdoutMsgInfo(String.format("[*] INIT PROXY_PORT: %s", Config.PROXY_PORT));
                Utils.showStdoutMsgInfo(String.format("[*] INIT PROXY_USERNAME: %s", Config.PROXY_USERNAME));
                Utils.showStdoutMsgInfo(String.format("[*] INIT PROXY_PASSWORD: %s", Config.PROXY_PASSWORD));
                Utils.showStdoutMsgInfo(String.format("[*] INIT PROXY_TIMEOUT: %s", Config.PROXY_TIMEOUT));
                Utils.showStdoutMsgInfo(String.format("[*] INIT INTERVAL_TIME: %s", Config.INTERVAL_TIME));
                Utils.showStdoutMsgInfo(String.format("[*] INIT SELECTED_UNIQ: %s", Config.SELECTED_UNIQ));
                Utils.showStdoutMsgInfo(String.format("[*] INIT SELECTED_PARAM: %s", Config.SELECTED_PARAM));
                Utils.showStdoutMsgInfo(String.format("[*] INIT SELECTED_SMART: %s", Config.SELECTED_SMART));
                Utils.showStdoutMsgInfo(String.format("[*] INIT HASH_MAP_LIMIT: %s", Config.HASH_MAP_LIMIT));
                Utils.showStdoutMsgInfo(String.format("[*] INIT HASH_SET_LIMIT: %s", Config.HASH_SET_LIMIT));
                Utils.showStdoutMsgInfo(String.format("[*] INIT TARGET_HOST_REGX: %s", Config.TARGET_HOST_REGX));
                Utils.showStdoutMsgInfo(String.format("[*] INIT BLACK_HOST_REGX: %s", Config.BLACK_HOST_REGX));
                Utils.showStdoutMsgInfo(String.format("[*] INIT BLACK_SUFFIX_REGX: %s", Config.BLACK_SUFFIX_REGX));
                Utils.showStdoutMsgInfo(String.format("[*] INIT SELECTED_AUTH: %s", Config.SELECTED_AUTH));
                Utils.showStdoutMsgInfo(String.format("[*] INIT AUTH_INFO_STR: %s", Config.AUTH_INFO_STR));
                Utils.showStdoutMsgInfo(String.format("[*] INIT DEL_ERROR_KEY: %s", Config.DEL_ERROR_KEY));
                Utils.showStdoutMsgInfo(String.format("[*] INIT SHOW_DEBUG_MSG: %s", Config.SHOW_DEBUG_MSG));
                Utils.showStdoutMsgInfo("[*] ####################################");
            }
        });


        executorService = Executors.newSingleThreadExecutor();
        //必须等插件界面显示完毕，重置JTable列宽才生效
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //按照比例显示列宽
                float[] columnWidthPercentage = {5.0f, 5.0f, 55.0f, 20.0f, 15.0f};
                int tW = GUI.logTable.getWidth();
                TableColumn column;
                TableColumnModel jTableColumnModel = GUI.logTable.getColumnModel();
                int cantCols = jTableColumnModel.getColumnCount();
                for (int i = 0; i < cantCols; i++) {
                    column = jTableColumnModel.getColumn(i);
                    int pWidth = Math.round(columnWidthPercentage[i] * tW);
                    column.setPreferredWidth(pWidth);
                }
            }
        });
    }

    //callbacks.registerContextMenuFactory(this);//必须注册右键菜单Factory
    //实现右键 感谢原作者Conanjun
    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        final IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        JMenuItem i1 = new JMenuItem(String.format("Send to %s", Config.EXTENSION_NAME));
        i1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final IHttpRequestResponse message : messages) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (log) {
                                int row = log.size();
                                String method = helpers.analyzeRequest(message).getMethod();
                                byte[] req = message.getRequest();
                                String req_str = new String(req);
                                //向代理转发请求
                                Map<String, String> mapResult = null;

                                try {
                                    mapResult = HttpAndHttpsProxy.Proxy(message);
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                }

                                log.add(new LogEntry(row + 1,
                                        callbacks.saveBuffersToTempFiles(message), helpers.analyzeRequest(message).getUrl(),
                                        method,
                                        mapResult)
                                );
                                GUI.logTable.getHttpLogTableModel().fireTableRowsInserted(row, row);
                            }
                        }
                    });
                }
            }
        });

        return Arrays.asList(i1);
    }


    //
    //实现ITab
    //

    @Override
    public String getTabCaption() {
        return this.extensionName;
    }

    @Override
    public Component getUiComponent() {
        return gui.getComponent();
    }

    public void processProxyMessage(boolean messageIsRequest, final IInterceptedProxyMessage iInterceptedProxyMessage) {
        if (!messageIsRequest && Config.IS_RUNNING) {
            IHttpRequestResponse rep_rsp = iInterceptedProxyMessage.getMessageInfo();
            IHttpService httpService = rep_rsp.getHttpService();
            String host = rep_rsp.getHttpService().getHost();

            //白名单域名匹配
            if(!Utils.isMatchTargetHost(Config.TARGET_HOST_REGX, host, true)){
                Utils.showStdoutMsgDebug(String.format("[-] MatchTargetHost HOST:[%s] NOT Match Regex:[%s]", host, Config.TARGET_HOST_REGX));
                return;
            } else {
                Utils.showStdoutMsgDebug(String.format("[*] MatchTargetHost HOST:[%s] Match Regex:[%s]", host, Config.TARGET_HOST_REGX));
            }

            //黑名单域名匹配
            if(Utils.isMatchBlackHost(Config.BLACK_HOST_REGX, host, false)){
                Utils.showStdoutMsgDebug(String.format("[-] MatchBlackHost HOST:[%s] Match Regex:[%s]", host , Config.BLACK_HOST_REGX));
                return;
            }else {
                Utils.showStdoutMsgDebug(String.format("[*] MatchBlackHost HOST:[%s] NOT Match Regex:[%s]", host , Config.BLACK_HOST_REGX));
            }

            //黑名单后缀匹配
            String path = helpers.analyzeRequest(httpService,rep_rsp.getRequest()).getUrl().getPath();
            if(Utils.isMatchBlackSuffix(Config.BLACK_SUFFIX_REGX, path, false)){
                Utils.showStdoutMsgDebug(String.format("[-] MatchBlackSuffix PATH:[%s] Match Regex:[%s]", path , Config.BLACK_SUFFIX_REGX));
                return;
            }else {
                Utils.showStdoutMsgDebug(String.format("[*] MatchBlackSuffix PATH:[%s] NOT Match Regex:[%s]", path , Config.BLACK_SUFFIX_REGX));
            }

            final IHttpRequestResponse req_resp = iInterceptedProxyMessage.getMessageInfo();

            //final LogEntry logEntry = new LogEntry(1,callbacks.saveBuffersToTempFiles(iInterceptedProxyMessage.getMessageInfo()),helpers.analyzeRequest(resrsp).getUrl());

            //create a new log entry with the message details
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized(log) {
                        int row = log.size();
                        String method = helpers.analyzeRequest(req_resp).getMethod();
                        Map<String, String> mapResult = null;
                        try {
                            mapResult = HttpAndHttpsProxy.Proxy(req_resp);
                        } catch (InterruptedException e) {
                            //TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        //log.add(new LogEntry(iInterceptedProxyMessage.getMessageReference(),
                        log.add(new LogEntry(row + 1,
                                callbacks.saveBuffersToTempFiles(req_resp), helpers.analyzeRequest(req_resp).getUrl(),
                                method,
                                mapResult)
                        );
                        GUI.logTable.getHttpLogTableModel().fireTableRowsInserted(row, row);
                    }
                }
            });
        }
    }
}