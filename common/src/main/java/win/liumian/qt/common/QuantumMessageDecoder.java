package win.liumian.qt.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import win.liumian.qt.common.proto.QuantumMessage;

import java.util.List;

/**
 * @author liumian  2021/9/25 11:12
 */
public class QuantumMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {

        if (msg.isReadable()) {
            byte[] bytes = ByteBufUtil.getBytes(msg);

            QuantumMessage.Message message = QuantumMessage.Message.parseFrom(bytes);
            out.add(message);
        }
    }

}
