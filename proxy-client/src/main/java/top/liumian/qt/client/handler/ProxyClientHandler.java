package top.liumian.qt.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.common.handler.QuantumCommonHandler;
import top.liumian.qt.common.proto.QuantumMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理proxy-client与proxy-server之间的连接
 *
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyClientHandler extends QuantumCommonHandler {

    /**
     * key：用户channelId
     * value：ProxyClient与被代码服务器的channel
     */
    public final static Map<String, Channel> USER_2_PROXY_CHANNEL_MAP = new ConcurrentHashMap<>();

    private final static NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    private final static Bootstrap CLIENT_BOOTSTRAP;

    static {
        CLIENT_BOOTSTRAP = new Bootstrap();
        CLIENT_BOOTSTRAP.group(WORKER_GROUP);
        CLIENT_BOOTSTRAP.channel(NioSocketChannel.class);
        CLIENT_BOOTSTRAP.option(ChannelOption.SO_KEEPALIVE, true);
    }

    /**
     * 被代理服务地址白名单
     */
    private final Set<String> tupleWhiteSet;

    public ProxyClientHandler(String networkId, Set<String> tupleWhiteSet) {
        super.networkId = networkId;
        this.tupleWhiteSet = tupleWhiteSet;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("准备注册量子通道");
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setNetworkId(networkId).setMessageType(QuantumMessage.MessageType.REGISTER).build();
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
        Channel channel = USER_2_PROXY_CHANNEL_MAP.get(quantumMessage.getChannelId());
        if (channel != null && channel.isOpen()) {
            log.info("UserServer关闭代理通道：{}", quantumMessage.getChannelId());
            channel.close();
        }
    }

    private void processData(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        doProxyRequest(ctx, quantumMessage);
    }

    /**
     * 向目标服务器发起真正的请求
     *
     * @param ctx            上下文
     * @param quantumMessage 服务器发送过来的指令
     */
    private void doProxyRequest(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        Channel proxyChannel = USER_2_PROXY_CHANNEL_MAP.get(quantumMessage.getChannelId());
        if (proxyChannel == null) {
            String targetTuple = quantumMessage.getTargetHost() + ":" + quantumMessage.getTargetPort();
            if (!tupleWhiteSet.isEmpty() && !tupleWhiteSet.contains(targetTuple)) {
                log.info("targetTuple不在白名单内：{}", targetTuple);
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
                return;
            }
            ByteBuf byteBuf = Unpooled.copiedBuffer(quantumMessage.getData().toByteArray());
            CLIENT_BOOTSTRAP.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new ProxyRequestHandler(ctx, quantumMessage.getChannelId(), networkId));
                }
            });

            try {
                Channel channel = CLIENT_BOOTSTRAP.connect(quantumMessage.getTargetHost(), quantumMessage.getTargetPort()).sync().channel();
                channel.writeAndFlush(byteBuf);
            } catch (Exception e) {
                log.error("请求" + targetTuple + "异常", e);
                //通知服务端proxyChannel已经断开，让其断开userChannel
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
            }
        } else {
            ByteBuf byteBuf = Unpooled.copiedBuffer(quantumMessage.getData().toByteArray());
            proxyChannel.writeAndFlush(byteBuf);
        }
    }

    /**
     * 通知服务器断开本次与用户的连接
     *
     * @param ctx       上下文
     * @param channelId 与用户的channelId
     */
    private void disconnectUserChannel(ChannelHandlerContext ctx, String channelId) {
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setChannelId(channelId).setNetworkId(networkId)
                .setMessageType(QuantumMessage.MessageType.PROXY_DISCONNECTED).build();
        ctx.writeAndFlush(message);
    }

}
