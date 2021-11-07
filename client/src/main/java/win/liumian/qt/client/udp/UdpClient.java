package win.liumian.qt.client.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.client.udp.handler.UdpClientHandler;
import win.liumian.qt.common.proto.QuantumMessage;

import java.net.InetSocketAddress;

/**
 * @author liumian  2021/10/24 14:18
 */
@Slf4j
public class UdpClient {

    private final String networkId;

    private final String remoteHost;

    private final int remotePort;

    public UdpClient(String networkId, String remoteHost, int remotePort) {
        this.networkId = networkId;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public void run() {

        EventLoopGroup bossGroup = new NioEventLoopGroup(20);
        try {
            //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
            //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
            Bootstrap b = new Bootstrap();
            b.group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new DefaultEventExecutorGroup(20), new UdpClientHandler());
                        }
                    });
//                    .handler(new UdpClientHandler());
            Channel channel = b.bind(0).sync().channel();


            QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(networkId).setMessageType(QuantumMessage.MessageType.REGISTER).build();

            ByteBuf byteBuf1 = Unpooled.copiedBuffer(message.toByteArray());
            DatagramPacket packet1 = new DatagramPacket(byteBuf1, new InetSocketAddress(remoteHost, remotePort));


//            ByteBuf byteBuf2 = Unpooled.copiedBuffer(JSONObject.toJSONString(quantumMessage), CharsetUtil.UTF_8);
//            DatagramPacket packet2 = new DatagramPacket(byteBuf2,  new InetSocketAddress(remoteHost, 10000));

            log.info("udp客户端启动，端口：{}", channel.localAddress());


            channel.writeAndFlush(packet1).sync();
//            channel.writeAndFlush(packet2).sync();
            channel.closeFuture().await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
        }
    }

}
