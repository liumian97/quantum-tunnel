package win.liumian.qt.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.udp.handler.UdpServerHandler;

/**
 * @author liumian  2021/10/24 16:31
 */
@Slf4j
public class UdpServer {

    private final int localPort;

    public UdpServer(int localPort) {
        this.localPort = localPort;
    }

    public void run() {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        try {
            //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
            //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
            Bootstrap b = new Bootstrap();
            b.group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UdpServerHandler());
            log.info("udp服务启动：{}", localPort);
            b.bind(localPort).sync().channel().closeFuture().await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

}
