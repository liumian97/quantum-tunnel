package win.liumian.qt.common.handler;

import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

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
        log.error("捕获通道异常",cause);
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
                QuantumMessage quantumMessage = new QuantumMessage();
                quantumMessage.setNetworkId(networkId);
                quantumMessage.setMessageType(QuantumMessageType.KEEPALIVE);
                ctx.writeAndFlush(quantumMessage);
            }
        }
    }
}
