package win.liumian.qt.client;

import win.liumian.qt.client.handler.ProxyClientHandler;
import win.liumian.qt.common.QuantumMessageDecoder;
import win.liumian.qt.common.QuantumMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;

/**
 * @author liumian  2021/9/26 11:40
 */
public class ProxyClient {


    public static void main(String[] args) throws IOException, InterruptedException {
        ChannelFuture channelFuture = new ProxyClient().connect("localhost", 9090);
        channelFuture.awaitUninterruptibly();
    }

    public ChannelFuture connect(String serverAddress, int serverPort) throws IOException, InterruptedException {
        return this.connect(serverAddress, serverPort, new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ProxyClientHandler proxyClientHandler = new ProxyClientHandler();
                ch.pipeline().addLast(
                        new LengthFieldBasedFrameDecoder(65535,0,4,0,4),
                        new QuantumMessageDecoder(),
                        new QuantumMessageEncoder(),
//                        new IdleStateHandler(60, 30, 0),
                        proxyClientHandler);
            }
        });

    }


    /**
     * @param host
     * @param port
     * @param channelInitializer
     * @throws InterruptedException
     */
    public ChannelFuture connect(String host, int port, ChannelInitializer<SocketChannel> channelInitializer) throws InterruptedException, IOException {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(channelInitializer);

            Channel channel = b.connect(host, port).sync().channel();
            return channel.closeFuture().addListener(future -> workerGroup.shutdownGracefully());
        } catch (Exception e) {
            workerGroup.shutdownGracefully();
            throw e;
        }
    }


}
