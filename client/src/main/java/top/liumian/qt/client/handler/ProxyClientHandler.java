package top.liumian.qt.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.common.handler.QuantumCommonHandler;
import top.liumian.qt.common.proto.QuantumMessage;

import javax.net.ssl.SSLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyClientHandler extends QuantumCommonHandler {

    public final static int DEFAULT_HTTPS_PORT = 443;

    public final static Map<String, Channel> user2ProxyChannelMap = new ConcurrentHashMap<>();

    private final static NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    private final Set<String> tupleWhiteSet;


    public ProxyClientHandler(String networkId, Set<String> tupleWhiteSet) {
        super.networkId = networkId;
        this.tupleWhiteSet = tupleWhiteSet;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("准备注册量子通道");
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setNetworkId(networkId).
                setMessageType(QuantumMessage.MessageType.REGISTER).build();
        ctx.writeAndFlush(message);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        QuantumMessage.Message quantumMessage = (QuantumMessage.Message) msg;
        if (quantumMessage.getMessageType() == QuantumMessage.MessageType.DATA) {
            processData(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.USER_DISCONNECTED) {
            processUserChannelDisconnected(quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.REGISTER_SUCCESS) {
            processRegisterSuccess(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.KEEPALIVE) {
            log.info("收到心跳消息，网络id：{}", quantumMessage.getNetworkId());
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.REGISTER_FAILED) {
            processRegisterFailed(ctx, quantumMessage);
        } else {
            throw new RuntimeException("Unknown type: " + quantumMessage.getMessageType());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("量子通道断开");
    }

    private void processRegisterSuccess(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        log.info("量子通道注册成功：{}", new String(quantumMessage.getData().toByteArray()));
    }

    private void processRegisterFailed(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        log.info("量子通道注册失败：{}", new String(quantumMessage.getData().toByteArray()));
        ctx.channel().close();
        throw new RuntimeException("注册失败");
    }

    private void processUserChannelDisconnected(QuantumMessage.Message quantumMessage) {
        Channel channel = user2ProxyChannelMap.get(quantumMessage.getChannelId());
        if (channel != null && channel.isOpen()) {
            log.info("UserServer关闭代理通道：{}", quantumMessage.getChannelId());
            channel.close();
        }
    }

    private void processData(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        try {
            doProxyRequest(ctx, quantumMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void doProxyRequest(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) throws InterruptedException {
        Channel proxyChannel = user2ProxyChannelMap.get(quantumMessage.getChannelId());


        if (proxyChannel == null) {
            String targetTuple = quantumMessage.getTargetHost() + ":" + quantumMessage.getTargetPort();
            if (!tupleWhiteSet.isEmpty() && !tupleWhiteSet.contains(targetTuple)) {
                log.info("targetTuple不在白名单内：{}", targetTuple);
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
                return;
            }
            ByteBuf byteBuf = Unpooled.copiedBuffer(quantumMessage.getData().toByteArray());
            try {
                Bootstrap b = new Bootstrap();
                b.group(WORKER_GROUP);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.SO_KEEPALIVE, true);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        if (DEFAULT_HTTPS_PORT == quantumMessage.getTargetPort()) {
                            try {
                                SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                                pipeline.addLast(sslContext.newHandler(ch.alloc()));
                            } catch (SSLException e) {
                                log.error("初始化sslContext失败：" + networkId, e);
                            }
                        }
                        pipeline.addLast(new ProxyRequestHandler(ctx, quantumMessage.getChannelId(), networkId));
                    }
                });
                Channel channel = b.connect(quantumMessage.getTargetHost(), quantumMessage.getTargetPort()).sync().channel();
                channel.writeAndFlush(byteBuf);
            } catch (Exception e) {
                log.error("请求targetServer异常", e);
                //通知服务端proxyChannel已经断开，让其断开userChannel
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
            }
        } else {
            ByteBuf byteBuf = Unpooled.copiedBuffer(quantumMessage.getData().toByteArray());
            proxyChannel.writeAndFlush(byteBuf);
        }
    }

    private void disconnectUserChannel(ChannelHandlerContext ctx, String channelId) {
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setChannelId(channelId).setNetworkId(networkId)
                .setMessageType(QuantumMessage.MessageType.PROXY_DISCONNECTED).build();
        ctx.writeAndFlush(message);
    }

}
