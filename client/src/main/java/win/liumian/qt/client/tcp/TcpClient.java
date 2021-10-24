package win.liumian.qt.client.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.client.tcp.handler.ProxyClientHandler;
import win.liumian.qt.common.QuantumMessageDecoder;
import win.liumian.qt.common.QuantumMessageEncoder;

import java.io.IOException;

/**
 * @author liumian  2021/10/24 16:11
 */
@Slf4j
public class TcpClient {

    private final String proxyServerHost;
    private final String proxyServerPort;
    private final String networkId;
    private final String targetServerHost;
    private final String targetServerPort;


    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public TcpClient(String proxyServerHost, String proxyServerPort, String networkId, String targetServerHost, String targetServerPort) {
        this.proxyServerHost = proxyServerHost;
        this.proxyServerPort = proxyServerPort;
        this.networkId = networkId;
        this.targetServerHost = targetServerHost;
        this.targetServerPort = targetServerPort;
    }

    /**
     * @throws InterruptedException
     */
    public Channel connect() throws InterruptedException, IOException {

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ProxyClientHandler proxyClientHandler = new ProxyClientHandler(networkId, targetServerHost, targetServerPort);
                ch.pipeline().addLast(
                        new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4),
                        new QuantumMessageDecoder(),
                        new QuantumMessageEncoder(),
                        new IdleStateHandler(360, 300, 0),
                        proxyClientHandler);
            }
        });
        return b.connect(proxyServerHost, Integer.parseInt(proxyServerPort)).addListener(future -> log.info("内网穿透客户端启动成功...")).sync().channel();
    }

}
