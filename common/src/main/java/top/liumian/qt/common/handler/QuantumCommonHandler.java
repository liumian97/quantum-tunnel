package top.liumian.qt.common.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.common.proto.QuantumMessage;

/**
 * @author liumian  2021/9/25 11:24
 */
@Slf4j
public class QuantumCommonHandler extends ChannelInboundHandlerAdapter {

    protected ChannelHandlerContext ctx;

    protected String networkId;

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                log.info("长时间未收到消息，关闭连接");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(networkId)
                        .setMessageType(QuantumMessage.MessageType.KEEPALIVE).build();
                ctx.writeAndFlush(message);
            }
        }
    }
}
