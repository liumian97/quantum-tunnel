package top.liumian.qt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.client.handler.ProxyClientHandler;
import top.liumian.qt.common.proto.QuantumMessage;

import java.util.Set;

/**
 * @author liumian  2021/9/26 11:40
 */
@Slf4j
public class ProxyClient {

    /**
     * 代理服务器地址
     */
    private final String proxyServerHost;

    /**
     * 代理服务器端口
     */
    private final String proxyServerPort;

    /**
     * 所在网络id，需要手动配置
     */
    private final String networkId;

    /**
     * 被代理服务白名单
     */
    private final Set<String> tupleWhiteSet;

    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public ProxyClient(String proxyServerHost, String proxyServerPort, String networkId, Set<String> tupleWhiteSet) {
        this.proxyServerHost = proxyServerHost;
        this.proxyServerPort = proxyServerPort;
        this.networkId = networkId;
        this.tupleWhiteSet = tupleWhiteSet;
    }

    /**
     * @return 与服务器通信的channel
     * @throws InterruptedException exception
     */
    public Channel connect() throws InterruptedException {

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ProxyClientHandler proxyClientHandler = new ProxyClientHandler(networkId, tupleWhiteSet);
                ch.pipeline()
                        .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                        .addLast(new ProtobufDecoder(QuantumMessage.Message.getDefaultInstance()))
                        .addLast("frameEncoder", new LengthFieldPrepender(4))
                        .addLast(new ProtobufEncoder())
                        .addLast(new IdleStateHandler(360, 300, 0))
                        .addLast(proxyClientHandler);
            }
        });
        return b.connect(proxyServerHost, Integer.parseInt(proxyServerPort)).addListener(future -> log.info("内网穿透客户端启动成功...")).sync().channel();
    }
}
