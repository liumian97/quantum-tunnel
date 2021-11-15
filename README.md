# quantum-tunnel

1. `QuantumTunnel`，量子隧道，是一款简单、强大、可扩展性高的内网穿透工具；
2. 高性能、兼容性好，底层基于Netty实现，理论上可代理所有应用层协议；
3. 流量转发灵活，支持端口路由和协议路由两种流量路由方式；
4. 使用简单，两行命令即可快速搭建内网穿透服务；
5. 安全性高，客户端可限制代理的服务器地址和端口；
6. 支持点对点内网穿透 **（开发中）**。

## 介绍
QuantumTunnel，量子隧道，名字来源于量子纠缠现象。 
> 两个处于量子纠缠的粒子，无论处于多么远的距离，当其中一个粒子状态改变时，另外一个粒子也会做出相应的改变。

QuantumTunnel也取意于此，希望把公网发出来的请求，完整的同步到内网，就像在内网发出的请求，打破网络的限制。

## 体验使用
1. 下载内网穿透客户端：[v1.0.0正式版本客户端](https://gitee.com/liumian/quantum-tunnel/attach_files/881457/download/quantum-tunnel-client.jar)
2. 执行启动命令：
```shell 
java -jar client/target/quantum-tunnel-client.jar -network_id localTest -proxy_server_host qt.liumian.top  -proxy_server_port 8880
```
3. 发起一个内网穿透请求：
```shell 
curl -L -X GET 'qt.liumian.top:9990/' \
-H 'network_id: localTest' \
-H 'target_host: www.baidu.com' \
-H 'host: www.baidu.com' \
-H 'target_port: 80' \
-H 'Connection: close'
```

## 仓库地址
1. gitee：[乐天派 / quantum-tunnel](https://gitee.com/liumian/quantum-tunnel)
2. github：[liumian97/quantum-tunnel](https://github.com/liumian97/quantum-tunnel)

## 软件架构

### 架构图
![软件架构](https://images.gitee.com/uploads/images/2021/1015/183025_5f640314_602197.png "屏幕截图.png")


### 流程图
![流程图](https://images.gitee.com/uploads/images/2021/1019/001318_3a3e6f11_602197.png "屏幕截图.png")



## 使用说明

1. 克隆仓库到本地：git clone git@gitee.com:liumian/quantum-tunnel.git
2. 打包
   - 内网穿透服务端： sh package_server.sh
   - 内网穿透客户端： sh package_client.sh
3. 启动服务
   - 启动服务端： java -jar quantum-tunnel-server.jar -proxy_server_port 9090 -user_server_port 8090
   - 启动客户端： java -jar quantum-tunnel-client.jar -network_id localTest -proxy_server_host 127.0.0.1 -proxy_server_port 9090

全部参数说明：
```shell
# 内网穿透服务端
1. proxy_server_port：代理服务器端口，即接收代理客户连接的端口
2. user_server_port：用户服务器端口，即接收用户请求的端口
3. route_mode：路由模式
  3.1 协议路由：protocol_route，即解析用户提交的路由信息决定真实服务器的地址，适用于业务代理场景；默认协议路由模式
  3.2 端口路由：port_route，即将真实服务器的路由信息与端口绑定，忽略用户提交的路由信息，适用于中间件代理场景
4. network_id：被代理网络id。若选择端口路由，则必填
5. target_server_host：真实服务器地址。若选择端口路由，则必填
6. target_server_port：真实服务端口。若选择端口路由，则必填

# 内网穿透客户端
1. network_id：所在网络id
2. proxy_server_host：代理服务器地址
3. proxy_server_port：代理服务器端口
4. target_server_host：限制目标服务器host，默认不限制
4. target_server_port：限制目标服务器端口，默认不限制 
```

4. 使用代理服务访问百度
```shell
curl --location --request GET '127.0.0.1:8090/' \
--header 'Host: www.baidu.com' \
--header 'network_id: localTest' \
--header 'target_host: www.baidu.com' \
--header 'target_port: 80' \
--header 'Cookie: BDSVRTM=11; BD_HOME=1'
```

## release note

### v1.0.0
[发布链接](https://gitee.com/liumian/quantum-tunnel/releases/v1.0.0)

核心能力的简要说明：
1. 支持协议路由和端口路由：[QuantumTunnel：端口路由 vs 协议路由](https://mp.weixin.qq.com/s/6Ru1dWzU3JjNClIRFJ1I4A)
2. 基于Netty实现、使用Protobuf编解码，拥有优秀的转发性能
3. 支持客户端指定访问白名单，避免被恶意网络攻击
4. 使用极其简单，用两行命令即可拉起服务端与客户端


### v0.2.2-beta
[发布链接](https://gitee.com/liumian/quantum-tunnel/releases/v0.2.2-beta)
1. 使用protobuf进行编解码，提高数据编解码性能
2. 增加queryParam路由参数解析，适用不方便添加路由信息到header的场景，如websocket连接
3. 应用启动时增加git信息，方便查看当前版本

### v0.2-beta
[发布链接](https://gitee.com/liumian/quantum-tunnel/releases/v0.2-beta)
1. 新增端口路由模式：适用于端口场景
2. 新增心跳检测：避免被网络设备掐断QuantumTunnel长连接
3. 代理客户端支持指定host、port代理，增加内网访问安全


### v0.1-beta
[发布链接](https://gitee.com/liumian/quantum-tunnel/releases/v0.1-beta)
1. 支持协议路由模式：适用于业务场景


## 参与贡献

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request


#### 技术文章
1. [QuantumTunnel：内网穿透服务设计](https://mp.weixin.qq.com/s/7t5n_nI7CZ3VhownRhCsrg)
2. [QuantumTunnel：Netty实现](https://mp.weixin.qq.com/s/3N_c6IR--e85kmt0tjHSvw)
3. [QuantumTunnel：v0.1-beta发布](https://mp.weixin.qq.com/s/9GRyeFTZ_jdwXAtktSc9Uw)
4. [QuantumTunnel：端口路由 vs 协议路由](https://mp.weixin.qq.com/s/6Ru1dWzU3JjNClIRFJ1I4A)

