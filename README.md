# Passive Scan Client | Burp被动扫描流量转发插件

## 0x00 原始项目

https://github.com/c0ny1/passive-scan-client

## 0x01 插件简介

```
Q1: 将浏览器代理到被动扫描器上，访问网站变慢，甚至有时被封ip，这该怎么办？
Q2: 需要人工渗透的同时后台进行被动扫描，到底是代理到burp还是被动扫描器？
Q3: ......
```

该插件正是为了解决该问题，将`正常访问网站的流量`与`提交给被动扫描器的流量`分开，互不影响。

![流程图](./doc/process.png)



## 0x02 功能介绍(本分支)

```
1、右键转发到PSC进行扫描
	1、右键转发功能不需要开启[run]按钮，[run]是开启Proxy流量自动转发扫描.
	2、右键转发功能能够绕过白名单HOST、黑名单URL和黑名单后缀过滤

2、支持默认启动配置。
	1、优先从插件所在目录读取psc.config.yml文件
	2、不存在时,从jar包内部读取psc.config.yml文件
	3、默认功能都是关闭的,机器性能足够建议开启AUTH、SMART功能

3、使用多种算法实现请求去重，不转发相同流量. 
    1、【HASH】按钮 实现 (URL及参数)完全相同请求去重(记录请求hashset去重)
    2、【PARAM】按钮 实现 (URL及参数)支持忽略无参数请求(由burp内置sdk解析实现)
    3、【SMART】按钮　实现　(URL及参数)去重相同参数的请求(由burp内置sdk解析实现)
        支持Json嵌套请求体自动解码(json递归处理)
        支持Json格式参数值的自动解码(由burp内置sdk解析实现)
    4、【AUTH】按钮　实现　支持根据不同的认证信息作为转发凭证(Cookie、token不同时，会作为不同请求)
	
4、新增黑名单HOST正则过滤。白名单域名匹配>>黑名单URL匹配>>黑名单后缀匹配

```



## 0x03 顽固问题

```
对于jdk17启动的burpsuite,依旧无法运行本程序，原因未知，请使用jdk8-jdk15
jdk11启动burpsuite 2022.6.1  可以运行PSC
jdk12启动burpsuite 2022.11.1  可以运行PSC
jdk17启动burpsuite 2022.11.1  不可运行PSC

问题详情：https://github.com/c0ny1/passive-scan-client/issues
```



## 0x04 下载编译

```
手动编译： mvn package
发布版本： release
```

## 0x05 插件演示

可以通过插件将流量转发到各种被动式扫描器中，这里我选`xray`来演示.

![动图演示](./doc/show.gif)



## 0x06 一些被动式漏洞扫描器

* [GourdScanV2](https://github.com/ysrc/GourdScanV2)  由ysrc出品的基于sqlmapapi的被动式漏洞扫描器
* [xray](https://github.com/chaitin/xray) 由长亭科技出品的一款被动式漏洞扫描器
* [w13scan](https://github.com/boy-hack/w13scan) Passive Security Scanner (被动安全扫描器)
* [Fox-scan](https://github.com/fengxuangit/Fox-scan) 基于sqlmapapi的主动和被动资源发现的漏洞扫描工具
* [SQLiScanner](https://github.com/0xbug/SQLiScanner) 一款基于sqlmapapi和Charles的被动SQL注入漏洞扫描工具
* [sqli-hunter](https://github.com/zt2/sqli-hunter) 基于sqlmapapi，ruby编写的漏洞代理型检测工具
* [passive_scan](https://github.com/netxfly/passive_scan) 基于http代理的web漏洞扫描器的实现

## 0x07 NEED STAR And ISSUE

```
1、右上角点击Star支持更新.
2、ISSUE或NOVASEC提更新需求.
```

![NOVASEC](doc/NOVASEC.jpg)