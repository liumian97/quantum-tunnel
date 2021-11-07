package win.liumian.qt.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.common.proto.QuantumMessage;
import win.liumian.qt.handler.ProxyServerHandler;

/**
 * @author liumian  2021/9/19 19:10
 */
@Slf4j
public class ProxyServer {

    private String proxyServerPort;

    public ProxyServer(String proxyServerPort) {
        this.proxyServerPort = proxyServerPort;
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
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                                .addLast(new ProtobufDecoder(QuantumMessage.Message.getDefaultInstance()))
                                .addLast("frameEncoder", new LengthFieldPrepender(4))
                                .addLast(new ProtobufEncoder())
                                .addLast(new IdleStateHandler(360, 300, 0))
                                .addLast(new ProxyServerHandler());

                        /**
                         *  * pipeline.addLast("frameDecoder",
                         *  *                  new {@link LengthFieldBasedFrameDecoder}(1048576, 0, 4, 0, 4));
                         *  * pipeline.addLast("protobufDecoder",
                         *  *                  new {@link ProtobufDecoder}(MyMessage.getDefaultInstance()));
                         */

                    }
                })
                .localAddress(Integer.parseInt(proxyServerPort))
                //设置队列大小
                .option(ChannelOption.SO_BACKLOG, 1024)
                // 两小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        //绑定端口,开始接收进来的连接
        try {
            ChannelFuture future = bootstrap.bind(Integer.parseInt(proxyServerPort)).sync();
            log.info("代理服务器启动，监听端口: {}", proxyServerPort);
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
