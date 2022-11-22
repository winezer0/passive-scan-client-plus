# Passive Scan Client | Burp被动扫描流量转发插件



## 0x00 更新记录

Jar包下载

```
release文件夹
```

未解决问题

```
对于jdk17启动的burpsuite,依旧无法运行本程序.
jdk11启动burpsuite 2022.6.1  可以运行PSC
jdk12启动burpsuite 2022.11.1  可以运行PSC
jdk17启动burpsuite 2022.11.1  不可运行PSC
```

v0.4.1 合并更新

```
1、基于c0ny1的passive-scan-client-0.3.0合并更新Pull #27 #21 #22.  详情: https://github.com/c0ny1/passive-scan-client/pulls.
```

v0.4.2 修复新增

```
1、参考#34 修复代理用户名密码为空时判断BUG.  详情: https://github.com/c0ny1/passive-scan-client/pulls.

2、对所有请求基于URL及Body hash进行hash记录，当下一次遇到相同请求时忽略请求报文,  注：点击clear按钮可清空hashset. 
```

v0.4.3 请求去重

```
1、优化变量名称

2、添加ReqUinq 框，仅当ReqUinq 内容设置为 true 时，对请求基于URL及Body hash进行hash记录。注: 不会记录40X、50X、访问拒绝等响应状态码。 
```

v0.4.4 请求去重

```
1、修改ReqUinq框为UNIQ按钮，并设置默认关闭请求去重功能
```

v0.4.5 无参数请求过滤

```
1、增加PARAM框，支持过滤没有参数的请求，默认关闭
```
![v0.4.5](./doc/v0.4.5.png)



## 0x01 插件简介

```
Q1: 将浏览器代理到被动扫描器上，访问网站变慢，甚至有时被封ip，这该怎么办？
Q2: 需要人工渗透的同时后台进行被动扫描，到底是代理到burp还是被动扫描器？
Q3: ......
```

该插件正是为了解决该问题，将`正常访问网站的流量`与`提交给被动扫描器的流量`分开，互不影响。

![流程图](./doc/process.png)

## 0x02 插件编译

```
mvn package
```

## 0x03 插件演示

可以通过插件将流量转发到各种被动式扫描器中，这里我选`xray`来演示.

![动图演示](./doc/show.gif)

[Conanjun](https://github.com/Conanjun/passive-scan-client-and-sendto/commits?author=Conanjun)师傅的项目[Passive Scan Client and Sendto](https://github.com/Conanjun/passive-scan-client-and-sendto)，增加了右键手动转发的菜单，拓展了插件的灵活性，已将该功能添加到本项目中。

![image-20220511142914622](./doc/image-20220511142914622.png)

## 0x04 一些被动式漏洞扫描器

* [GourdScanV2](https://github.com/ysrc/GourdScanV2)  由ysrc出品的基于sqlmapapi的被动式漏洞扫描器
* [xray](https://github.com/chaitin/xray) 由长亭科技出品的一款被动式漏洞扫描器
* [w13scan](https://github.com/boy-hack/w13scan) Passive Security Scanner (被动安全扫描器)
* [Fox-scan](https://github.com/fengxuangit/Fox-scan) 基于sqlmapapi的主动和被动资源发现的漏洞扫描工具
* [SQLiScanner](https://github.com/0xbug/SQLiScanner) 一款基于sqlmapapi和Charles的被动SQL注入漏洞扫描工具
* [sqli-hunter](https://github.com/zt2/sqli-hunter) 基于sqlmapapi，ruby编写的漏洞代理型检测工具
* [passive_scan](https://github.com/netxfly/passive_scan) 基于http代理的web漏洞扫描器的实现