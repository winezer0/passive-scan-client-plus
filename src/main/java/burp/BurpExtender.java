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
        Config.DOMAIN_REGX = YamlReader.getInstance(callbacks).getString("DEFAULT_DOMAIN_REGX");
        Config.SUFFIX_REGX = YamlReader.getInstance(callbacks).getString("DEFAULT_SUFFIX_REGX");
        Config.INTERVAL_TIME = YamlReader.getInstance(callbacks).getInteger("DEFAULT_INTERVAL_TIME");
        Config.SELECTED_UNIQ = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_UNIQ");
        Config.SELECTED_PARAM = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_PARAM");
        Config.SELECTED_SMART = YamlReader.getInstance(callbacks).getBoolean("DEFAULT_SELECTED_SMART");

        this.version = Config.VERSION;
        this.extensionName= Config.EXTENSION_NAME;
        callbacks.setExtensionName(this.extensionName + " " + this.version);
        BurpExtender.this.gui = new GUI();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this);
                BurpExtender.this.callbacks.registerProxyListener(BurpExtender.this);
                stdout.println(Utils.getBanner());

                stdout.println(String.format("[*] INIT DEFAULT_EXTENSION_NAME: %s", Config.EXTENSION_NAME));
                stdout.println(String.format("[*] INIT DEFAULT_VERSION: %s", Config.VERSION));
                stdout.println(String.format("[*] INIT DEFAULT_PROXY_HOST: %s", Config.PROXY_HOST));
                stdout.println(String.format("[*] INIT DEFAULT_PROXY_PORT: %s", Config.PROXY_PORT));
                stdout.println(String.format("[*] INIT DEFAULT_PROXY_USERNAME: %s", Config.PROXY_USERNAME));
                stdout.println(String.format("[*] INIT DEFAULT_PROXY_PASSWORD: %s", Config.PROXY_PASSWORD));
                stdout.println(String.format("[*] INIT DEFAULT_PROXY_TIMEOUT: %s", Config.PROXY_TIMEOUT));
                stdout.println(String.format("[*] INIT DEFAULT_DOMAIN_REGX: %s", Config.DOMAIN_REGX));
                stdout.println(String.format("[*] INIT DEFAULT_SUFFIX_REGX: %s", Config.SUFFIX_REGX));
                stdout.println(String.format("[*] INIT DEFAULT_INTERVAL_TIME: %s", Config.INTERVAL_TIME));
                stdout.println(String.format("[*] INIT DEFAULT_SELECTED_UNIQ: %s", Config.SELECTED_UNIQ));
                stdout.println(String.format("[*] INIT DEFAULT_SELECTED_PARAM: %s", Config.SELECTED_PARAM));
                stdout.println(String.format("[*] INIT DEFAULT_SELECTED_SMART: %s", Config.SELECTED_SMART));
                stdout.println("[*] ####################################");
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
    // 实现右键 感谢原作者Conanjun
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
                                    //mapResult = HttpAndHttpsProxy.Proxy(message , Config.reqBodyHashSet);

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
    // 实现ITab
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
            IHttpRequestResponse reprsp = iInterceptedProxyMessage.getMessageInfo();
            IHttpService httpService = reprsp.getHttpService();
            String host = reprsp.getHttpService().getHost();

            //stdout.println(Config.DOMAIN_REGX);
            if(!Utils.isMatchDomain(Config.DOMAIN_REGX,host)){
                return;
            }

            //String  url = helpers.analyzeRequest(httpService,reprsp.getRequest()).getUrl().toString();
            //url = url.indexOf("?") > 0 ? url.substring(0, url.indexOf("?")) : url;
            String  path = helpers.analyzeRequest(httpService,reprsp.getRequest()).getUrl().getPath();
            if(Utils.isMatchExtension(Config.SUFFIX_REGX,path)){
                return;
            }

            final IHttpRequestResponse req_resp = iInterceptedProxyMessage.getMessageInfo();

            //final LogEntry logEntry = new LogEntry(1,callbacks.saveBuffersToTempFiles(iInterceptedProxyMessage.getMessageInfo()),helpers.analyzeRequest(resrsp).getUrl());

            // create a new log entry with the message details
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized(log) {
                        int row = log.size();
                        String method = helpers.analyzeRequest(req_resp).getMethod();
                        Map<String, String> mapResult = null;
                        try {
                            mapResult = HttpAndHttpsProxy.Proxy(req_resp); //增加重复元素过滤功能
                            //mapResult = HttpAndHttpsProxy.Proxy(req_resp , Config.reqBodyHashSet);

                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
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