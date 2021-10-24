package win.liumian.qt.client.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.client.udp.handler.UdpClientHandler;

import java.net.InetSocketAddress;

/**
 * @author liumian  2021/10/24 14:18
 */
@Slf4j
public class UdpClient {

    private final String remoteHost;

    private final int remotePort;

    private final int localPort;

    public UdpClient(String remoteHost, int remotePort, int localPort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
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
                    .handler(new UdpClientHandler());
            Channel channel = b.bind(localPort).sync().channel();

            ByteBuf byteBuf = Unpooled.copiedBuffer("Hello UDP world", CharsetUtil.UTF_8);
            InetSocketAddress socketAddress = new InetSocketAddress(remoteHost, remotePort);
            DatagramPacket packet = new DatagramPacket(byteBuf, socketAddress);

            channel.writeAndFlush(packet).sync();

            if (!channel.closeFuture().await(15000)) {
                log.info("查询超时");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

}
