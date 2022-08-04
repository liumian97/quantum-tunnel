package top.liumian.qt.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.common.enumeration.RouteMode;
import top.liumian.qt.handler.UserServerHandler;

/**
 * 面向用户的服务器
 *
 * @author liumian  2021/9/18 16:22
 */
@Slf4j
public class UserServer {

    /**
     * 用户服务器端口
     */
    private final String userServerPort;

    /**
     * 路由模式
     * 协议路由：protocol_route
     * 端口路由：port_route
     */
    private final String routeMode;

    /**
     * 目标网络id
     */
    private final String networkId;

    /**
     * 目标host
     */
    private final String targetHost;

    /**
     * 目标端口
     */
    private final String targetPort;

    public UserServer(String userServerPort, String routeMode, String networkId, String targetHost, String targetPort) {
        this.userServerPort = userServerPort;
        this.routeMode = routeMode;
        if (RouteMode.PORT_ROUTE.value.equals(routeMode)) {
            if (networkId == null || targetHost == null || targetPort == null) {
                throw new RuntimeException("端口路由必须填写network_id，target_host，target_port参数");
            }
        }
        this.networkId = networkId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    public void start() {
        //new 一个主线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //new 一个工作线程组
        EventLoopGroup workGroup = new NioEventLoopGroup(200);
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        // 请求解码器
                        socketChannel.pipeline()
                                .addLast(new ByteArrayDecoder())
                                .addLast(new ByteArrayEncoder())
                                .addLast(new UserServerHandler(networkId, targetHost, targetPort));
                    }
                })
                .localAddress(Integer.parseInt(userServerPort))
                //设置队列大小
                .option(ChannelOption.SO_BACKLOG, 1024);
        //绑定端口,开始接收进来的连接
        try {
            ChannelFuture future = bootstrap.bind(Integer.parseInt(userServerPort)).sync();
            log.info("用户服务器启动，路由模式：{}，监听端口: {}", routeMode, userServerPort);
            if (RouteMode.PORT_ROUTE.value.equals(routeMode)) {
                log.info("目标网络Id：{}，目标host：{}，目标端口：{}", networkId, targetHost, targetPort);
            }
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //关闭主线程组
            bossGroup.shutdownGracefully();
            //关闭工作线程组
            workGroup.shutdownGracefully();
        }
    }

}
