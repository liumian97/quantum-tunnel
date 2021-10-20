# quantum-tunnel

## 介绍
QuantumTunnel，量子隧道，名字来源于量子纠缠现象。 > 两个处于量子纠缠的粒子，无论处于多么远的距离，当其中一个粒子状态改变时，另外一个粒子也会做出相应的改变。

QuantumTunnel也取意于此，希望把公网发出来的请求，完整的同步到内网，就像在内网发出的请求，打破网络的限制。

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
   - 启动客户端： java -jar quantum-tunnel-client.jar -network_id localTest -proxy_server_host lm.cn -proxy_server_port 9090
4. 使用代理服务访问百度
```shell
curl --location --request GET '127.0.0.1:8090/' \
--header 'targetPort: 80' \
--header 'networkId: localTest' \
--header 'Host: www.baidu.com' \
--header 'targetHost: www.baidu.com' \
--header 'Cookie: BDSVRTM=11; BD_HOME=1'
```

## 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 技术文章
1. [QuantumTunnel：内网穿透服务设计](https://mp.weixin.qq.com/s/7t5n_nI7CZ3VhownRhCsrg)
2. [QuantumTunnel:Netty实现](https://mp.weixin.qq.com/s/3N_c6IR--e85kmt0tjHSvw)
3. [QuantumTunnel：v0.1-beta发布](https://mp.weixin.qq.com/s/9GRyeFTZ_jdwXAtktSc9Uw)

