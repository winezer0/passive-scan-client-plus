package burp;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GUI implements ITab,IMessageEditorController {
    private final String tabName;

    private final JPanel contentPanel;
    private final JTextField tfHost;
    private final JTextField tfPort;
    private final JTextField tfTimeout;
    private final JTextField tfIntervalTime;
    private final JTextField tfUsername;
    private final JTextField tfPassword;
    private final JTextField tfTargetHost;
    private final JTextField tfBlackUrl;
    private final JTextField tfBlackSuffix;
    private final JToggleButton btnConn;
    private final JToggleButton btnHash;
    private final JToggleButton btnParam;
    private final JToggleButton btnSmart;
    private final JToggleButton btnAuth;
    private final JToggleButton btnIgnore;
    private final JButton btnClear;

    private final JPanel topPanel;
    private final JSplitPane splitPane;
    public static HttpLogTable logTable;
    public static IHttpRequestResponse currentlyDisplayedItem;
    public static JLabel lbRequestCount;
    public static JLabel lbSuccessCount;
    public static JLabel lbFailCount;

    private static JSplitPane OriginalMsgViewerPane;  //请求消息|响应消息 二合一 面板
    public static IMessageEditor originalRequestViewer;
    public static IMessageEditor originalResponseViewer;

    private static JSplitPane proxyMsgViewerPane;  //请求消息|响应消息 二合一 面板
    public static IMessageEditor proxyRequestViewer;
    public static IMessageEditor proxyResponseViewer;

    public GUI(IBurpExtenderCallbacks burpExtenderCallbacks, String tabName) {
        //设置插件名称
        this.tabName = tabName;

        contentPanel = new JPanel();
        //外边框设置
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        //添加一个四边各为5像素宽的空白边框。这通常用于增加组件与其相邻组件之间的间距，或者在组件边缘周围添加一些额外的空间。
        //内边距设置
        contentPanel.setLayout(new BorderLayout(0, 0));
        //设置没有额外间隙的 BorderLayout 布局管理器。 添加组件时，组件会紧密地排列在一起

        ////////////////////////////////////////////////////////////////////
        //topPanel start 开始设置配置文件部分面板
        ////////////////////////////////////////////////////////////////////
        topPanel = new JPanel();
        //把配置面板添加到主面板中
        contentPanel.add(topPanel,BorderLayout.NORTH);
        //自增配置面板内的行号
        int topPanelY = 0;

        //统一设置行高变量
        int LineHeight = 25;
        //统一设置行的元素的填充方式
        int LineFill = GridBagConstraints.NONE;
        //统一设置行内的元素的填充方式
        int LineInnerFill = GridBagConstraints.HORIZONTAL;
        //统一设置行内的元素的对齐方式
        int LineAnchor = GridBagConstraints.WEST;
        int LineInnerAnchor = GridBagConstraints.WEST;
        //设置文字列元素的宽度
        int lenText = 60;
        int lenInput = 100;
        int lenRight = 0;

        //设置 配置面板的 基本布局配置
        if(true){
            //GridBagLayout是一个灵活的布局管理器，它允许你创建复杂的网格布局，其中组件可以跨越多行或多列，并且可以有不同的尺寸和填充方式。
            GridBagLayout gridBagLayout = new GridBagLayout();
            //列数 columnWidths 设置GridBagLayout的列宽度。数组中的每个元素代表一个列的宽度。
            //gridBagLayout.columnWidths = new int[] {0};
            //各列占宽度比 | 设置列的权重
            gridBagLayout.columnWeights = new double[] {1.0D};
            //行数 rowHeights 设置 GridBagLayout的行高。
            gridBagLayout.rowHeights = new int[] {LineHeight, 0, LineHeight, LineHeight, LineHeight};   //设置四行
            //设置行的权重 | 各行占高度比
            gridBagLayout.rowWeights = new double[] {1.0D, 1.0D, 1.0D, 1.0D, 1.0D};  //设置四行

            //0.0D： 权重是0，不会接收任何额外的垂直空间。即使容器有额外的空间，这些空间也不会被分配
            //1.0D： 接收所有可用的额外空间。 如果有任何额外的垂直空间，它将被分配给第三行。
            //Double.MIN_VALUE：设置这样的权重基本上意味着不会接收任何额外的空间。

            topPanel.setLayout(gridBagLayout);
        }

        //设置 按钮 面板
        if(true){
            //按钮面板
            JPanel buttonPanel = new JPanel();
            //把按钮面板添加到配置面板中
            topPanel.add(buttonPanel, getGridBagConstraints(GridBagConstraints.NONE, GridBagConstraints.CENTER,5, 0, topPanelY));
            topPanelY += 1;

            //设置按钮面板的格式
            GridBagLayout gbl_panel = new GridBagLayout();
            //列数量和列宽| 列数和 innerX的最终值是一样的  列数不完善,暂时忽略
            gbl_panel.columnWidths = new int[] { lenInput, lenInput, lenInput, lenInput, lenInput, lenInput, lenInput};
            //列权重 | 列数不完善,暂时忽略
            gbl_panel.columnWeights = new double[] {1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D}; //6个按钮
            //行数量和行高 | 因为在一行,所以应该固定是0
            gbl_panel.rowHeights = new int[] {LineHeight};
            //行权重
            gbl_panel.rowWeights = new double[] {1.0D};
            buttonPanel.setLayout(gbl_panel);

            //设置 按钮 行的内容
            if(true){
                int buttonRight = 5;

                int innerX = 0; //内部列号 起始
                //增加URL HASH 去重开关
                btnHash = new JToggleButton("HASH");
                btnHash.setToolTipText("忽略转发HASH值相同的请求URL");
                btnHash.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnHash.isSelected();
                        boolean oldStatus = Config.REQ_HASH;

                        Config.REQ_HASH = isSelected;
                        btnHash.setSelected(isSelected);

                        boolean newStatus = Config.REQ_HASH;
                        //判断状态是否改变,改变了就输出
                        if(oldStatus != newStatus){
                            Utils.showStdoutMsg(1, String.format("[*] Click Button [%s]: %s --> %s", "HASH", oldStatus, newStatus));
                        }
                    }
                });
                //根据配置文件设置HASH按钮的默认选择行为
                if(Config.SELECTED_HASH){
                    btnHash.setSelected(true);
                }

                buttonPanel.add(btnHash, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;

                //增加无参数URL去除开关
                btnParam = new JToggleButton("PARAM");
                btnParam.setToolTipText("忽略转发没有参数的请求URL");
                btnParam.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnParam.isSelected();
                        boolean oldStatus = Config.REQ_PARAM;
                        Config.REQ_PARAM = isSelected;
                        btnParam.setSelected(isSelected);
                        //判断状态是否改变,改变了就输出
                        boolean newStatus = Config.REQ_PARAM;
                        if(oldStatus != newStatus) {
                            Utils.showStdoutMsg(1, String.format("[*] Click Button [%s]: %s --> %s", "PARAM", oldStatus, newStatus));
                        }
                    }
                });
                //根据配置文件设置PARAM按钮的默认选择行为
                if(Config.SELECTED_PARAM){
                    btnParam.setSelected(true);
                }

                buttonPanel.add(btnParam, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;

                //增加重复参数URL去除开关
                btnSmart = new JToggleButton("SMART");
                btnSmart.setToolTipText("忽略转发参数重复的请求URL");
                btnSmart.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnSmart.isSelected();
                        boolean oldStatus = Config.REQ_SMART;
                        Config.REQ_SMART = isSelected;
                        btnSmart.setSelected(isSelected);
                        boolean newStatus = Config.REQ_SMART;
                        //判断状态是否改变,改变了就输出
                        if(oldStatus != newStatus){
                            Utils.showStdoutMsg(1, String.format("[*] Click Button [%s]: %s --> %s", "SMART", oldStatus, newStatus));
                        }
                    }
                });
                //根据配置文件设置SMART按钮的默认选择行为
                if(Config.SELECTED_SMART){
                    btnSmart.setSelected(true);
                }
                buttonPanel.add(btnSmart, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;

                //增加去重时关注认证信息的开关
                btnAuth = new JToggleButton("AUTH");
                btnAuth.setToolTipText("转发URL完全相同但认证信息不同的请求");
                btnAuth.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnAuth.isSelected();
                        boolean oldStatus = Config.REQ_AUTH;
                        Config.REQ_AUTH = isSelected;
                        btnAuth.setSelected(isSelected);
                        boolean newStatus = Config.REQ_AUTH;
                        //判断状态是否改变,改变了就输出
                        if(oldStatus != newStatus){
                            Utils.showStdoutMsg(1, String.format("[*] Click Button [%s]: %s --> %s", "AUTH", oldStatus, newStatus));
                        }
                    }
                });
                //根据配置文件设置AUTH按钮的默认选择行为
                if(Config.SELECTED_AUTH){
                    btnAuth.setSelected(true);
                }
                buttonPanel.add(btnAuth, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;


                //增加显示转发响应结果的内容开关
                btnIgnore = new JToggleButton("IGNORE");
                btnIgnore.setToolTipText("忽略保存转发到代理服务器时的响应");
                btnIgnore.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnIgnore.isSelected();
                        boolean oldStatus = Config.IGNORE_RESP;
                        Config.IGNORE_RESP = isSelected;
                        btnIgnore.setSelected(isSelected);
                        boolean newStatus = Config.IGNORE_RESP;
                        //判断状态是否改变,改变了就输出
                        if(oldStatus != newStatus){
                            Utils.showStdoutMsg(1, String.format("[*] Click Button [%s]: %s --> %s", "IGNORE", oldStatus, newStatus));
                        }
                    }
                });
                //根据配置文件设置AUTH按钮的默认选择行为
                if(Config.IGNORE_RESP){
                    btnIgnore.setSelected(true);
                }
                buttonPanel.add(btnIgnore, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;


                // 增加运行按钮
                btnConn = new JToggleButton("Run");
                btnConn.setToolTipText("被动监听运行模式控制开关");
                btnConn.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent arg0) {
                        boolean isSelected = btnConn.isSelected();

                        if(isSelected){
                            btnConn.setText("Stop");
                            Config.IS_RUNNING = true;
                            Config.PROXY_HOST = tfHost.getText();
                            Config.PROXY_PORT = Integer.valueOf(tfPort.getText());
                            Config.PROXY_TIMEOUT = Integer.valueOf(tfTimeout.getText());
                            Config.PROXY_USERNAME = tfUsername.getText();
                            Config.PROXY_PASSWORD = tfPassword.getText();
                            Config.TARGET_HOST_REGX = tfTargetHost.getText();
                            Config.BLACK_URL_REGX = tfBlackUrl.getText();
                            Config.BLACK_SUFFIX_REGX = tfBlackSuffix.getText();
                            Config.INTERVAL_TIME = Integer.valueOf(tfIntervalTime.getText());
                            setAllEnabled(false);
                        }else{
                            btnConn.setText("Run");
                            Config.IS_RUNNING = false;
                            setAllEnabled(true);
                        }
                        btnConn.setSelected(isSelected);
                    }
                });

                buttonPanel.add(btnConn, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;

                //增加清除按钮
                btnClear = new JButton("Clear");
                btnClear.setToolTipText("清除所有请求转发记录");
                btnClear.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int n = JOptionPane.showConfirmDialog(null, "Are you sure you want to clear the data？", "Passive Scan Client prompt", JOptionPane.YES_NO_OPTION);
                        if(n == 0) {
                            Config.REQUEST_TOTAL = 0;
                            lbRequestCount.setText("0");
                            Config.SUCCESS_TOTAL = 0;
                            lbSuccessCount.setText("0");
                            Config.FAIL_TOTAL = 0;
                            lbFailCount.setText("0");
                            BurpExtender.log.clear();
                            logTable.getHttpLogTableModel().fireTableDataChanged();//通知模型更新
                            logTable.updateUI();//刷新表格

                            proxyRequestViewer.setMessage("".getBytes(),true);
                            originalRequestViewer.setMessage("".getBytes(),true);

                            proxyResponseViewer.setMessage("".getBytes(),false);
                            originalResponseViewer.setMessage("".getBytes(),false);
                            clearHashSet();  //新增URL去重
                        }
                    }
                });

                buttonPanel.add(btnClear, getGridBagConstraints(LineInnerFill,LineInnerAnchor, buttonRight,innerX,0));
                innerX += 1;
            }
        }

        //设置 分割边框 面板
        if(true){
            //按钮面板
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder(new LineBorder(Color.RED, 1)); //添加边框
            //把按钮面板添加到配置面板中
            topPanel.add(buttonPanel, getGridBagConstraints(GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER,0, 0, topPanelY));
            topPanelY += 1;

            //设置按钮面板的格式
            GridBagLayout gbl_panel = new GridBagLayout();
            gbl_panel.rowHeights = new int[] {0};
            gbl_panel.rowWeights = new double[] {1.0D};
            buttonPanel.setLayout(gbl_panel);
        }

        //设置 代理 面板
        if(true){
            //按钮面板
            JPanel proxyPanel = new JPanel();

            //把按钮面板添加到配置面板中
            topPanel.add(proxyPanel,getGridBagConstraints(LineFill, LineAnchor,0, 0, topPanelY));
            topPanelY += 1;

            //设置按钮面板的格式
            GridBagLayout gbl_panel = new GridBagLayout();

            //列数量和列宽| 列数和 innerX的最终值是一样的  列最后设计完才知道
            gbl_panel.columnWidths = new int[] {lenText, lenInput, lenText, lenInput, lenText, lenInput, lenText, lenInput, lenText, lenInput, lenText, lenInput};
            //列权重 | 列数最后设计完才知道
            gbl_panel.columnWeights = new double[] {1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D};
            //行数量和行高
            gbl_panel.rowHeights = new int[] {LineHeight};
            //行权重
            gbl_panel.rowWeights = new double[] {1.0D};
            proxyPanel.setLayout(gbl_panel);

            //设置 按钮 行的内容
            if(true){
                int innerX = 0; //内部列号 起始

                //HOST输入框
                JLabel lbHost = new JLabel("ProxyHost:");
                lbHost.setToolTipText("被转发的代理服务监听IP");
                proxyPanel.add(lbHost, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                // HOST输入框
                tfHost = new JTextField(10);
                tfHost.setText(Config.PROXY_HOST);
                proxyPanel.add(tfHost, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //Port输入框
                JLabel lbPort = new JLabel("ProxyPort:");
                lbPort.setToolTipText("被转发的代理服务监听端口");
                proxyPanel.add(lbPort, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //Port输入框
                tfPort = new JTextField(10);
                tfPort.setText(String.valueOf(Config.PROXY_PORT));
                proxyPanel.add(tfPort, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //用户名输入框
                JLabel lbUsername = new JLabel("ProxyUser:");
                lbUsername.setToolTipText("被转发的代理服务监听账号");
                proxyPanel.add(lbUsername, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //用户名输入框
                tfUsername = new JTextField(10);
                tfUsername.setText(Config.PROXY_USERNAME);
                proxyPanel.add(tfUsername, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //密码输入框
                JLabel lbPassword = new JLabel("ProxyPwd:");
                lbPassword.setToolTipText("被转发的代理服务监听密码");
                proxyPanel.add(lbPassword, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //密码输入框
                tfPassword = new JTextField(10);
                tfPassword.setText(Config.PROXY_PASSWORD);
                proxyPanel.add(tfPassword, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //超时输入框
                JLabel lbTimeout = new JLabel("Timeout:");
                lbTimeout.setToolTipText("请求转发超时时间");
                proxyPanel.add(lbTimeout, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //超时输入框
                tfTimeout = new JTextField(10);
                tfTimeout.setText(String.valueOf(Config.PROXY_TIMEOUT));
                proxyPanel.add(tfTimeout, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //增加间隔时间
                JLabel lbIntervalTime = new JLabel("Interval Time:");
                lbIntervalTime.setToolTipText("请求转发间隔时间");
                proxyPanel.add(lbIntervalTime, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //增加间隔时间
                tfIntervalTime = new JTextField(10);
                tfIntervalTime.setText(String.valueOf(Config.INTERVAL_TIME));
                proxyPanel.add(tfIntervalTime, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;
            }
        }

        //设置 过滤 面板
        if(true){
            JPanel filterPanel = new JPanel();
            //把过滤host面板添加到配置面板中
            topPanel.add(filterPanel, getGridBagConstraints(LineFill,LineAnchor, 0, 0, topPanelY));
            topPanelY += 1;

            //设置股过滤面板的格式
            GridBagLayout gbl_panel = new GridBagLayout();
            gbl_panel.columnWidths = new int[] {lenText, lenInput * 4, lenText, lenInput * 4}; //四个元素
            gbl_panel.columnWeights = new double[] {1.0D, 1.0D, 1.0D, 1.0D}; //四个元素
            gbl_panel.rowHeights = new int[] {LineHeight};
            gbl_panel.rowWeights = new double[] {1.0D};
            filterPanel.setLayout(gbl_panel);

            //设置过滤行的内容
            if(true){
                int innerX = 0;
                //新增黑名单主机控制
                JLabel lbBlackUrl = new JLabel("BlackUrls:");
                lbBlackUrl.setToolTipText("禁止转发的黑名单URL正则");
                filterPanel.add(lbBlackUrl, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                tfBlackUrl = new JTextField(10);
                tfBlackUrl.setText(Config.BLACK_URL_REGX);
                filterPanel.add(tfBlackUrl, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                //黑名单后缀处理
                JLabel lbBlackSuffix = new JLabel("BlackSuffix:");
                lbBlackSuffix.setToolTipText("禁止转发的黑名单后缀正则");
                filterPanel.add(lbBlackSuffix, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                tfBlackSuffix = new JTextField(10);
                tfBlackSuffix.setText(Config.BLACK_SUFFIX_REGX);
                filterPanel.add(tfBlackSuffix, getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;
            }
        }

        //设置 目标 面板
        if(true){
            //HOST面板和后缀面板
            JPanel targetPanel = new JPanel();
            topPanel.add(targetPanel, getGridBagConstraints(LineFill,LineAnchor, 0, 0, topPanelY));
            topPanelY += 1;

            //设置 目标 行 的格式
            GridBagLayout gbl_panel_1 = new GridBagLayout();
            gbl_panel_1.columnWidths = new int[] {lenText, lenInput * 4, lenText, lenText, lenText, lenText, lenText,lenText};
            gbl_panel_1.columnWeights = new double[] { 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D,0.0D};
            gbl_panel_1.rowHeights = new int[] {LineHeight};
            gbl_panel_1.rowWeights = new double[] {1.0D};
            targetPanel.setLayout(gbl_panel_1);

            //设置目标行的内容
            if (true){
                int innerX = 0;
                JLabel lbTargetHost = new JLabel("TargetHost:");
                lbTargetHost.setToolTipText("允许被转发的请求目标HOST正则");
                targetPanel.add(lbTargetHost,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                tfTargetHost = new JTextField(10);
                tfTargetHost.setText(Config.TARGET_HOST_REGX);
                targetPanel.add(tfTargetHost,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;


                // 转发url总数，默认0
                JLabel lbRequest = new JLabel("Total:");
                lbRequest.setToolTipText("监听转发请求总数");
                targetPanel.add(lbRequest,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                lbRequestCount = new JLabel("0");
                lbRequestCount.setForeground(new Color(0,0,255));
                targetPanel.add(lbRequestCount,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                // 转发成功url数，默认0
                JLabel lbSuccess = new JLabel("Success:");
                lbSuccess.setToolTipText("转发请求成功总数");
                targetPanel.add(lbSuccess,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));

                lbRequestCount.setForeground(new Color(0,0,255));
                innerX += 1;


                lbSuccessCount = new JLabel("0");
                lbSuccessCount.setForeground(new Color(0, 255, 0));
                targetPanel.add(lbSuccessCount,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;

                // 转发失败url数，默认0
                JLabel lbFail = new JLabel("Fail:");
                lbFail.setToolTipText("转发请求失败总数");
                targetPanel.add(lbFail,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;


                lbFailCount = new JLabel("0");
                lbFailCount.setForeground(new Color(255, 0, 0));
                targetPanel.add(lbFailCount,getGridBagConstraints(LineInnerFill,LineInnerAnchor, lenRight,innerX,0));
                innerX += 1;
            }

        }

        ////////////////////////////////////////////////////////////////////
        //topPanel end
        ////////////////////////////////////////////////////////////////////
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(0.5);
        //把响应信息面板加到主面板中
        contentPanel.add(splitPane, BorderLayout.CENTER);

        HttpLogTableModel model = new HttpLogTableModel();
        logTable = new HttpLogTable(model);
        //JTable表头排序,以下两种方法均存在问题，导致界面混乱。
        //方式一
        //TableRowSorter<HttpLogTableModel> tableRowSorter=new TableRowSorter<HttpLogTableModel>(model);
        //logTable.setRowSorter(tableRowSorter);
        //方式二
        //logTable.setAutoCreateRowSorter(true);

        JScrollPane jspLogTable = new JScrollPane(logTable);
        splitPane.setTopComponent(jspLogTable);

        //添加最后的响应信息面板
        JTabbedPane tabs = new JTabbedPane();
        proxyRequestViewer = BurpExtender.callbacks.createMessageEditor(this, false);
        proxyResponseViewer = BurpExtender.callbacks.createMessageEditor(this, false);

        originalRequestViewer = BurpExtender.callbacks.createMessageEditor(this, false);
        originalResponseViewer = BurpExtender.callbacks.createMessageEditor(this, false);

        //添加请求和响应信息面板到一个面板中
        proxyMsgViewerPane = new JSplitPane(1);
        proxyMsgViewerPane.setLeftComponent(proxyRequestViewer.getComponent());
        proxyMsgViewerPane.setRightComponent(proxyResponseViewer.getComponent());

        OriginalMsgViewerPane = new JSplitPane(1);
        OriginalMsgViewerPane.setLeftComponent(originalRequestViewer.getComponent());
        OriginalMsgViewerPane.setRightComponent(originalResponseViewer.getComponent());

        tabs.addTab("ProxyMsg", proxyMsgViewerPane);
        tabs.addTab("Original", OriginalMsgViewerPane);
        splitPane.setBottomComponent(tabs);

        burpExtenderCallbacks.customizeUiComponent(topPanel);
        burpExtenderCallbacks.customizeUiComponent(splitPane);
        burpExtenderCallbacks.customizeUiComponent(contentPanel);
    }

    @Override
    public String getTabCaption() {
        return this.tabName;
    }

    @Override
    public Component getUiComponent() {
        return this.getComponent();
    }

    private GridBagConstraints getGridBagConstraints(int fill, int anchor, int top, int left, int bottom, int right, int grid_x, int grid_y) {
        GridBagConstraints gbc = new GridBagConstraints();
        // fill属性用来处理 GridBagLayout 网格布局时子节点渲染的占位大小
        gbc.fill = fill;
        //组件对齐方式
        gbc.anchor = anchor;
        //内边距设置
        gbc.insets = new Insets(top, left, bottom, right); //insets 内边距 代表上、左、下、右四个方向的外部间距。
        //所在列
        gbc.gridx = grid_x; //组件应该放在哪个网格列
        //所在行
        gbc.gridy = grid_y; //组件应该放在哪个网格行
        return gbc;
    }

    //无内框 设置方案
    private GridBagConstraints getGridBagConstraints(int fill, int anchor, int right, int grid_x, int grid_y) {
        return getGridBagConstraints(fill,anchor,0,0,0,right,grid_x,grid_y);
    }

    public Component getComponent(){
        return contentPanel;
    }

    public IHttpService getHttpService() {
        return currentlyDisplayedItem.getHttpService();
    }

    public byte[] getRequest() {
        return currentlyDisplayedItem.getRequest();
    }

    public byte[] getResponse() {
        return currentlyDisplayedItem.getResponse();
    }

    public void setAllEnabled(boolean is){
        tfHost.setEnabled(is);
        tfPort.setEnabled(is);
        tfUsername.setEnabled(is);
        tfPassword.setEnabled(is);
        tfTimeout.setEnabled(is);
        tfTargetHost.setEnabled(is);
        tfBlackUrl.setEnabled(is);
        tfBlackSuffix.setEnabled(is);
        tfIntervalTime.setEnabled(is);
    }

    //更新失败计数
    public static void updateFailCount(){
        synchronized(Config.SUCCESS_TOTAL){
            Config.REQUEST_TOTAL++;
            Config.FAIL_TOTAL++;
            lbRequestCount.setText(String.valueOf(Config.REQUEST_TOTAL));
            lbFailCount.setText(String.valueOf(Config.FAIL_TOTAL));
        }
    }

    //更新成功计数
    public static void updateSuccessCount(){
        synchronized(Config.FAIL_TOTAL){
            Config.REQUEST_TOTAL++;
            Config.SUCCESS_TOTAL++;
            lbRequestCount.setText(String.valueOf(Config.REQUEST_TOTAL));
            lbSuccessCount.setText(String.valueOf(Config.SUCCESS_TOTAL));
        }
    }

    //新增URL去重
    private void clearHashSet(){
        int HashSetSizeBefore = Config.reqInfoHashSet.size();
        Config.reqInfoHashSet.clear();
        int HashSetSizeAfter = Config.reqInfoHashSet.size();
        Utils.showStdoutMsg(0, String.format("[*] Clear HashSet By Button, HashSet Size %s --> %s.",HashSetSizeBefore, HashSetSizeAfter));

        int HashMapSizeBefore = Config.reqInfoHashMap.size();
        Config.reqInfoHashMap.clear();
        int HashMapSizeAfter = Config.reqInfoHashMap.size();
        Utils.showStdoutMsg(0, String.format("[*] Clear HashSet By Button, HashMap Size %s --> %s.",HashMapSizeBefore, HashMapSizeAfter));
    }

    /**
     * 当左边极小时 设置请求体和响应体各占一半空间
     */
    public static void msgViewerAutoSetSplitCenter(JSplitPane msgViewerPane) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (msgViewerPane.getLeftComponent().getWidth() <= 20)
                    msgViewerPane.setDividerLocation(msgViewerPane.getParent().getWidth() / 2);
            }
        });
    }

    public static void proxyMsgViewerAutoSetSplitCenter(){
        msgViewerAutoSetSplitCenter(proxyMsgViewerPane);
    }

    public static void originalMsgViewerAutoSetSplitCenter() {
        msgViewerAutoSetSplitCenter(OriginalMsgViewerPane);
    }
}